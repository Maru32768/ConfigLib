package net.kunmc.lab.configlib.exception;

import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.configlib.ConfigCommandDescriptions;
import net.kunmc.lab.configlib.schema.ConfigSchemaPath;

import java.util.Objects;

/**
 * Runtime validation failure for one normalized config path.
 * <p>
 * ConfigLib throws this exception when file load, command mutation, undo, or
 * accepted programmatic mutation would leave a config entry with an invalid
 * value. The exception exposes the path, rejected value, and original
 * {@link InvalidValueException} so callers can log or report the failure.
 * </p>
 */
public final class ConfigValidationException extends RuntimeException {
    private final ConfigSchemaPath path;
    private final Object value;
    private final InvalidValueException validationCause;

    public ConfigValidationException(ConfigSchemaPath path, Object value, InvalidValueException validationCause) {
        super(buildMessage(path, value, validationCause), validationCause);
        this.path = Objects.requireNonNull(path, "path");
        this.value = value;
        this.validationCause = Objects.requireNonNull(validationCause, "validationCause");
    }

    private static String buildMessage(ConfigSchemaPath path, Object value, InvalidValueException validationCause) {
        String reason = validationCause.getMessage();
        if (reason == null || reason.isEmpty()) {
            reason = validationCause.getClass()
                                    .getSimpleName();
        }
        return "Validation failed for " + path.asString() + " (value: " + String.valueOf(value) + "): " + reason;
    }

    public ConfigSchemaPath path() {
        return path;
    }

    public Object value() {
        return value;
    }

    public InvalidValueException validationCause() {
        return validationCause;
    }

    public void sendMessage(CommonCommandContext<?, ?> ctx) {
        sendMessage(ctx, ConfigCommandDescriptions.defaultProvider());
    }

    public void sendMessage(CommonCommandContext<?, ?> ctx, ConfigCommandDescriptions.Provider descriptions) {
        ctx.sendFailure(descriptions.describe(ctx,
                                              ConfigCommandDescriptions.Key.VALIDATION_FAILED,
                                              path.asString(),
                                              String.valueOf(value)));
        validationCause.sendMessage(ctx);
    }
}
