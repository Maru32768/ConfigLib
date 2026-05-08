package net.kunmc.lab.configlib.util.function;

import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentValidationException;

@FunctionalInterface
public interface SextArgumentMapper<A, B, C, D, E, R> {
    R apply(A a, B b, C c, D d, E e, CommonCommandContext<?, ?> ctx) throws ArgumentValidationException;
}
