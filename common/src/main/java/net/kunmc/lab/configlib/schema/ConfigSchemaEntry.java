package net.kunmc.lab.configlib.schema;

import net.kunmc.lab.configlib.CommonBaseConfig;
import net.kunmc.lab.configlib.annotation.Masked;
import net.kunmc.lab.configlib.exception.InvalidValueException;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * One normalized config entry in a {@link ConfigSchema}.
 * <p>
 * This is the stable read-facing schema API for code that needs to inspect
 * ConfigLib entries without caring whether they are backed by the Value API or
 * the POJO API. Public consumers should rely on {@link #path()}, {@link #entryName()},
 * {@link #metadata()}, {@link #isMasked()}, {@link #validate(Object)}, {@link #get()},
 * {@link #displayString()}, and {@link #supportsModificationCommand()}.
 * </p>
 * <p>
 * Methods marked {@link ApiStatus.Internal} are runtime hooks for ConfigLib's
 * stores, generated commands, mutation detector, history, diff, undo, and audit
 * pipeline. They remain public for cross-package implementation reasons but are
 * not part of the user extension contract.
 * </p>
 *
 * @param <E> entry value type
 */
public abstract class ConfigSchemaEntry<E> {
    private final ConfigSchemaPath path;
    private final String entryName;
    private final Field field;
    private final ConfigSchemaMetadata metadata;

    @ApiStatus.Internal
    protected ConfigSchemaEntry(ConfigSchemaPath path, String entryName, Field field, ConfigSchemaMetadata metadata) {
        this.path = Objects.requireNonNull(path, "path");
        this.entryName = Objects.requireNonNull(entryName, "entryName");
        this.field = Objects.requireNonNull(field, "field");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
    }

    public ConfigSchemaPath path() {
        return path;
    }

    public String entryName() {
        return entryName;
    }

    @ApiStatus.Internal
    public Field field() {
        return field;
    }

    public ConfigSchemaMetadata metadata() {
        return metadata;
    }

    public boolean isMasked() {
        return field.isAnnotationPresent(Masked.class);
    }

    public abstract void validate(E value) throws InvalidValueException;

    public abstract E get();

    @ApiStatus.Internal
    public abstract E get(CommonBaseConfig config);

    @ApiStatus.Internal
    public abstract void set(E value);

    @ApiStatus.Internal
    public abstract Object commandObject();

    public abstract boolean supportsModificationCommand();

    public final String displayString() {
        return displayString(get(), DisplayContext.raw());
    }

    public final String displayString(DisplayContext context) {
        return displayString(get(), context);
    }

    public final String displayString(Object fieldValue) {
        return displayString(fieldValue, DisplayContext.raw());
    }

    public final String displayString(Object fieldValue, DisplayContext context) {
        return context.display(displayRawString(fieldValue), this);
    }

    protected abstract String displayRawString(Object fieldValue);

    @ApiStatus.Internal
    public abstract int sourceHash();

    @ApiStatus.Internal
    public abstract void dispatchModify();
}
