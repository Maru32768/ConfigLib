package net.kunmc.lab.configlib.schema;

import org.jetbrains.annotations.Nullable;

/**
 * Public metadata attached to a normalized config schema entry.
 * <p>
 * Metadata is read-only once the schema has been built. Value API metadata comes
 * from fluent value declarations, while POJO metadata comes from annotations.
 * </p>
 */
public final class ConfigSchemaMetadata {
    private final String description;

    public ConfigSchemaMetadata(@Nullable String description) {
        this.description = description;
    }

    @Nullable
    public String description() {
        return description;
    }

    public boolean hasDescription() {
        return description != null;
    }
}
