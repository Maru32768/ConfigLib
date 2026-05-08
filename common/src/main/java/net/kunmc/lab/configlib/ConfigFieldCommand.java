package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentValidationException;
import net.kunmc.lab.commandlib.util.ChatColorUtil;
import net.kunmc.lab.configlib.exception.ConfigValidationException;
import net.kunmc.lab.configlib.schema.ConfigSchemaEntry;
import net.kunmc.lab.configlib.schema.DisplayContext;
import net.kunmc.lab.configlib.store.ChangeTrace;
import net.kunmc.lab.configlib.util.function.ArgumentApplier;
import net.kunmc.lab.configlib.util.function.ArgumentMapper;

import java.util.Objects;

class ConfigFieldCommand extends Command {
    ConfigFieldCommand(CommonBaseConfig config,
                       String entryName,
                       ConfigSchemaEntry<?> schemaEntry,
                       boolean getEnabled,
                       boolean modifyEnabled,
                       ConfigCommandDescriptions.Provider descriptions,
                       MaskedRevealPolicy maskedRevealPolicy) {
        super(entryName);
        description(ConfigCommandDescriptions.field(descriptions, schemaEntry.entryName(), getEnabled, modifyEnabled));
        Object obj = schemaEntry.commandObject();

        if (obj instanceof SingleValue) {
            initSingleValue(config,
                            schemaEntry,
                            (SingleValue<?, ?>) obj,
                            getEnabled,
                            modifyEnabled,
                            descriptions,
                            maskedRevealPolicy);
        } else if (obj instanceof CollectionValue) {
            initCollectionValue(config,
                                schemaEntry,
                                (CollectionValue<?, ?, ?>) obj,
                                getEnabled,
                                modifyEnabled,
                                descriptions,
                                maskedRevealPolicy);
        } else if (obj instanceof MapValue) {
            initMapValue(config,
                         schemaEntry,
                         (MapValue<?, ?, ?>) obj,
                         getEnabled,
                         modifyEnabled,
                         descriptions,
                         maskedRevealPolicy);
        } else if (getEnabled) {
            execute(ctx -> config.inspect(() -> {
                ctx.sendSuccess(schemaEntry.entryName() + ": " + schemaEntry.displayString(DisplayContext.command(ctx,
                                                                                                                  config,
                                                                                                                  maskedRevealPolicy)));
            }));
        }
    }

    static void applySet(CommonBaseConfig config,
                         Command command,
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

    private void initSingleValue(CommonBaseConfig config,
                                 ConfigSchemaEntry<?> schemaEntry,
                                 SingleValue<?, ?> v,
                                 boolean getEnabled,
                                 boolean modifyEnabled,
                                 ConfigCommandDescriptions.Provider descriptions,
                                 MaskedRevealPolicy maskedRevealPolicy) {
        addPrerequisite(v::checkExecutable);

        if (getEnabled) {
            execute(ctx -> config.inspect(() -> ctx.sendMessageWithOption(schemaEntry.entryName() + ": " + schemaEntry.displayString(
                                                                                  DisplayContext.command(ctx, config, maskedRevealPolicy)),
                                                                          option -> option.rgb(ChatColorUtil.GREEN.getRGB())
                                                                                          .hoverText(Objects.toString(schemaEntry.metadata()
                                                                                                                                  .description(),
                                                                                                                       "")))));
        }

        if (modifyEnabled && v.isModifyEnabled()) {
            applySet(config, this, schemaEntry, v, descriptions, maskedRevealPolicy);
            addChildren(new Command("set") {{
                description(ConfigCommandDescriptions.set(descriptions, schemaEntry.entryName()));
                applySet(config, this, schemaEntry, v, descriptions, maskedRevealPolicy);
            }});

            if (v instanceof NumericValue) {
                addChildren(new ModifyIncCommand(config,
                                                 schemaEntry,
                                                 (NumericValue<?, ?>) v,
                                                 descriptions,
                                                 maskedRevealPolicy));
                addChildren(new ModifyDecCommand(config,
                                                 schemaEntry,
                                                 (NumericValue<?, ?>) v,
                                                 descriptions,
                                                 maskedRevealPolicy));
            }

            addChildren(new Command("reset") {{
                description(ConfigCommandDescriptions.resetEntry(descriptions, schemaEntry.entryName()));
                execute(ctx -> resetEntry(ctx, config, schemaEntry, descriptions, maskedRevealPolicy));
            }});
        }
    }

    @SuppressWarnings("unchecked")
    private static void setSchemaValue(ConfigSchemaEntry<?> schemaEntry, Object newValue) {
        ((ConfigSchemaEntry<Object>) schemaEntry).set(newValue);
    }

    private void initCollectionValue(CommonBaseConfig config,
                                     ConfigSchemaEntry<?> schemaEntry,
                                     CollectionValue<?, ?, ?> v,
                                     boolean getEnabled,
                                     boolean modifyEnabled,
                                     ConfigCommandDescriptions.Provider descriptions,
                                     MaskedRevealPolicy maskedRevealPolicy) {
        addPrerequisite(v::checkExecutable);

        if (getEnabled) {
            execute(ctx -> config.inspect(() -> ctx.sendMessageWithOption(schemaEntry.entryName() + ": " + schemaEntry.displayString(
                                                                                  DisplayContext.command(ctx, config, maskedRevealPolicy)),
                                                                          option -> option.rgb(ChatColorUtil.GREEN.getRGB())
                                                                                          .hoverText(Objects.toString(schemaEntry.metadata()
                                                                                                                                  .description(),
                                                                                                                       "")))));
        }

        if (modifyEnabled) {
            if (v.isAddEnabled()) {
                addChildren(new ModifyAddCommand(config, schemaEntry, v, descriptions, maskedRevealPolicy));
            }
            if (v.isRemoveEnabled()) {
                addChildren(new ModifyRemoveCommand(config, schemaEntry, v, descriptions, maskedRevealPolicy));
            }
            if (v.isClearEnabled()) {
                addChildren(new ModifyClearCommand(config, schemaEntry, v, descriptions));
            }

            addChildren(new Command("reset") {{
                description(ConfigCommandDescriptions.resetEntry(descriptions, schemaEntry.entryName()));
                execute(ctx -> resetEntry(ctx, config, schemaEntry, descriptions, maskedRevealPolicy));
            }});
        }
    }

    private void initMapValue(CommonBaseConfig config,
                              ConfigSchemaEntry<?> schemaEntry,
                              MapValue<?, ?, ?> v,
                              boolean getEnabled,
                              boolean modifyEnabled,
                              ConfigCommandDescriptions.Provider descriptions,
                              MaskedRevealPolicy maskedRevealPolicy) {
        addPrerequisite(v::checkExecutable);

        if (getEnabled) {
            execute(ctx -> config.inspect(() -> ctx.sendMessageWithOption(schemaEntry.entryName() + ": " + schemaEntry.displayString(
                                                                                  DisplayContext.command(ctx, config, maskedRevealPolicy)),
                                                                          option -> option.rgb(ChatColorUtil.GREEN.getRGB())
                                                                                          .hoverText(Objects.toString(schemaEntry.metadata()
                                                                                                                                  .description(),
                                                                                                                       "")))));
        }

        if (modifyEnabled) {
            if (v.isPutEnabled()) {
                addChildren(new ModifyMapPutCommand(config, schemaEntry, v, descriptions, maskedRevealPolicy));
            }
            if (v.isRemoveEnabled()) {
                addChildren(new ModifyMapRemoveCommand(config, schemaEntry, v, descriptions, maskedRevealPolicy));
            }
            if (v.isClearEnabled()) {
                addChildren(new ModifyMapClearCommand(config, schemaEntry, v, descriptions));
            }

            addChildren(new Command("reset") {{
                description(ConfigCommandDescriptions.resetEntry(descriptions, schemaEntry.entryName()));
                execute(ctx -> resetEntry(ctx, config, schemaEntry, descriptions, maskedRevealPolicy));
            }});
        }
    }

    private static void resetEntry(CommandContext ctx,
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
