package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.argument.CommonEnumArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;

public class String2EnumMapValue<T extends Enum<T>> extends String2ObjectMapValue<T, String2EnumMapValue<T>> {
    private final transient Class<T> clazz;
    private transient BiFunction<T, CommonCommandContext<?, ?>, Boolean> valueFilter = (x, ctx) -> true;

    public String2EnumMapValue(Class<T> clazz) {
        super(new HashMap<>());
        this.clazz = clazz;
    }

    public String2EnumMapValue<T> filterForValue(BiFunction<T, CommonCommandContext<?, ?>, Boolean> filter) {
        this.valueFilter = filter;
        return this;
    }

    @Override
    protected List<PutArgumentDefinition<String, T>> argumentDefinitionsForPut() {
        return List.of(new PutArgumentDefinition<>(keyArgumentDefinitionForPut(),
                                                   new ArgumentDefinition<>(new CommonEnumArgument<>("name",
                                                                                                     clazz).validator(
                                                           valueFilter), (name, ctx) -> {
                                                       return name;
                                                   })));
    }

    @Override
    protected String valueToString(T t) {
        return t.name();
    }
}
