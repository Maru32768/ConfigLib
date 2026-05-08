package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.CommonCommand;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.argument.CommonDoubleArgument;
import net.kunmc.lab.configlib.exception.ConfigValidationException;
import net.kunmc.lab.configlib.schema.ConfigSchemaEntry;
import net.kunmc.lab.configlib.store.ChangeTrace;

final class ModifyDecCommand {
    static <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> T create(CommandFactory<C, T> commandFactory,
                                                                                          CommonBaseConfig config,
                                                                                          ConfigSchemaEntry<?> schemaEntry,
                                                                                          NumericValue value,
                                                                                          ConfigCommandDescriptions.Provider descriptions,
                                                                                          MaskedRevealPolicy maskedRevealPolicy) {
        T command = commandFactory.create("dec");
        command.description(ConfigCommandDescriptions.decrement(descriptions, schemaEntry.entryName()));
        command.addPrerequisite(value::checkExecutable);
        command.execute(ctx -> exec(1.0, ctx, config, schemaEntry, value, descriptions, maskedRevealPolicy));
        command.argument(new CommonDoubleArgument<>("decValue"))
               .description(ConfigCommandDescriptions.decrementBy(descriptions, schemaEntry.entryName()))
               .execute((amount, ctx) -> exec(amount,
                                              ctx,
                                              config,
                                              schemaEntry,
                                              value,
                                              descriptions,
                                              maskedRevealPolicy));
        return command;
    }

    private static void exec(double amount,
                             CommonCommandContext<?, ?> ctx,
                             CommonBaseConfig config,
                             ConfigSchemaEntry<?> schemaEntry,
                             NumericValue value,
                             ConfigCommandDescriptions.Provider descriptions,
                             MaskedRevealPolicy maskedRevealPolicy) {
        if (value.compare(value.min.doubleValue() + amount) < 0) {
            amount = ((Number) value.value).doubleValue() - value.min.doubleValue();
        }

        Number newValue = value.copySub(amount);
        try {
            ConfigSchemaValidation.validate(schemaEntry, newValue);
        } catch (ConfigValidationException e) {
            e.sendMessage(ctx, descriptions);
            return;
        }

        try {
            config.mutate(() -> {
                value.dispatchModifyCommand(newValue);
                value.value(newValue);
            }, ChangeTrace.command(ctx, "dec " + schemaEntry.entryName(), schemaEntry.entryName()));
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
    }
}
