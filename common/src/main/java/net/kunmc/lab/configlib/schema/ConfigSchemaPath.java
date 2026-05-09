package net.kunmc.lab.configlib.schema;

import java.util.Objects;

/**
 * Dotted path of a normalized config schema entry.
 * <p>
 * Top-level entries use their field name. Nested POJO entries use dot-separated
 * field names such as {@code arena.maxPlayers}.
 * </p>
 */
public final class ConfigSchemaPath {
    private final String path;

    public ConfigSchemaPath(String path) {
        this.path = Objects.requireNonNull(path, "path");
    }

    public String asString() {
        return path;
    }

    @Override
    public String toString() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ConfigSchemaPath)) {
            return false;
        }
        return path.equals(((ConfigSchemaPath) o).path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}
