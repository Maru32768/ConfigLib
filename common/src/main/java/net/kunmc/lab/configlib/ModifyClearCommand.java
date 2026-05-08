package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.CommonCommand;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.configlib.exception.ConfigValidationException;
import net.kunmc.lab.configlib.schema.ConfigSchemaEntry;
import net.kunmc.lab.configlib.store.ChangeTrace;

import java.util.Collection;

final class ModifyClearCommand {
    static <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> T create(CommandFactory<C, T> commandFactory,
                                                                                          CommonBaseConfig config,
                                                                                          ConfigSchemaEntry<?> schemaEntry,
                                                                                          CollectionValue value,
                                                                                          ConfigCommandDescriptions.Provider descriptions) {
        T command = commandFactory.create("clear");
        command.description(ConfigCommandDescriptions.clear(descriptions, schemaEntry.entryName()));

        command.addPrerequisite(value::checkExecutable);
        command.execute(ctx -> {
            try {
                Collection cleared = (Collection) value.copyValue(value.value());
                cleared.clear();
                ConfigSchemaValidation.validate(schemaEntry, cleared);
            } catch (ConfigValidationException e) {
                e.sendMessage(ctx, descriptions);
                return;
            }

            try {
                config.mutate(() -> {
                    value.dispatchClear();
                    ((Collection) value.value()).clear();
                }, ChangeTrace.command(ctx, "clear " + schemaEntry.entryName(), schemaEntry.entryName()));
            } catch (ConfigValidationException e) {
                e.sendMessage(ctx, descriptions);
                return;
            }

            ctx.sendSuccess(descriptions.describe(ctx,
                                                  ConfigCommandDescriptions.Key.COLLECTION_CLEAR_SUCCESS,
                                                  schemaEntry.entryName()));
        });
        return command;
    }
}
