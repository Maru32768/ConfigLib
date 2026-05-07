package net.kunmc.lab.configlib.store;

import net.kunmc.lab.configlib.CommonBaseConfig;
import net.kunmc.lab.configlib.migration.Migrations;

import java.io.Closeable;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

public interface ConfigStore {
    UnknownKeyPolicy unknownKeyPolicy();

    void unknownKeyPolicy(UnknownKeyPolicy unknownKeyPolicy);

    boolean exists();

    CommonBaseConfig read(Class<? extends CommonBaseConfig> clazz, Migrations migrations, CommonBaseConfig defaults);

    CommonBaseConfig write(CommonBaseConfig config, Class<? extends CommonBaseConfig> clazz, Migrations migrations);

    /**
     * Creates a detached copy of a value using this store's configured serialization rules.
     */
    Object copyValue(Type type, Object value);

    /**
     * Returns the migration result that would be produced for the currently stored document, if any.
     * Intended for diagnostics and tests rather than normal config loading, and typically backed by
     * {@link Migrations#execute(int, com.google.gson.Gson, com.google.gson.JsonObject)}.
     */
    Optional<Migrations.MigrationResult> previewMigrations(Migrations migrations);

    /**
     * Start watching for external changes to the config data.
     * The returned {@link Closeable} stops watching when closed.
     * {@code onChanged} is called when a change is detected.
     */
    Closeable startWatching(Runnable onChanged);

    /**
     * Save the current config state to history with its origin.
     */
    void pushHistory(CommonBaseConfig config, ChangeTrace trace);

    /**
     * Returns true if the history entry at {@code historyIndex} can be restored.
     */
    boolean canRestoreHistoryIndex(int historyIndex);

    /**
     * Discards newer entries and returns the requested entry as a deserialized config.
     * Only call when {@link #canRestoreHistoryIndex(int)} is true.
     */
    CommonBaseConfig restoreHistoryIndex(Class<? extends CommonBaseConfig> clazz,
                                         Migrations migrations,
                                         int historyIndex);

    /**
     * Returns all history entries, most recent first.
     */
    List<HistoryEntry> readHistory(Class<? extends CommonBaseConfig> clazz, Migrations migrations);

    /**
     * Appends an accepted change event to the audit log.
     */
    void pushAudit(AuditEntry entry);

    /**
     * Returns all audit entries, most recent first.
     */
    List<AuditEntry> readAudit();

    /**
     * Returns the most recent migration result applied during {@link #read}, if that read migrated data.
     */
    Optional<Migrations.MigrationResult> lastAppliedMigrationResult();
}
