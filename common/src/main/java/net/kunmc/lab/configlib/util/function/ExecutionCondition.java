package net.kunmc.lab.configlib.util.function;

import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.exception.CommandPrerequisiteException;

@FunctionalInterface
public interface ExecutionCondition {
    void check(CommonCommandContext<?, ?> ctx) throws CommandPrerequisiteException;
}
