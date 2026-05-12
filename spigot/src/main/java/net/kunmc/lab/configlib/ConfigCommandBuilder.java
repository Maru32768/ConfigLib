package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandContext;
import org.jetbrains.annotations.NotNull;

public class ConfigCommandBuilder extends CommonConfigCommandBuilder<CommandContext, Command, ConfigCommandBuilder> {
    public ConfigCommandBuilder(@NotNull CommonBaseConfig config) {
        super(config, Command::new);
    }
}
