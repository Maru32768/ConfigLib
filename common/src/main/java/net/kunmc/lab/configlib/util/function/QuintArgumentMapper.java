package net.kunmc.lab.configlib.util.function;

import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentValidationException;

@FunctionalInterface
public interface QuintArgumentMapper<A, B, C, D, R> {
    R apply(A a, B b, C c, D d, CommonCommandContext<?, ?> ctx) throws ArgumentValidationException;
}
