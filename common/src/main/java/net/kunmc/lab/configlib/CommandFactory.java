package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.CommonCommand;
import net.kunmc.lab.commandlib.CommonCommandContext;

@FunctionalInterface
public interface CommandFactory<C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> {
    T create(String name);
}
