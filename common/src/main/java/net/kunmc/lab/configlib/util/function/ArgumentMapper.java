package net.kunmc.lab.configlib.util.function;

import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentValidationException;

@FunctionalInterface
public interface ArgumentMapper<T> {
    /**
     * Defines how arguments are mapped to a value.
     * This may throw {@link net.kunmc.lab.commandlib.exception.ArgumentValidationException}
     * to send an error message to the command executor.
     */
    T mapArgument(CommonCommandContext<?, ?> ctx) throws ArgumentValidationException;
}
