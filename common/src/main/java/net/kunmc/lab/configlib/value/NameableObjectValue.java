package net.kunmc.lab.configlib.value;

import net.kunmc.lab.commandlib.argument.CommonNameableObjectArgument;
import net.kunmc.lab.commandlib.argument.Nameable;
import net.kunmc.lab.configlib.ArgumentDefinition;
import net.kunmc.lab.configlib.SingleValue;
import net.kunmc.lab.configlib.util.ListUtil;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class NameableObjectValue<T extends Nameable> extends SingleValue<T, NameableObjectValue<T>> {
    private transient final Collection<? extends T> candidates;
    private transient final Predicate<? super T> filter;

    public NameableObjectValue(T initial, T candidate, T... candidates) {
        this(initial, ListUtil.asList(candidate, candidates));
    }

    public NameableObjectValue(T initial, Predicate<T> filter, T candidate, T... candidates) {
        this(initial, ListUtil.asList(candidate, candidates), filter);
    }

    public NameableObjectValue(T initial, Collection<? extends T> candidates) {
        this(initial, candidates, x -> true);
    }

    public NameableObjectValue(T initial, Collection<? extends T> candidates, Predicate<? super T> filter) {
        super(initial);

        this.candidates = candidates;
        this.filter = filter;
    }

    @Override
    protected List<ArgumentDefinition<T>> argumentDefinitions() {
        return List.of(new ArgumentDefinition<>(new CommonNameableObjectArgument<>("name",
                                                                                   candidates).validator(filter),
                                                (name, ctx) -> {
                                                    return name;
                                                }));
    }

    @Override
    protected String valueToString(T t) {
        return value.tabCompleteName();
    }
}
