package net.kunmc.lab.configlib;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import net.kunmc.lab.configlib.exception.ConfigValidationException;
import net.kunmc.lab.configlib.migration.MigrationDsl;
import net.kunmc.lab.configlib.migration.Migrations;
import net.kunmc.lab.configlib.schema.ConfigSchema;
import net.kunmc.lab.configlib.schema.ConfigSchemaEntry;
import net.kunmc.lab.configlib.schema.DisplayContext;
import net.kunmc.lab.configlib.store.*;
import net.kunmc.lab.configlib.util.ConfigUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class CommonBaseConfig {
    @SuppressWarnings("unused")
    @SerializedName(ConfigKeys.VERSION)
    private int schemaVersion = 0;
    protected final transient Timer timer = new Timer();
    protected final transient Logger logger = Logger.getLogger(getClass().getName());
    private final transient List<Runnable> onChangeListeners = new CopyOnWriteArrayList<>();
    private final transient Object ioLock = new Object();
    private final transient ConfigModificationDetector modificationDetector = new ConfigModificationDetector(this);
    private transient boolean listEnabled = true;
    private transient boolean reloadEnabled = true;
    private transient boolean resetEnabled = true;
    private transient boolean historyEnabled = true;
    transient volatile boolean initialized = false;
    private transient String entryName;
    private transient Migrations migrations;
    private transient ConfigStore configStore;
    private transient Closeable configStoreWatcher;
    private transient ConfigSchema schema;
    private transient Map<String, Object> defaultValues;

    protected CommonBaseConfig() {
        String s = getClass().getSimpleName();
        this.entryName = s.substring(0, 1)
                          .toLowerCase() + s.substring(1);
    }

    protected void entryName(@NotNull String entryName) {
        this.entryName = Objects.requireNonNull(entryName);
    }

    public final String entryName() {
        return entryName;
    }

    public final ConfigSchema schema() {
        return schema;
    }

    final void resetEntryToDefault(ConfigSchemaEntry<?> entry) {
        setSchemaValue(entry, copyDefaultValue(entry));
    }

    final void resetAllEntriesToDefault() {
        schema.entries()
              .forEach(this::resetEntryToDefault);
    }

    final void init(Option option) {
        migrations = option.migrations.build();
        schemaVersion = migrations.latestVersion();
        configStore = Objects.requireNonNull(createConfigStore());

        schema = ConfigSchema.fromConfig(this);
        defaultValues = schema.entries()
                              .stream()
                              .collect(LinkedHashMap::new,
                                       (m, v) -> m.put(v.entryName(), snapshotDefaultValue(v)),
                                       LinkedHashMap::putAll);

        try {
            saveConfigIfAbsent();
            loadConfig();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        if (configStore.readHistory(getClass(), migrations)
                       .isEmpty()) {
            ChangeTrace trace = configStore.lastAppliedMigrationResult()
                                           .map(x -> ChangeTrace.migration())
                                           .orElseGet(ChangeTrace::initial);
            configStore.pushHistory(this, trace);
            if (trace.source() == ChangeSource.INITIAL) {
                configStore.pushAudit(new AuditEntry(System.currentTimeMillis(),
                                                     trace,
                                                     schema.entries()
                                                           .stream()
                                                           .map(entry -> new AuditChange(entry.entryName(),
                                                                                         auditText(entry, null),
                                                                                         auditText(entry, entry.get())))
                                                           .collect(Collectors.toList())));
            } else {
                configStore.pushAudit(new AuditEntry(System.currentTimeMillis(), trace));
            }
        }

        modificationDetector.start(timer, option.modifyDetectionTimerPeriod);
        configStoreWatcher = configStore.startWatching(() -> {
            try {
                loadConfig();
            } catch (ConfigValidationException ex) {
                logValidationFailure(ex);
            }
        });
    }

    public final void onChange(Runnable listener) {
        onChangeListeners.add(listener);
    }

    final void dispatchOnChange() {
        onChangeListeners.forEach(Runnable::run);
    }

    final void detectModifications() {
        modificationDetector.detect();
    }

    final boolean isListEnabled() {
        return listEnabled;
    }

    final boolean isReloadEnabled() {
        return reloadEnabled;
    }

    final boolean isResetEnabled() {
        return resetEnabled;
    }

    final boolean isHistoryEnabled() {
        return historyEnabled;
    }

    protected final void disableList() {
        this.listEnabled = false;
    }

    protected final void disableReload() {
        this.reloadEnabled = false;
    }

    protected final void disableReset() {
        this.resetEnabled = false;
    }

    protected final void disableHistory() {
        this.historyEnabled = false;
    }

    /**
     * Creates the {@link ConfigStore} used for reading and writing this config.
     * Called once during {@link #init}.
     */
    protected abstract ConfigStore createConfigStore();

    protected void saveConfig() {
        withIoLock(this::saveConfigLocked);
    }

    protected void saveConfigIfAbsent() {
        withIoLock(() -> {
            if (!configStore.exists()) {
                saveConfigLocked();
            }
        });
    }

    protected void saveConfigIfPresent() {
        withIoLock(() -> {
            if (configStore.exists()) {
                saveConfigLocked();
            }
        });
    }

    final void pushHistory() {
        withIoLock(() -> {
            recordAcceptedChange(ChangeTrace.programmatic());
        });
    }

    /**
     * Runs a config mutation as one consistent change.
     * <p>
     * Use this when changing config values outside ConfigLib's built-in commands.
     * If the mutation changes any observed value after initialization, ConfigLib saves the
     * resulting config, appends one history entry, refreshes modification tracking, and
     * dispatches {@link #onChange(Runnable)} listeners.
     * </p>
     * <p>
     * Multiple value updates inside one mutation are treated as a single change. Read-only
     * operations should use {@link #inspect(Runnable)} or {@link #inspect(Supplier)} instead.
     * </p>
     *
     * @param mutation code that updates this config
     * @throws ConfigValidationException if the mutated config does not satisfy schema validation
     */
    public final void mutate(Runnable mutation) {
        mutate(mutation, ChangeTrace.programmatic());
    }

    public final void mutate(Runnable mutation, ChangeTrace trace) {
        withIoLock(() -> {
            Map<ConfigSchemaEntry<?>, Object> before = snapshotEntryValues();
            mutation.run();
            if (!initialized || !modificationDetector.isModified()) {
                return;
            }

            saveConfigLocked();
            recordAcceptedChange(resolveTrace(trace, before), before);
            modificationDetector.initializeHash();
            dispatchOnChange();
        });
    }

    /**
     * Runs read-only code while holding this config's consistency lock.
     * <p>
     * Use this for inspecting multiple fields, collections, maps, history, or formatted values
     * that should be based on a single stable config state. This method does not save the config,
     * append history, or dispatch change listeners.
     * </p>
     *
     * @param inspector read-only code to run
     */
    public final void inspect(Runnable inspector) {
        inspect(() -> {
            inspector.run();
            return null;
        });
    }

    /**
     * Runs read-only code while holding this config's consistency lock and returns its result.
     * <p>
     * Use this for deriving a value from multiple config fields without allowing a concurrent
     * mutation to interleave with the calculation. This method does not save the config, append
     * history, or dispatch change listeners.
     * </p>
     *
     * @param inspector read-only code to run
     * @param <T>       result type
     * @return the value returned by {@code inspector}
     */
    public final <T> T inspect(Supplier<T> inspector) {
        return withIoLock(inspector);
    }

    private void saveConfigLocked() {
        validateConfig(this);
        CommonBaseConfig saved = configStore.write(this, getClass(), migrations);
        replaceWithValidatedConfig(saved);
    }

    final void withIoLock(Runnable action) {
        withIoLock(() -> {
            action.run();
            return null;
        });
    }

    final <T> T withIoLock(Supplier<T> action) {
        synchronized (ioLock) {
            return action.get();
        }
    }

    public final boolean applyUndo(int historyIndex) {
        return applyUndo(historyIndex, ChangeTrace.undo("history[" + historyIndex + "]"));
    }

    public final boolean applyUndo(int historyIndex, ChangeTrace trace) {
        return withIoLock(() -> {
            if (!configStore.canRestoreHistoryIndex(historyIndex)) {
                return false;
            }
            Map<ConfigSchemaEntry<?>, Object> before = snapshotEntryValues();
            CommonBaseConfig historical = configStore.restoreHistoryIndex(getClass(), migrations, historyIndex);
            ConfigUtil.replaceFields(this, historical, this);
            saveConfigLocked();
            pushAudit(resolveTrace(trace, before), before);
            modificationDetector.initializeHash();
            dispatchOnChange();
            return true;
        });
    }

    public final List<HistoryEntry> readHistory() {
        return withIoLock(() -> {
            return configStore.readHistory(getClass(), migrations);
        });
    }

    public final List<AuditEntry> readAudit() {
        return withIoLock(configStore::readAudit);
    }

    /**
     * Loads this config from the backing store and applies it to the live config.
     *
     * @return true when stored config content was loaded; false when no store exists
     * @throws ConfigValidationException if the loaded config does not satisfy schema validation
     */
    @SuppressWarnings("unchecked")
    protected final boolean loadConfig() {
        return withIoLock(() -> {
            if (!configStore.exists()) {
                return false;
            }

            Map<ConfigSchemaEntry<?>, Object> before = initialized ? snapshotEntryValues() : Map.of();
            CommonBaseConfig config = configStore.read(getClass(), migrations, this);
            validateConfig(config);

            ConfigUtil.replaceFields(this, config, this);
            if (!initialized) {
                ConfigUtil.getValues(this)
                          .stream()
                          .map(Value.class::cast)
                          .forEach(x -> x.dispatchInitialize(x.value()));
                modificationDetector.initializeHash();
                initialized = true;
                return true;
            }

            List<String> changedPaths = changedPaths(before);
            if (!changedPaths.isEmpty()) {
                recordAcceptedChange(ChangeTrace.file(changedPaths), before);
                modificationDetector.initializeHash();
            }
            dispatchOnChange();
            return true;
        });
    }

    private void replaceWithValidatedConfig(CommonBaseConfig config) {
        validateConfig(config);
        ConfigUtil.replaceFields(this, config, this);
    }

    @Nullable
    private Object snapshotDefaultValue(ConfigSchemaEntry<?> entry) {
        Object value = entry.get();
        Value<?, ?> backingValue = resolveValueField(entry);
        if (backingValue != null) {
            return copyValueDefault(backingValue, value);
        }
        return configStore.copyValue(entry.field()
                                          .getGenericType(), value);
    }

    @Nullable
    final Object copyDefaultValue(ConfigSchemaEntry<?> entry) {
        Object value = defaultValues.get(entry.entryName());
        Value<?, ?> backingValue = resolveValueField(entry);
        if (backingValue != null) {
            return copyValueDefault(backingValue, value);
        }
        return configStore.copyValue(entry.field()
                                          .getGenericType(), value);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static Object copyValueDefault(Value<?, ?> value, Object rawValue) {
        return ((Value<Object, ?>) value).copyValue(rawValue);
    }

    @Nullable
    private Value<?, ?> resolveValueField(ConfigSchemaEntry<?> entry) {
        if (!Value.class.isAssignableFrom(entry.field()
                                               .getType())) {
            return null;
        }

        try {
            Object value = entry.field()
                                .get(this);
            return value instanceof Value ? (Value<?, ?>) value : null;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void setSchemaValue(ConfigSchemaEntry<?> entry, Object newValue) {
        ((ConfigSchemaEntry<Object>) entry).set(newValue);
    }

    final void validateCurrentConfig() {
        validateConfig(this);
    }

    final void recordAcceptedChange(ChangeTrace trace) {
        configStore.pushHistory(this, trace);
        configStore.pushAudit(new AuditEntry(System.currentTimeMillis(), trace));
    }

    final void recordAcceptedChange(ChangeTrace trace, Map<ConfigSchemaEntry<?>, Object> before) {
        configStore.pushHistory(this, trace);
        pushAudit(trace, before);
    }

    private Map<ConfigSchemaEntry<?>, Object> snapshotEntryValues() {
        Map<ConfigSchemaEntry<?>, Object> snapshot = new LinkedHashMap<>();
        for (ConfigSchemaEntry<?> entry : schema.entries()) {
            snapshot.put(entry, copyEntryValue(entry, entry.get()));
        }
        return snapshot;
    }

    private List<String> changedPaths(Map<ConfigSchemaEntry<?>, Object> before) {
        List<String> changedPaths = new ArrayList<>();
        for (Map.Entry<ConfigSchemaEntry<?>, Object> entry : before.entrySet()) {
            Object current = entry.getKey()
                                  .get();
            if (!Objects.equals(entry.getValue(), current)) {
                changedPaths.add(entry.getKey()
                                      .entryName());
            }
        }
        return changedPaths;
    }

    private ChangeTrace resolveTrace(ChangeTrace trace, Map<ConfigSchemaEntry<?>, Object> before) {
        if (trace.hasPaths()) {
            return trace;
        }
        return trace.withPaths(changedPaths(before));
    }

    private void pushAudit(ChangeTrace trace, Map<ConfigSchemaEntry<?>, Object> before) {
        configStore.pushAudit(new AuditEntry(System.currentTimeMillis(), trace, auditChanges(before)));
    }

    private List<AuditChange> auditChanges(Map<ConfigSchemaEntry<?>, Object> before) {
        List<AuditChange> changes = new ArrayList<>();
        for (Map.Entry<ConfigSchemaEntry<?>, Object> entry : before.entrySet()) {
            Object current = entry.getKey()
                                  .get();
            if (!Objects.equals(entry.getValue(), current)) {
                changes.add(new AuditChange(entry.getKey()
                                                 .entryName(),
                                            auditText(entry.getKey(), entry.getValue()),
                                            auditText(entry.getKey(), current)));
            }
        }
        return changes;
    }

    private String auditText(ConfigSchemaEntry<?> entry, Object value) {
        return entry.displayString(value, DisplayContext.raw());
    }

    private Object copyEntryValue(ConfigSchemaEntry<?> entry, Object value) {
        Value<?, ?> backingValue = resolveValueField(entry);
        if (backingValue != null) {
            return copyValueDefault(backingValue, value);
        }
        return configStore.copyValue(entry.field()
                                          .getGenericType(), value);
    }

    void logValidationFailure(ConfigValidationException ex) {
        logger.log(Level.WARNING, ex.getMessage(), ex);
    }

    private void validateConfig(CommonBaseConfig config) {
        for (ConfigSchemaEntry<?> entry : schema.entries()) {
            Object value = entry.get(config);
            ConfigSchemaValidation.validate(entry, value);
        }
    }

    protected final void close() {
        timer.cancel();
        try {
            if (configStoreWatcher != null) {
                configStoreWatcher.close();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static class Option {
        int modifyDetectionTimerPeriod = 100;
        final Migrations.Builder migrations = Migrations.builder();

        public Option() {
        }

        public Option modifyDetectionTimerPeriod(int period) {
            Preconditions.checkArgument(period > 0);
            this.modifyDetectionTimerPeriod = period;
            return this;
        }

        /**
         * Registers a schema migration for the given target version.
         * <p>
         * Migrations run automatically in ascending version order when an existing config file has an
         * older {@value ConfigKeys#VERSION} value. New installations start at the latest registered
         * version and do not run historical migrations.
         * </p>
         * <p>
         * Version numbers are explicit to keep migration history stable when declarations are moved or
         * reordered in source. Register each version at most once.
         * </p>
         *
         * @param version   target schema version for this migration step
         * @param migration migration operations to apply for that version
         * @return this option instance
         */
        public Option migrateTo(int version, Consumer<MigrationDsl> migration) {
            migrations.migrateTo(version, Objects.requireNonNull(migration));
            return this;
        }
    }

}
