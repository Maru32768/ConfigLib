package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.exception.CommandPrerequisiteException;
import net.kunmc.lab.configlib.exception.InvalidValueException;
import net.kunmc.lab.configlib.util.function.ExecutionCondition;
import net.kunmc.lab.configlib.util.function.Validator;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Base class for ConfigLib's typed Value API.
 * <p>
 * Value fields are the public API for configs that need command-aware behavior
 * such as custom parsing, display formatting, validation, execution guards, or
 * listeners. Metadata is configured with fluent methods on the value instance.
 * </p>
 *
 * @param <E> wrapped value type
 * @param <T> concrete fluent return type
 */
public abstract class Value<E, T extends Value<E, T>> {
    private transient final List<Consumer<E>> initializeListeners = new ArrayList<>();
    private transient final List<Consumer<E>> modifyListeners = new ArrayList<>();
    protected E value;
    private String description;
    private transient volatile boolean initialized = false;
    private transient E initValue;
    private transient Validator<E> validator = x -> {
    };
    private transient Function<E, String> displayFormatter;
    private transient String entryName;
    private transient ExecutionCondition executableIf;
    private boolean nullable;

    public Value(E value) {
        this.value = value;
        this.nullable = value == null;
    }

    /**
     * Returns the current raw value.
     */
    public E value() {
        return value;
    }

    /**
     * Replaces the current raw value.
     * <p>
     * Prefer {@link CommonBaseConfig#mutate(Runnable)} when changing values from
     * plugin code so ConfigLib can validate, save, record history, and dispatch
     * config-level change listeners as one accepted change.
     * </p>
     */
    public void value(E value) {
        this.value = value;
    }

    protected E copyValue(E value) {
        return value;
    }

    @Nullable
    public final String description() {
        return description;
    }

    /**
     * Sets the user-facing description for this value.
     * <p>
     * The description is used by generated commands and YAML comment generation.
     * </p>
     */
    @SuppressWarnings("unchecked")
    public final T description(String description) {
        this.description = description;
        return ((T) this);
    }

    /**
     * Sets a custom entry name used for this value in schema entries and generated commands.
     * By default, the Java field name is used.
     */
    @SuppressWarnings("unchecked")
    public final T entryName(String entryName) {
        this.entryName = entryName;
        return ((T) this);
    }

    /**
     * Sets a custom formatter for converting this value to a display string.
     * The formatter takes precedence over the default {@link #defaultDisplayString(Object)}
     * implementation and is used wherever the value is displayed as text, including list,
     * get, history, diff, and audit command output.
     *
     * <pre>{@code
     * new IntegerValue(10).displayFormatter(n -> n + " items")
     * }</pre>
     */
    @SuppressWarnings("unchecked")
    public final T displayFormatter(Function<@Nullable E, String> formatter) {
        this.displayFormatter = formatter;
        return (T) this;
    }

    /**
     * Allows this value to be {@code null}.
     * <p>
     * Values are non-null by default. Passing {@code null} to the constructor also
     * marks the value as nullable, so this method is mainly for values with a
     * non-null default that should still accept {@code null} from files or custom
     * code.
     * </p>
     */
    @SuppressWarnings("unchecked")
    public final T nullable() {
        this.nullable = true;
        return (T) this;
    }

    /**
     * Returns whether this value accepts {@code null}.
     */
    public final boolean isNullable() {
        return nullable;
    }

    final int valueHashCode() {
        if (value == null) {
            return 0;
        }
        return value.hashCode();
    }

    /**
     * Adds a listener that is triggered after the value has been initialized.
     * If initialization has already completed, the listener is triggered immediately.
     */
    @SuppressWarnings("unchecked")
    public final T onInitialize(Consumer<E> listener) {
        if (initialized) {
            listener.accept(initValue);
            return ((T) this);
        }

        initializeListeners.add(listener);
        return ((T) this);
    }

    final void dispatchInitialize(E initValue) {
        initializeListeners.forEach(x -> x.accept(initValue));
        this.initialized = true;
        this.initValue = initValue;
    }

    /**
     * Adds a listener triggered after this value is modified and accepted.
     * <p>
     * This listener is used for command changes, file reloads, and programmatic changes
     * that pass validation and are accepted by modification detection.
     * </p>
     */
    public final T onModify(Consumer<E> listener) {
        return onModify(listener, false);
    }

    /**
     * Adds a listener triggered after this value is modified and accepted.
     * If {@code triggeredOnInitialize} is true, the listener is also triggered after
     * the value has been initialized.
     *
     * @see Value#onInitialize(Consumer)
     */
    @SuppressWarnings("unchecked")
    public final T onModify(Consumer<E> listener, boolean triggeredOnInitialize) {
        modifyListeners.add(listener);
        if (triggeredOnInitialize) {
            onInitialize(listener);
        }

        return ((T) this);
    }

    final void dispatchModify(E newValue) {
        modifyListeners.forEach(x -> x.accept(newValue));
    }

    /**
     * Adds validation to this value.
     * <p>
     * Validators run through the schema validation pipeline for file load, command
     * mutation, and accepted programmatic changes. Throw {@link InvalidValueException}
     * to reject the value with a custom message.
     * </p>
     */
    @SuppressWarnings("unchecked")
    public final T addValidator(Validator<E> validator) {
        this.validator = this.validator.and(validator);
        return ((T) this);
    }

    public final void validate(E value) throws InvalidValueException {
        if (value == null) {
            if (nullable) {
                return;
            }
            throw new InvalidValueException(ctx -> ctx.sendFailure(ConfigCommandDescriptions.describe(ctx,
                                                                                                      ConfigCommandDescriptions.Key.VALUE_NOT_NULL)),
                                            "Value must not be null.");
        }
        validator.validate(value);
    }

    /**
     * Resolves the schema and command entry name for this value.
     *
     * @param fieldName Java field name to use when no override was set
     * @return the configured entry name or {@code fieldName}
     */
    public final String resolveEntryName(String fieldName) {
        if (entryName != null) {
            return entryName;
        }
        return fieldName;
    }

    /**
     * Sets a condition for this value's command execution.
     * Throw {@link net.kunmc.lab.commandlib.exception.CommandPrerequisiteException} to deny execution.
     *
     * <pre>{@code
     * new IntegerValue(10).executableIf(ctx -> {
     *     if (!ctx.getSender().isOp()) throw new CommandPrerequisiteException(c -> c.sendFailure("Ops only."));
     * })
     * }</pre>
     */
    @SuppressWarnings("unchecked")
    public final T executableIf(ExecutionCondition condition) {
        this.executableIf = condition;
        return (T) this;
    }

    /**
     * Sets a simple boolean condition. Uses a default error message when denied.
     */
    public final T executableIf(Predicate<CommonCommandContext<?, ?>> condition) {
        return executableIf(ctx -> {
            if (!condition.test(ctx)) {
                throw new CommandPrerequisiteException(c -> c.sendFailure(ConfigCommandDescriptions.describe(c,
                                                                                                             ConfigCommandDescriptions.Key.COMMAND_NOT_EXECUTABLE)));
            }
        });
    }

    final void checkExecutable(CommonCommandContext<?, ?> ctx) throws CommandPrerequisiteException {
        if (executableIf != null) {
            executableIf.check(ctx);
        }
    }

    final String displayString() {
        return displayString(value);
    }

    final String displayString(@Nullable E displayValue) {
        if (displayFormatter != null) {
            return displayFormatter.apply(displayValue);
        }
        return defaultDisplayString(displayValue);
    }

    protected String defaultDisplayString(@Nullable E value) {
        return String.valueOf(value);
    }

    @Override
    public String toString() {
        return String.format("%s{value=%s}", getClass().getSimpleName(), value);
    }
}
