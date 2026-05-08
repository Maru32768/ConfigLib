package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.argument.CommonEnumArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;
import net.kunmc.lab.configlib.MapValue;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public abstract class Enum2ObjectMapValue<T extends Enum<T>, V, U extends Enum2ObjectMapValue<T, V, U>> extends MapValue<T, V, U> {
    private transient final Class<T> clazz;
    private transient BiFunction<T, CommonCommandContext<?, ?>, Boolean> keyFilter = (x, ctx) -> true;

    public Enum2ObjectMapValue(Class<T> clazz, Map<T, V> value) {
        super(value);

        this.clazz = clazz;
    }

    public U filterForKey(BiFunction<T, CommonCommandContext<?, ?>, Boolean> filter) {
        this.keyFilter = filter;
        return ((U) this);
    }

    protected ArgumentDefinition<T> keyArgumentDefinitionForPut() {
        return new ArgumentDefinition<>(new CommonEnumArgument<>("name", clazz).validator(keyFilter), (name, ctx) -> {
            return name;
        });
    }

    @Override
    protected List<ArgumentDefinition<T>> argumentDefinitionsForRemove() {
        return List.of(new ArgumentDefinition<>(new CommonEnumArgument<>("name", clazz).validator(x -> {
            return value.containsKey(x);
        }), (name, ctx) -> {
            return name;
        }));
    }

    @Override
    protected String keyToString(T t) {
        return t.name();
    }
}
