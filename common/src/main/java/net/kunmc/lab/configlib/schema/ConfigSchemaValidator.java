package net.kunmc.lab.configlib.schema;

import net.kunmc.lab.configlib.exception.InvalidValueException;

import java.util.Objects;

/**
 * Validation function used by normalized schema entries.
 * <p>
 * This is the supported extension point for custom schema-level validation.
 * Implementations should throw {@link InvalidValueException} when a value is not
 * acceptable and return normally otherwise.
 * </p>
 *
 * @param <E> validated value type
 */
@FunctionalInterface
public interface ConfigSchemaValidator<E> {
    void validate(E value) throws InvalidValueException;

    default ConfigSchemaValidator<E> and(ConfigSchemaValidator<E> other) {
        Objects.requireNonNull(other);
        return v -> {
            validate(v);
            other.validate(v);
        };
    }

    static <E> ConfigSchemaValidator<E> noOp() {
        return v -> {
        };
    }
}
