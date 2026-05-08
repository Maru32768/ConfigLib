package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.CommonCommand;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.util.ChatColorUtil;
import net.kunmc.lab.configlib.schema.ConfigSchemaEntry;
import net.kunmc.lab.configlib.schema.DisplayContext;
import net.kunmc.lab.configlib.util.ConfigUtil;

import java.util.Objects;
import java.util.Set;

final class ConfigListCommand {
    static <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> T create(CommandFactory<C, T> commandFactory,
                                                                                          Set<CommonBaseConfig> configs,
                                                                                          ConfigCommandDescriptions.Provider descriptions,
                                                                                          MaskedRevealPolicy maskedRevealPolicy) {
        T command = commandFactory.create(SubCommandType.List.name);
        command.description(ConfigCommandDescriptions.list(descriptions));

        if (configs.isEmpty()) {
            throw new IllegalArgumentException("configs is empty");
        }

        command.execute(ctx -> configs.forEach(config -> {
            listFields(ctx, config, maskedRevealPolicy);
        }));

        if (configs.size() > 1) {
            configs.forEach(config -> {
                T child = commandFactory.create(config.entryName());
                child.description(ConfigCommandDescriptions.config(descriptions, config.entryName()));
                child.execute(ctx -> listFields(ctx, config, maskedRevealPolicy));
                command.addChildren(child);
            });
        }
        return command;
    }

    static void listFields(CommonCommandContext<?, ?> ctx,
                           CommonBaseConfig config,
                           MaskedRevealPolicy maskedRevealPolicy) {
        config.inspect(() -> {
            ctx.sendMessage(ConfigUtil.configHeader(config));
            for (ConfigSchemaEntry<?> entry : config.schema()
                                                    .entries()) {
                ctx.sendMessageWithOption(entry.entryName() + ": " + entry.displayString(DisplayContext.command(ctx,
                                                                                                                config,
                                                                                                                maskedRevealPolicy)),
                                          option -> option.rgb(ChatColorUtil.GREEN.getRGB())
                                                          .hoverText(Objects.toString(entry.metadata()
                                                                                           .description(), "")));
            }
        });
    }
}
