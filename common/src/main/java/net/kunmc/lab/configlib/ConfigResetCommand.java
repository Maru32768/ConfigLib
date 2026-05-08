package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.CommonCommand;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.configlib.exception.ConfigValidationException;
import net.kunmc.lab.configlib.store.ChangeTrace;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

final class ConfigResetCommand {
    static <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> T create(CommandFactory<C, T> commandFactory,
                                                                                          @NotNull Set<CommonBaseConfig> configs,
                                                                                          ConfigCommandDescriptions.Provider descriptions,
                                                                                          MaskedRevealPolicy maskedRevealPolicy) {
        T command = commandFactory.create(SubCommandType.Reset.name);
        command.description(ConfigCommandDescriptions.reset(descriptions));

        if (configs.isEmpty()) {
            throw new IllegalArgumentException("configs is empty");
        }


        if (configs.size() == 1) {
            command.execute(ctx -> configs.forEach(config -> exec(ctx, config, descriptions, maskedRevealPolicy)));
        } else {
            configs.forEach(config -> {
                T child = commandFactory.create(config.entryName());
                child.description(ConfigCommandDescriptions.resetConfig(descriptions, config.entryName()));
                child.execute(ctx -> exec(ctx, config, descriptions, maskedRevealPolicy));
                command.addChildren(child);
            });
        }
        return command;
    }

    private static void exec(CommonCommandContext<?, ?> ctx,
                             CommonBaseConfig config,
                             ConfigCommandDescriptions.Provider descriptions,
                             MaskedRevealPolicy maskedRevealPolicy) {
        try {
            config.mutate(() -> {
                config.schema()
                      .entries()
                      .forEach(config::resetEntryToDefault);
            }, ChangeTrace.command(ctx, "reset " + config.entryName()));
        } catch (ConfigValidationException e) {
            e.sendMessage(ctx, descriptions);
            return;
        }
        ctx.sendSuccess(descriptions.describe(ctx, ConfigCommandDescriptions.Key.RESET_SUCCESS, config.entryName()));
        ConfigListCommand.listFields(ctx, config, maskedRevealPolicy);
    }
}
