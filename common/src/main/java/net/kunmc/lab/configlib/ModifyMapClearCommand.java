package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.CommonCommand;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.configlib.exception.ConfigValidationException;
import net.kunmc.lab.configlib.schema.ConfigSchemaEntry;
import net.kunmc.lab.configlib.store.ChangeTrace;

import java.util.Map;

final class ModifyMapClearCommand {
    static <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> T create(CommandFactory<C, T> commandFactory,
                                                                                          CommonBaseConfig config,
                                                                                          ConfigSchemaEntry<?> schemaEntry,
                                                                                          MapValue value,
                                                                                          ConfigCommandDescriptions.Provider descriptions) {
        T command = commandFactory.create("clear");
        command.description(ConfigCommandDescriptions.clearMap(descriptions, schemaEntry.entryName()));

        command.addPrerequisite(value::checkExecutable);
        command.execute(ctx -> {
            try {
                Map cleared = (Map) value.copyValue(value.value());
                cleared.clear();
                ConfigSchemaValidation.validate(schemaEntry, cleared);
            } catch (ConfigValidationException e) {
                e.sendMessage(ctx, descriptions);
                return;
            }

            try {
                config.mutate(() -> {
                    value.dispatchClear();
                    value.clear();
                }, ChangeTrace.command(ctx, "clear " + schemaEntry.entryName(), schemaEntry.entryName()));
            } catch (ConfigValidationException e) {
                e.sendMessage(ctx, descriptions);
                return;
            }

            ctx.sendSuccess(descriptions.describe(ctx,
                                                  ConfigCommandDescriptions.Key.MAP_CLEAR_SUCCESS,
                                                  schemaEntry.entryName()));
        });
        return command;
    }
}
