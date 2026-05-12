package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.argument.EnumArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

public class UUID2EnumMapValue<T extends Enum<T>> extends UUID2ObjectMapValue<T, UUID2EnumMapValue<T>> {
    private final transient Class<T> clazz;
    private transient BiFunction<T, CommandContext, Boolean> valueFilter = (x, ctx) -> true;

    public UUID2EnumMapValue(Class<T> clazz) {
        super(new HashMap<>());
        this.clazz = clazz;
    }

    public UUID2EnumMapValue<T> filterForValue(BiFunction<T, CommandContext, Boolean> filter) {
        this.valueFilter = filter;
        return this;
    }

    @Override
    protected List<PutArgumentDefinition<UUID, T>> argumentDefinitionsForPut() {
        return List.of(new PutArgumentDefinition<>(keyArgumentDefinitionForPut(),
                                                   new ArgumentDefinition<>(new EnumArgument<>("name", clazz).validator(
                                                           valueFilter), (t, ctx) -> t)));
    }

    @Override
    protected String valueToString(T t) {
        return t.name();
    }
}
