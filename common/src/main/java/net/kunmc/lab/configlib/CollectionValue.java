package net.kunmc.lab.configlib;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class CollectionValue<T extends Collection<E>, E, U extends CollectionValue<T, E, U>> extends Value<T, U> {
    private final transient List<Consumer<T>> addListeners = new ArrayList<>();
    private final transient List<Consumer<T>> removeListeners = new ArrayList<>();
    private final transient List<Runnable> clearListeners = new ArrayList<>();
    private transient boolean addable = true;
    private transient boolean removable = true;
    private transient boolean clearable = true;

    public CollectionValue(T value) {
        super(value);
    }

    public final U disableAdd() {
        this.addable = false;
        return ((U) this);
    }

    protected final boolean isAddEnabled() {
        return addable;
    }

    protected abstract List<ArgumentDefinition<T>> argumentDefinitionsForAdd();

    /**
     * Adds a listener fired only after an element is added by a generated command.
     */
    public final U onAdd(Consumer<T> listener) {
        addListeners.add(listener);
        return ((U) this);
    }

    protected final void dispatchAdd(T newValue) {
        addListeners.forEach(x -> x.accept(newValue));
    }

    public final U disableRemove() {
        this.removable = false;
        return ((U) this);
    }

    protected final boolean isRemoveEnabled() {
        return removable;
    }

    protected abstract List<ArgumentDefinition<T>> argumentDefinitionsForRemove();

    /**
     * Adds a listener fired only after an element is removed by a generated command.
     */
    public final U onRemove(Consumer<T> listener) {
        removeListeners.add(listener);
        return ((U) this);
    }

    final void dispatchRemove(T newValue) {
        removeListeners.forEach(x -> x.accept(newValue));
    }

    public final U disableClear() {
        this.clearable = false;
        return ((U) this);
    }

    protected final boolean isClearEnabled() {
        return clearable;
    }

    /**
     * Adds a listener fired only after this collection is cleared by a generated command.
     */
    public final U onClear(Runnable listener) {
        clearListeners.add(listener);
        return ((U) this);
    }

    final void dispatchClear() {
        clearListeners.forEach(Runnable::run);
    }

    public abstract T toAdded(E... elements);

    public abstract T toRemoved(E... elements);

    protected abstract String elementToString(E e);

    final String elementsToString(Collection<E> elements) {
        if (elements.size() == 1) {
            return elementToString(elements.iterator()
                                           .next());
        }

        return "[" + elements.stream()
                             .map(this::elementToString)
                             .collect(Collectors.joining(", ")) + "]";
    }

    @Override
    protected String defaultDisplayString(@Nullable T t) {
        if (t == null) {
            return "null";
        }

        return "[" + t.stream()
                      .map(this::elementToString)
                      .collect(Collectors.joining(", ")) + "]";
    }
}
