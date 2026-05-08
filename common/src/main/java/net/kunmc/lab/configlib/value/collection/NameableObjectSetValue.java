package net.kunmc.lab.configlib.value.collection;

import net.kunmc.lab.commandlib.argument.Nameable;
import net.kunmc.lab.commandlib.argument.CommonNameableObjectArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;
import net.kunmc.lab.configlib.util.NameableSet;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class NameableObjectSetValue<T extends Nameable> extends SetValue<T, NameableObjectSetValue<T>> {
    private transient final Collection<? extends T> candidates;
    private transient final Predicate<? super T> filter;

    public NameableObjectSetValue(Collection<? extends T> candidates) {
        this(candidates, x -> true);
    }

    public NameableObjectSetValue(Collection<? extends T> candidates, Predicate<? super T> filter) {
        super(new NameableSet<>());

        this.candidates = candidates;
        this.filter = filter;
    }

    @Override
    protected List<ArgumentDefinition<Set<T>>> argumentDefinitionsForAdd() {
        return List.of(new ArgumentDefinition<>(new CommonNameableObjectArgument<>("name", candidates).validator(x -> {
            if (value.stream()
                     .map(Nameable::tabCompleteName)
                     .anyMatch(y -> y.equals(x.tabCompleteName()))) {
                return false;
            }

            return filter == null || filter.test(x);
        }), (name, ctx) -> {
            return Set.of(name);
        }));
    }

    @Override
    protected List<ArgumentDefinition<Set<T>>> argumentDefinitionsForRemove() {
        return List.of(new ArgumentDefinition<>(new CommonNameableObjectArgument<>("name", candidates).validator(x -> {
            return value.stream()
                        .map(Nameable::tabCompleteName)
                        .anyMatch(y -> y.equals(x.tabCompleteName()));
        }), (name, ctx) -> {
            return Set.of(name);
        }));
    }

    @Override
    protected String elementToString(T t) {
        return t.tabCompleteName();
    }

    @Override
    public void value(Set<T> value) {
        this.value = new NameableSet<>(value);
    }
}
