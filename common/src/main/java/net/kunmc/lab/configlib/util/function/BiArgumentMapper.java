package net.kunmc.lab.configlib.util.function;

import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentValidationException;

@FunctionalInterface
public interface BiArgumentMapper<A, R> {
    R apply(A a, CommonCommandContext<?, ?> ctx) throws ArgumentValidationException;
}