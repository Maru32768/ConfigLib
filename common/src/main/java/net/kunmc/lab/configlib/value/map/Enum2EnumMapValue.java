package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.argument.CommonEnumArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;

public class Enum2EnumMapValue<K extends Enum<K>, V extends Enum<V>> extends Enum2ObjectMapValue<K, V, Enum2EnumMapValue<K, V>> {
    private final transient Class<V> clazz;
    private transient BiFunction<V, CommonCommandContext<?, ?>, Boolean> valueFilter = (x, ctx) -> true;

    public Enum2EnumMapValue(Class<K> keyClass, Class<V> valueClass) {
        super(keyClass, new HashMap<>());
        this.clazz = valueClass;
    }

    public Enum2EnumMapValue<K, V> filterForValue(BiFunction<V, CommonCommandContext<?, ?>, Boolean> filter) {
        this.valueFilter = filter;
        return this;
    }

    @Override
    protected List<PutArgumentDefinition<K, V>> argumentDefinitionsForPut() {
        return List.of(new PutArgumentDefinition<>(keyArgumentDefinitionForPut(),
                                                   new ArgumentDefinition<>(new CommonEnumArgument<>("value",
                                                                                                     clazz).validator(
                                                           valueFilter), (v, ctx) -> {
                                                       return v;
                                                   })));
    }

    @Override
    protected String valueToString(V v) {
        return v.name();
    }
}
