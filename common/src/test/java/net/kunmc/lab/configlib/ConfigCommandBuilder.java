package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.TestCommand;
import net.kunmc.lab.commandlib.TestCommandContext;

class ConfigCommandBuilder extends CommonConfigCommandBuilder<TestCommandContext, TestCommand, ConfigCommandBuilder> {
    ConfigCommandBuilder(CommonBaseConfig config) {
        super(config, TestCommand::new);
    }
}
