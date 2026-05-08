package net.kunmc.lab.configlib.value.collection;

import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.argument.CommonEnumArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class EnumListValue<T extends Enum<T>> extends ListValue<T, EnumListValue<T>> {
    private final transient Class<T> clazz;
    private transient BiFunction<T, CommonCommandContext<?, ?>, Boolean> filterForAdd = (x, ctx) -> true;

    public EnumListValue(Class<T> clazz) {
        super(new ArrayList<>());
        this.clazz = clazz;
    }

    public EnumListValue<T> filterForAdd(BiFunction<T, CommonCommandContext<?, ?>, Boolean> filter) {
        this.filterForAdd = filter;
        return this;
    }

    @Override
    protected List<ArgumentDefinition<List<T>>> argumentDefinitionsForAdd() {
        return List.of(new ArgumentDefinition<>(new CommonEnumArgument<>("name", clazz).validator(filterForAdd),
                                                (name, ctx) -> {
                                                    return List.of(name);
                                                }));
    }

    @Override
    protected List<ArgumentDefinition<List<T>>> argumentDefinitionsForRemove() {
        return List.of(new ArgumentDefinition<>(new CommonEnumArgument<>("name", clazz).validator(x -> {
            return value.contains(x);
        }), (name, ctx) -> {
            return List.of(name);
        }));
    }

    @Override
    protected String elementToString(T t) {
        return t.name();
    }
}
