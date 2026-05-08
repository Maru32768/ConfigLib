package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.CommonCommand;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentValidationException;
import net.kunmc.lab.commandlib.util.ChatColorUtil;
import net.kunmc.lab.configlib.exception.ConfigValidationException;
import net.kunmc.lab.configlib.schema.ConfigSchemaEntry;
import net.kunmc.lab.configlib.schema.DisplayContext;
import net.kunmc.lab.configlib.store.ChangeTrace;
import net.kunmc.lab.configlib.util.function.ArgumentApplier;
import net.kunmc.lab.configlib.util.function.ArgumentMapper;

import java.util.Objects;

final class ConfigFieldCommand {
    static <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> T create(CommandFactory<C, T> commandFactory,
                                                                                          CommonBaseConfig config,
                                                                                          String entryName,
                                                                                          ConfigSchemaEntry<?> schemaEntry,
                                                                                          boolean getEnabled,
                                                                                          boolean modifyEnabled,
                                                                                          ConfigCommandDescriptions.Provider descriptions,
                                                                                          MaskedRevealPolicy maskedRevealPolicy) {
        T command = commandFactory.create(entryName);
        command.description(ConfigCommandDescriptions.field(descriptions,
                                                            schemaEntry.entryName(),
                                                            getEnabled,
                                                            modifyEnabled));
        Object obj = schemaEntry.commandObject();

        if (obj instanceof SingleValue) {
            initSingleValue(commandFactory,
                            command,
                            config,
                            schemaEntry,
                            (SingleValue<?, ?>) obj,
                            getEnabled,
                            modifyEnabled,
                            descriptions,
                            maskedRevealPolicy);
        } else if (obj instanceof CollectionValue) {
            initCollectionValue(commandFactory,
                                command,
                                config,
                                schemaEntry,
                                (CollectionValue<?, ?, ?>) obj,
                                getEnabled,
                                modifyEnabled,
                                descriptions,
                                maskedRevealPolicy);
        } else if (obj instanceof MapValue) {
            initMapValue(commandFactory,
                         command,
                         config,
                         schemaEntry,
                         (MapValue<?, ?, ?>) obj,
                         getEnabled,
                         modifyEnabled,
                         descriptions,
                         maskedRevealPolicy);
        } else if (getEnabled) {
            command.execute(ctx -> config.inspect(() -> {
                ctx.sendSuccess(schemaEntry.entryName() + ": " + schemaEntry.displayString(DisplayContext.command(ctx,
                                                                                                                  config,
                                                                                                                  maskedRevealPolicy)));
            }));
        }
        return command;
    }

    static <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> void applySet(CommonBaseConfig config,
                                                                                               T command,
                                                                                               ConfigSchemaEntry<?> schemaEntry,
                                                                                               SingleValue value,
                                                                                               ConfigCommandDescriptions.Provider descriptions,
                                                                                               MaskedRevealPolicy maskedRevealPolicy) {
        for (Object definition : value.argumentDefinitions()) {
            command.argument(builder -> {
                       ((ArgumentApplier) definition).applyArgument(builder);

                       builder.execute(ctx -> {
                           Object newValue;
                           try {
                               newValue = ((ArgumentMapper<?>) definition).mapArgument(ctx);
                           } catch (ArgumentValidationException e) {
                               e.sendMessage(ctx);
                               return;
                           }

                           try {
                               ConfigSchemaValidation.validate(schemaEntry, newValue);
                           } catch (ConfigValidationException e) {
                               e.sendMessage(ctx, descriptions);
                               return;
                           }

                           try {
                               config.mutate(() -> {
                                   value.dispatchModifyCommand(newValue);
                                   setSchemaValue(schemaEntry, newValue);
                               }, ChangeTrace.command(ctx, "set " + schemaEntry.entryName(), schemaEntry.entryName()));
                           } catch (ConfigValidationException e) {
                               e.sendMessage(ctx, descriptions);
                               return;
                           }

                           if (MaskedCommandOutput.shouldMask(ctx, config, schemaEntry, maskedRevealPolicy)) {
                               ctx.sendSuccess(descriptions.describe(ctx,
                                                                     ConfigCommandDescriptions.Key.SINGLE_VALUE_MODIFY_SUCCESS,
                                                                     schemaEntry.entryName(),
                                                                     MaskedCommandOutput.text(ctx,
                                                                                              config,
                                                                                              schemaEntry,
                                                                                              maskedRevealPolicy)));
                           } else {
                               ctx.sendSuccess(descriptions.describe(ctx,
                                                                     ConfigCommandDescriptions.Key.SINGLE_VALUE_MODIFY_SUCCESS,
                                                                     schemaEntry.entryName(),
                                                                     value.valueToString(value.value())));
                           }
                       });
                   })
                   .description(ConfigCommandDescriptions.set(descriptions, schemaEntry.entryName()));
        }
    }

    private static <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> void initSingleValue(
            CommandFactory<C, T> commandFactory,
            T command,
            CommonBaseConfig config,
            ConfigSchemaEntry<?> schemaEntry,
            SingleValue<?, ?> v,
            boolean getEnabled,
            boolean modifyEnabled,
            ConfigCommandDescriptions.Provider descriptions,
            MaskedRevealPolicy maskedRevealPolicy) {
        command.addPrerequisite(v::checkExecutable);

        if (getEnabled) {
            command.execute(ctx -> config.inspect(() -> ctx.sendMessageWithOption(schemaEntry.entryName() + ": " + schemaEntry.displayString(
                                                                                          DisplayContext.command(ctx, config, maskedRevealPolicy)),
                                                                                  option -> option.rgb(ChatColorUtil.GREEN.getRGB())
                                                                                                  .hoverText(Objects.toString(
                                                                                                          schemaEntry.metadata()
                                                                                                                     .description(),
                                                                                                          "")))));
        }

        if (modifyEnabled && v.isModifyEnabled()) {
            applySet(config, command, schemaEntry, v, descriptions, maskedRevealPolicy);
            T setCommand = commandFactory.create("set");
            setCommand.description(ConfigCommandDescriptions.set(descriptions, schemaEntry.entryName()));
            applySet(config, setCommand, schemaEntry, v, descriptions, maskedRevealPolicy);
            command.addChildren(setCommand);

            if (v instanceof NumericValue) {
                command.addChildren(ModifyIncCommand.create(commandFactory,
                                                            config,
                                                            schemaEntry,
                                                            (NumericValue<?, ?>) v,
                                                            descriptions,
                                                            maskedRevealPolicy));
                command.addChildren(ModifyDecCommand.create(commandFactory,
                                                            config,
                                                            schemaEntry,
                                                            (NumericValue<?, ?>) v,
                                                            descriptions,
                                                            maskedRevealPolicy));
            }

            T resetCommand = commandFactory.create("reset");
            resetCommand.description(ConfigCommandDescriptions.resetEntry(descriptions, schemaEntry.entryName()));
            resetCommand.execute(ctx -> resetEntry(ctx, config, schemaEntry, descriptions, maskedRevealPolicy));
            command.addChildren(resetCommand);
        }
    }

    @SuppressWarnings("unchecked")
    private static void setSchemaValue(ConfigSchemaEntry<?> schemaEntry, Object newValue) {
        ((ConfigSchemaEntry<Object>) schemaEntry).set(newValue);
    }

    private static <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> void initCollectionValue(
            CommandFactory<C, T> commandFactory,
            T command,
            CommonBaseConfig config,
            ConfigSchemaEntry<?> schemaEntry,
            CollectionValue<?, ?, ?> v,
            boolean getEnabled,
            boolean modifyEnabled,
            ConfigCommandDescriptions.Provider descriptions,
            MaskedRevealPolicy maskedRevealPolicy) {
        command.addPrerequisite(v::checkExecutable);

        if (getEnabled) {
            command.execute(ctx -> config.inspect(() -> ctx.sendMessageWithOption(schemaEntry.entryName() + ": " + schemaEntry.displayString(
                                                                                          DisplayContext.command(ctx, config, maskedRevealPolicy)),
                                                                                  option -> option.rgb(ChatColorUtil.GREEN.getRGB())
                                                                                                  .hoverText(Objects.toString(
                                                                                                          schemaEntry.metadata()
                                                                                                                     .description(),
                                                                                                          "")))));
        }

        if (modifyEnabled) {
            if (v.isAddEnabled()) {
                command.addChildren(ModifyAddCommand.create(commandFactory,
                                                            config,
                                                            schemaEntry,
                                                            v,
                                                            descriptions,
                                                            maskedRevealPolicy));
            }
            if (v.isRemoveEnabled()) {
                command.addChildren(ModifyRemoveCommand.create(commandFactory,
                                                               config,
                                                               schemaEntry,
                                                               v,
                                                               descriptions,
                                                               maskedRevealPolicy));
            }
            if (v.isClearEnabled()) {
                command.addChildren(ModifyClearCommand.create(commandFactory, config, schemaEntry, v, descriptions));
            }

            T resetCommand = commandFactory.create("reset");
            resetCommand.description(ConfigCommandDescriptions.resetEntry(descriptions, schemaEntry.entryName()));
            resetCommand.execute(ctx -> resetEntry(ctx, config, schemaEntry, descriptions, maskedRevealPolicy));
            command.addChildren(resetCommand);
        }
    }

    private static <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> void initMapValue(
            CommandFactory<C, T> commandFactory,
            T command,
            CommonBaseConfig config,
            ConfigSchemaEntry<?> schemaEntry,
            MapValue<?, ?, ?> v,
            boolean getEnabled,
            boolean modifyEnabled,
            ConfigCommandDescriptions.Provider descriptions,
            MaskedRevealPolicy maskedRevealPolicy) {
        command.addPrerequisite(v::checkExecutable);

        if (getEnabled) {
            command.execute(ctx -> config.inspect(() -> ctx.sendMessageWithOption(schemaEntry.entryName() + ": " + schemaEntry.displayString(
                                                                                          DisplayContext.command(ctx, config, maskedRevealPolicy)),
                                                                                  option -> option.rgb(ChatColorUtil.GREEN.getRGB())
                                                                                                  .hoverText(Objects.toString(
                                                                                                          schemaEntry.metadata()
                                                                                                                     .description(),
                                                                                                          "")))));
        }

        if (modifyEnabled) {
            if (v.isPutEnabled()) {
                command.addChildren(ModifyMapPutCommand.create(commandFactory,
                                                               config,
                                                               schemaEntry,
                                                               v,
                                                               descriptions,
                                                               maskedRevealPolicy));
            }
            if (v.isRemoveEnabled()) {
                command.addChildren(ModifyMapRemoveCommand.create(commandFactory,
                                                                  config,
                                                                  schemaEntry,
                                                                  v,
                                                                  descriptions,
                                                                  maskedRevealPolicy));
            }
            if (v.isClearEnabled()) {
                command.addChildren(ModifyMapClearCommand.create(commandFactory, config, schemaEntry, v, descriptions));
            }

            T resetCommand = commandFactory.create("reset");
            resetCommand.description(ConfigCommandDescriptions.resetEntry(descriptions, schemaEntry.entryName()));
            resetCommand.execute(ctx -> resetEntry(ctx, config, schemaEntry, descriptions, maskedRevealPolicy));
            command.addChildren(resetCommand);
        }
    }

    private static void resetEntry(CommonCommandContext<?, ?> ctx,
                                   CommonBaseConfig config,
                                   ConfigSchemaEntry<?> schemaEntry,
                                   ConfigCommandDescriptions.Provider descriptions,
                                   MaskedRevealPolicy maskedRevealPolicy) {
        try {
            config.mutate(() -> config.resetEntryToDefault(schemaEntry),
                          ChangeTrace.command(ctx, "reset " + schemaEntry.entryName(), schemaEntry.entryName()));
        } catch (ConfigValidationException e) {
            e.sendMessage(ctx, descriptions);
            return;
        }
        ctx.sendSuccess(descriptions.describe(ctx,
                                              ConfigCommandDescriptions.Key.FIELD_RESET_SUCCESS,
                                              schemaEntry.entryName(),
                                              schemaEntry.displayString(DisplayContext.command(ctx,
                                                                                               config,
                                                                                               maskedRevealPolicy))));
    }
}
