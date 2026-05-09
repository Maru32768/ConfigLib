package net.kunmc.lab.samplemod;

import net.kunmc.lab.commandlib.Command;

public class TestCommand extends Command {
    public TestCommand(Command configCommand) {
        super("test");

        addChildren(configCommand);
    }
}
