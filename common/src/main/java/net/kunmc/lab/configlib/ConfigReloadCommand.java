package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.CommonCommand;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.configlib.exception.ConfigValidationException;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

final class ConfigReloadCommand {
    static <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> T create(CommandFactory<C, T> commandFactory,
                                                                                          @NotNull Set<CommonBaseConfig> configs,
                                                                                          ConfigCommandDescriptions.Provider descriptions,
                                                                                          MaskedRevealPolicy maskedRevealPolicy) {
        T command = commandFactory.create(SubCommandType.Reload.name);
        command.description(ConfigCommandDescriptions.reload(descriptions));

        if (configs.isEmpty()) {
            throw new IllegalArgumentException("configs is empty");
        }

        command.execute(ctx -> {
            configs.forEach(config -> {
                exec(ctx, config, descriptions);
            });
        });

        if (configs.size() > 1) {
            configs.forEach(config -> {
                T child = commandFactory.create(config.entryName());
                child.description(ConfigCommandDescriptions.reloadConfig(descriptions, config.entryName()));
                child.execute(ctx -> exec(ctx, config, descriptions));
                command.addChildren(child);
            });
        }
        return command;
    }

    private static void exec(CommonCommandContext<?, ?> ctx,
                             CommonBaseConfig config,
                             ConfigCommandDescriptions.Provider descriptions) {
        try {
            if (config.loadConfig()) {
                ctx.sendSuccess(descriptions.describe(ctx,
                                                      ConfigCommandDescriptions.Key.RELOAD_SUCCESS,
                                                      config.entryName()));
            } else {
                ctx.sendFailure(descriptions.describe(ctx,
                                                      ConfigCommandDescriptions.Key.RELOAD_FAILURE,
                                                      config.entryName()));
            }
        } catch (ConfigValidationException e) {
            ctx.sendFailure(descriptions.describe(ctx,
                                                  ConfigCommandDescriptions.Key.RELOAD_FAILURE,
                                                  config.entryName()));
            e.sendMessage(ctx, descriptions);
        }
    }
}
