package net.kunmc.lab.configlib.store;

/**
 * Thrown when stored config content is syntactically valid but not a valid
 * ConfigLib document shape.
 */
public final class InvalidConfigFormatException extends RuntimeException {
    public InvalidConfigFormatException(String message) {
        super(message);
    }
}
