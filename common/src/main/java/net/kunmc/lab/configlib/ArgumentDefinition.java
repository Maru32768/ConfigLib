package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.ArgumentBuilder;
import net.kunmc.lab.commandlib.CommonArgument;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentValidationException;
import net.kunmc.lab.configlib.util.function.*;

/**
 * Defines how arguments are applied to a builder and mapped to a value.
 * The mapper may throw {@link net.kunmc.lab.commandlib.exception.ArgumentValidationException}
 * to send an error message to the command executor.
 */
public class ArgumentDefinition<T> implements ArgumentApplier, ArgumentMapper<T> {
    private final ArgumentApplier argumentApplier;
    private final ArgumentMapper<T> mapper;

    public ArgumentDefinition(ArgumentApplier argumentApplier, ArgumentMapper<T> mapper) {
        this.argumentApplier = argumentApplier;
        this.mapper = mapper;
    }

    public ArgumentDefinition(CommonArgument<T, ? extends CommonCommandContext<?, ?>, ?> argument1) {
        this(singleArgumentApplier(argument1), singleArgumentMapper(argument1));
    }

    private static <T> ArgumentApplier singleArgumentApplier(CommonArgument<T, ? extends CommonCommandContext<?, ?>, ?> argument1) {
        return b -> b.argument(argument1);
    }

    private static <T> ArgumentMapper<T> singleArgumentMapper(CommonArgument<T, ? extends CommonCommandContext<?, ?>, ?> argument1) {
        return ctx -> argument1.cast(ctx.getArgument(argument1.name()));
    }

    public <A1> ArgumentDefinition(CommonArgument<A1, ? extends CommonCommandContext<?, ?>, ?> argument1,
                                   BiArgumentMapper<A1, T> mapper) {
        this(b -> {
            b.argument(argument1);
        }, (ctx) -> {
            return mapper.apply(argument1.cast(ctx.getArgument(argument1.name())), ctx);
        });
    }

    public <A1, A2> ArgumentDefinition(CommonArgument<A1, ? extends CommonCommandContext<?, ?>, ?> argument1,
                                       CommonArgument<A2, ? extends CommonCommandContext<?, ?>, ?> argument2,
                                       TriArgumentMapper<A1, A2, T> mapper) {
        this(b -> {
            b.argument(argument1);
            b.argument(argument2);
        }, (ctx) -> {
            return mapper.apply(argument1.cast(ctx.getArgument(argument1.name())),
                                argument2.cast(ctx.getArgument(argument2.name())),
                                ctx);
        });
    }

    public <A1, A2, A3> ArgumentDefinition(CommonArgument<A1, ? extends CommonCommandContext<?, ?>, ?> argument1,
                                           CommonArgument<A2, ? extends CommonCommandContext<?, ?>, ?> argument2,
                                           CommonArgument<A3, ? extends CommonCommandContext<?, ?>, ?> argument3,
                                           QuadArgumentMapper<A1, A2, A3, T> mapper) {
        this(b -> {
            b.argument(argument1);
            b.argument(argument2);
            b.argument(argument3);
        }, (ctx) -> {
            return mapper.apply(argument1.cast(ctx.getArgument(argument1.name())),
                                argument2.cast(ctx.getArgument(argument2.name())),
                                argument3.cast(ctx.getArgument(argument3.name())),
                                ctx);
        });
    }

    public <A1, A2, A3, A4> ArgumentDefinition(CommonArgument<A1, ? extends CommonCommandContext<?, ?>, ?> argument1,
                                               CommonArgument<A2, ? extends CommonCommandContext<?, ?>, ?> argument2,
                                               CommonArgument<A3, ? extends CommonCommandContext<?, ?>, ?> argument3,
                                               CommonArgument<A4, ? extends CommonCommandContext<?, ?>, ?> argument4,
                                               QuintArgumentMapper<A1, A2, A3, A4, T> mapper) {
        this(b -> {
            b.argument(argument1);
            b.argument(argument2);
            b.argument(argument3);
            b.argument(argument4);
        }, (ctx) -> {
            return mapper.apply(argument1.cast(ctx.getArgument(argument1.name())),
                                argument2.cast(ctx.getArgument(argument2.name())),
                                argument3.cast(ctx.getArgument(argument3.name())),
                                argument4.cast(ctx.getArgument(argument4.name())),
                                ctx);
        });
    }

    public <A1, A2, A3, A4, A5> ArgumentDefinition(CommonArgument<A1, ? extends CommonCommandContext<?, ?>, ?> argument1,
                                                   CommonArgument<A2, ? extends CommonCommandContext<?, ?>, ?> argument2,
                                                   CommonArgument<A3, ? extends CommonCommandContext<?, ?>, ?> argument3,
                                                   CommonArgument<A4, ? extends CommonCommandContext<?, ?>, ?> argument4,
                                                   CommonArgument<A5, ? extends CommonCommandContext<?, ?>, ?> argument5,
                                                   SextArgumentMapper<A1, A2, A3, A4, A5, T> mapper) {
        this(b -> {
            b.argument(argument1);
            b.argument(argument2);
            b.argument(argument3);
            b.argument(argument4);
            b.argument(argument5);
        }, (ctx) -> {
            return mapper.apply(argument1.cast(ctx.getArgument(argument1.name())),
                                argument2.cast(ctx.getArgument(argument2.name())),
                                argument3.cast(ctx.getArgument(argument3.name())),
                                argument4.cast(ctx.getArgument(argument4.name())),
                                argument5.cast(ctx.getArgument(argument5.name())),
                                ctx);
        });
    }

    public <A1, A2, A3, A4, A5, A6> ArgumentDefinition(CommonArgument<A1, ? extends CommonCommandContext<?, ?>, ?> argument1,
                                                       CommonArgument<A2, ? extends CommonCommandContext<?, ?>, ?> argument2,
                                                       CommonArgument<A3, ? extends CommonCommandContext<?, ?>, ?> argument3,
                                                       CommonArgument<A4, ? extends CommonCommandContext<?, ?>, ?> argument4,
                                                       CommonArgument<A5, ? extends CommonCommandContext<?, ?>, ?> argument5,
                                                       CommonArgument<A6, ? extends CommonCommandContext<?, ?>, ?> argument6,
                                                       SeptArgumentMapper<A1, A2, A3, A4, A5, A6, T> mapper) {
        this(b -> {
            b.argument(argument1);
            b.argument(argument2);
            b.argument(argument3);
            b.argument(argument4);
            b.argument(argument5);
            b.argument(argument6);
        }, (ctx) -> {
            return mapper.apply(argument1.cast(ctx.getArgument(argument1.name())),
                                argument2.cast(ctx.getArgument(argument2.name())),
                                argument3.cast(ctx.getArgument(argument3.name())),
                                argument4.cast(ctx.getArgument(argument4.name())),
                                argument5.cast(ctx.getArgument(argument5.name())),
                                argument6.cast(ctx.getArgument(argument6.name())),
                                ctx);
        });
    }

    public void applyArgument(ArgumentBuilder builder) {
        argumentApplier.applyArgument(builder);
    }

    public T mapArgument(CommonCommandContext<?, ?> ctx) throws ArgumentValidationException {
        return mapper.mapArgument(ctx);
    }
}
