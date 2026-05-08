package net.kunmc.lab.configlib.util.function;

import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentValidationException;

@FunctionalInterface
public interface TriArgumentMapper<A, B, R> {
    R apply(A a, B b, CommonCommandContext<?, ?> ctx) throws ArgumentValidationException;
}
