package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.exception.ArgumentValidationException;
import net.kunmc.lab.configlib.exception.ConfigValidationException;
import net.kunmc.lab.configlib.schema.ConfigSchemaEntry;
import net.kunmc.lab.configlib.store.ChangeTrace;
import net.kunmc.lab.configlib.util.function.ArgumentApplier;
import net.kunmc.lab.configlib.util.function.ArgumentMapper;

import java.util.Collection;

class ModifyRemoveCommand extends Command {
    public ModifyRemoveCommand(CommonBaseConfig config,
                               ConfigSchemaEntry<?> schemaEntry,
                               CollectionValue value,
                               ConfigCommandDescriptions.Provider descriptions,
                               MaskedRevealPolicy maskedRevealPolicy) {
        super("remove");
        description(ConfigCommandDescriptions.remove(descriptions, schemaEntry.entryName()));

        addPrerequisite(value::checkExecutable);
        for (Object definition : value.argumentDefinitionsForRemove()) {
            argument(builder -> {
                ((ArgumentApplier) definition).applyArgument(builder);

                builder.execute(ctx -> {
                    Collection removeValue;
                    try {
                        removeValue = ((ArgumentMapper<Collection>) definition).mapArgument(ctx);
                    } catch (ArgumentValidationException e) {
                        e.sendMessage(ctx);
                        return;
                    }

                    try {
                        Collection remaining = value.toRemoved(removeValue.toArray());
                        ConfigSchemaValidation.validate(schemaEntry, remaining);
                    } catch (ConfigValidationException e) {
                        e.sendMessage(ctx, descriptions);
                        return;
                    }

                    try {
                        config.mutate(() -> {
                            value.dispatchRemove(removeValue);
                            ((Collection) value.value()).removeAll(removeValue);
                        }, ChangeTrace.command(ctx, "remove " + schemaEntry.entryName(), schemaEntry.entryName()));
                    } catch (ConfigValidationException e) {
                        e.sendMessage(ctx, descriptions);
                        return;
                    }

                    if (MaskedCommandOutput.shouldMask(ctx, config, schemaEntry, maskedRevealPolicy)) {
                        ctx.sendSuccess(descriptions.describe(ctx,
                                                              ConfigCommandDescriptions.Key.COLLECTION_REMOVE_SUCCESS,
                                                              schemaEntry.entryName(),
                                                              MaskedCommandOutput.text(ctx,
                                                                                       config,
                                                                                       schemaEntry,
                                                                                       maskedRevealPolicy)));
                    } else {
                        ctx.sendSuccess(descriptions.describe(ctx,
                                                              ConfigCommandDescriptions.Key.COLLECTION_REMOVE_SUCCESS,
                                                              schemaEntry.entryName(),
                                                              value.elementsToString(removeValue)));
                    }
                });
            }).description(ConfigCommandDescriptions.remove(descriptions, schemaEntry.entryName()));
        }
    }
}
