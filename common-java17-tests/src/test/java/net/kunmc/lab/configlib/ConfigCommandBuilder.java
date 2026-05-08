package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.TestCommand;
import net.kunmc.lab.commandlib.TestCommandContext;

public class ConfigCommandBuilder extends CommonConfigCommandBuilder<TestCommandContext, TestCommand, ConfigCommandBuilder> {
    public ConfigCommandBuilder(CommonBaseConfig config) {
        super(config, TestCommand::new);
    }
}
