package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.CommonCommand;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentValidationException;
import net.kunmc.lab.configlib.exception.ConfigValidationException;
import net.kunmc.lab.configlib.schema.ConfigSchemaEntry;
import net.kunmc.lab.configlib.store.ChangeTrace;
import net.kunmc.lab.configlib.util.function.ArgumentApplier;
import net.kunmc.lab.configlib.util.function.ArgumentMapper;

import java.util.Collection;

final class ModifyAddCommand {
    static <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> T create(CommandFactory<C, T> commandFactory,
                                                                                          CommonBaseConfig config,
                                                                                          ConfigSchemaEntry<?> schemaEntry,
                                                                                          CollectionValue value,
                                                                                          ConfigCommandDescriptions.Provider descriptions,
                                                                                          MaskedRevealPolicy maskedRevealPolicy) {
        T command = commandFactory.create("add");
        command.description(ConfigCommandDescriptions.add(descriptions, schemaEntry.entryName()));
        command.addPrerequisite(value::checkExecutable);

        for (Object definition : value.argumentDefinitionsForAdd()) {
            command.argument(builder -> {
                       ((ArgumentApplier) definition).applyArgument(builder);

                       builder.execute(ctx -> {
                           Collection newValue;
                           try {
                               newValue = ((ArgumentMapper<Collection>) definition).mapArgument(ctx);
                           } catch (ArgumentValidationException e) {
                               e.sendMessage(ctx);
                               return;
                           }

                           try {
                               Collection result = value.toAdded(newValue.toArray());
                               ConfigSchemaValidation.validate(schemaEntry, result);
                           } catch (ConfigValidationException e) {
                               e.sendMessage(ctx, descriptions);
                               return;
                           }

                           try {
                               config.mutate(() -> {
                                   value.dispatchAdd(newValue);
                                   ((Collection) value.value()).addAll(newValue);
                               }, ChangeTrace.command(ctx, "add " + schemaEntry.entryName(), schemaEntry.entryName()));
                           } catch (ConfigValidationException e) {
                               e.sendMessage(ctx, descriptions);
                               return;
                           }

                           if (MaskedCommandOutput.shouldMask(ctx, config, schemaEntry, maskedRevealPolicy)) {
                               ctx.sendSuccess(descriptions.describe(ctx,
                                                                     ConfigCommandDescriptions.Key.COLLECTION_ADD_SUCCESS,
                                                                     schemaEntry.entryName(),
                                                                     MaskedCommandOutput.text(ctx,
                                                                                              config,
                                                                                              schemaEntry,
                                                                                              maskedRevealPolicy)));
                           } else {
                               ctx.sendSuccess(descriptions.describe(ctx,
                                                                     ConfigCommandDescriptions.Key.COLLECTION_ADD_SUCCESS,
                                                                     schemaEntry.entryName(),
                                                                     value.elementsToString(newValue)));
                           }
                       });
                   })
                   .description(ConfigCommandDescriptions.add(descriptions, schemaEntry.entryName()));
        }
        return command;
    }
}
