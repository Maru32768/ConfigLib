package net.kunmc.lab.configlib.value;

import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.argument.CommonEnumArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;
import net.kunmc.lab.configlib.SingleValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.BiFunction;

public class EnumValue<T extends Enum<T>> extends SingleValue<T, EnumValue<T>> {
    private final transient Class<T> clazz;
    private transient BiFunction<T, CommonCommandContext<?, ?>, Boolean> filter = (x, ctx) -> true;

    public EnumValue(@NotNull T value) {
        this(value, value.getDeclaringClass());
    }

    public EnumValue(@NotNull T value, Class<T> clazz) {
        super(value);
        this.clazz = clazz;
    }

    public EnumValue<T> filter(BiFunction<T, CommonCommandContext<?, ?>, Boolean> filter) {
        this.filter = filter;
        return this;
    }

    @Override
    protected List<ArgumentDefinition<T>> argumentDefinitions() {
        return List.of(new ArgumentDefinition<>(new CommonEnumArgument<>("name", clazz).validator(filter),
                                                (name, ctx) -> {
                                                    return name;
                                                }));
    }

    @Override
    protected String valueToString(T t) {
        return t.name();
    }
}
