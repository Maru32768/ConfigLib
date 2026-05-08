package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.CommonCommand;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentValidationException;
import net.kunmc.lab.configlib.exception.ConfigValidationException;
import net.kunmc.lab.configlib.schema.ConfigSchemaEntry;
import net.kunmc.lab.configlib.store.ChangeTrace;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ModifyMapRemoveCommand {
    static <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> T create(CommandFactory<C, T> commandFactory,
                                                                                          CommonBaseConfig config,
                                                                                          ConfigSchemaEntry<?> schemaEntry,
                                                                                          MapValue value,
                                                                                          ConfigCommandDescriptions.Provider descriptions,
                                                                                          MaskedRevealPolicy maskedRevealPolicy) {
        T command = commandFactory.create("remove");
        command.description(ConfigCommandDescriptions.removeMap(descriptions, schemaEntry.entryName()));

        command.addPrerequisite(value::checkExecutable);
        for (ArgumentDefinition<?> definition : ((List<ArgumentDefinition<?>>) value.argumentDefinitionsForRemove())) {
            command.argument(builder -> {
                       definition.applyArgument(builder);

                       builder.execute(ctx -> {
                           Object k;
                           try {
                               k = definition.mapArgument(ctx);
                           } catch (ArgumentValidationException e) {
                               e.sendMessage(ctx);
                               return;
                           }

                           Object v = value.get(k);

                           try {
                               Map remaining = new HashMap<>(((Map) value.value()));
                               remaining.remove(k);
                               ConfigSchemaValidation.validate(schemaEntry, remaining);
                           } catch (ConfigValidationException e) {
                               e.sendMessage(ctx, descriptions);
                               return;
                           }

                           try {
                               config.mutate(() -> {
                                   value.remove(k);
                                   value.dispatchRemove(k, v);
                               }, ChangeTrace.command(ctx, "remove " + schemaEntry.entryName(), schemaEntry.entryName()));
                           } catch (ConfigValidationException e) {
                               e.sendMessage(ctx, descriptions);
                               return;
                           }

                           if (MaskedCommandOutput.shouldMask(ctx, config, schemaEntry, maskedRevealPolicy)) {
                               String masked = MaskedCommandOutput.text(ctx, config, schemaEntry, maskedRevealPolicy);
                               ctx.sendSuccess(descriptions.describe(ctx,
                                                                     ConfigCommandDescriptions.Key.MAP_REMOVE_SUCCESS,
                                                                     schemaEntry.entryName(),
                                                                     masked,
                                                                     masked));
                           } else {
                               ctx.sendSuccess(descriptions.describe(ctx,
                                                                     ConfigCommandDescriptions.Key.MAP_REMOVE_SUCCESS,
                                                                     schemaEntry.entryName(),
                                                                     value.keyToString(k),
                                                                     value.valueToString(v)));
                           }
                       });
                   })
                   .description(ConfigCommandDescriptions.removeMap(descriptions, schemaEntry.entryName()));
        }
        return command;
    }
}
