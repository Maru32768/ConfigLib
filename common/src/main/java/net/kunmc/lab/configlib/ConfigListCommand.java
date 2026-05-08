package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.util.ChatColorUtil;
import net.kunmc.lab.configlib.schema.ConfigSchemaEntry;
import net.kunmc.lab.configlib.schema.DisplayContext;
import net.kunmc.lab.configlib.util.ConfigUtil;

import java.util.Objects;
import java.util.Set;

class ConfigListCommand extends Command {
    public ConfigListCommand(Set<CommonBaseConfig> configs,
                             ConfigCommandDescriptions.Provider descriptions,
                             MaskedRevealPolicy maskedRevealPolicy) {
        super(SubCommandType.List.name);
        description(ConfigCommandDescriptions.list(descriptions));

        if (configs.isEmpty()) {
            throw new IllegalArgumentException("configs is empty");
        }

        execute(ctx -> configs.forEach(config -> {
            listFields(ctx, config, maskedRevealPolicy);
        }));

        if (configs.size() > 1) {
            configs.forEach(config -> {
                addChildren(new Command(config.entryName()) {{
                    description(ConfigCommandDescriptions.config(descriptions, config.entryName()));
                    execute(ctx -> {
                        listFields(ctx, config, maskedRevealPolicy);
                    });
                }});
            });
        }
    }

    static void listFields(CommandContext ctx, CommonBaseConfig config, MaskedRevealPolicy maskedRevealPolicy) {
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
