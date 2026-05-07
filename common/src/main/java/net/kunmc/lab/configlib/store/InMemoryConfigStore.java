package net.kunmc.lab.configlib.store;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kunmc.lab.configlib.CommonBaseConfig;
import net.kunmc.lab.configlib.ConfigKeys;
import net.kunmc.lab.configlib.migration.MigrationExecutionException;
import net.kunmc.lab.configlib.migration.Migrations;
import net.kunmc.lab.configlib.schema.ConfigSchema;

import java.io.Closeable;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Logger;

public class InMemoryConfigStore implements ConfigStore {
    private final Gson gson;
    private final int maxHistorySize;
    private final transient Logger logger = Logger.getLogger(InMemoryConfigStore.class.getName());
    private String data = null;
    private final Deque<String> history = new ArrayDeque<>();
    private final Deque<String> audit = new ArrayDeque<>();
    private UnknownKeyPolicy unknownKeyPolicy = UnknownKeyPolicy.PRESERVE;
    private transient Migrations.MigrationResult lastAppliedMigrationResult;

    public InMemoryConfigStore(Gson gson) {
        this(gson, 50);
    }

    public InMemoryConfigStore(Gson gson, int maxHistorySize) {
        this.gson = gson;
        this.maxHistorySize = maxHistorySize;
    }

    @Override
    public UnknownKeyPolicy unknownKeyPolicy() {
        return unknownKeyPolicy;
    }

    @Override
    public void unknownKeyPolicy(UnknownKeyPolicy unknownKeyPolicy) {
        this.unknownKeyPolicy = Objects.requireNonNull(unknownKeyPolicy, "unknownKeyPolicy");
    }

    @Override
    public boolean exists() {
        return data != null;
    }

    @Override
    public CommonBaseConfig read(Class<? extends CommonBaseConfig> clazz,
                                 Migrations migrations,
                                 CommonBaseConfig defaults) {
        JsonObject jsonObject = JsonParser.parseString(data)
                                          .getAsJsonObject();
        int storedVersion = jsonObject.has(ConfigKeys.VERSION) ? jsonObject.get(ConfigKeys.VERSION)
                                                                           .getAsInt() : 0;
        Migrations.MigrationResult migrationResult;
        try {
            migrationResult = migrations.execute(storedVersion, gson, jsonObject);
        } catch (MigrationExecutionException e) {
            logger.severe(e.getMessage());
            throw e;
        }
        lastAppliedMigrationResult = migrationResult.migrated() ? migrationResult : null;
        if (migrationResult.migrated()) {
            jsonObject = migrationResult.document();
            jsonObject.addProperty(ConfigKeys.VERSION, migrations.latestVersion());
            data = gson.toJson(jsonObject);
            logMigrationResult(migrationResult);
        }
        ConfigSchema schema = ConfigSchema.fromConfig(defaults);
        unknownKeyPolicy.apply(jsonObject, schema);
        JsonObject defaultObject = JsonConfigDefaults.fromConfig(defaults, gson);
        JsonObject merged = JsonConfigDefaults.fillMissing(jsonObject, defaultObject);
        return gson.fromJson(merged, clazz);
    }

    @Override
    public CommonBaseConfig write(CommonBaseConfig config,
                                  Class<? extends CommonBaseConfig> clazz,
                                  Migrations migrations) {
        JsonObject memory = gson.toJsonTree(config)
                                .getAsJsonObject();
        ConfigSchema schema = ConfigSchema.fromConfig(config);
        if (data != null) {
            JsonObject disk = JsonParser.parseString(data)
                                        .getAsJsonObject();
            unknownKeyPolicy.apply(disk, schema);
            unknownKeyPolicy.beforeWrite(memory, disk, schema);
        }
        unknownKeyPolicy.apply(memory, schema);
        data = gson.toJson(memory);
        return gson.fromJson(memory, clazz);
    }

    @Override
    public Object copyValue(Type type, Object value) {
        if (value == null) {
            return null;
        }
        return gson.fromJson(gson.toJsonTree(value, type), type);
    }

    @Override
    public Optional<Migrations.MigrationResult> previewMigrations(Migrations migrations) {
        if (!exists()) {
            return Optional.empty();
        }

        JsonObject jsonObject = JsonParser.parseString(data)
                                          .getAsJsonObject();
        int storedVersion = jsonObject.has(ConfigKeys.VERSION) ? jsonObject.get(ConfigKeys.VERSION)
                                                                           .getAsInt() : 0;
        return Optional.of(migrations.execute(storedVersion, gson, jsonObject));
    }

    public void write(CommonBaseConfig config) {
        data = gson.toJson(config);
    }

    @Override
    public void pushHistory(CommonBaseConfig config, ChangeTrace trace) {
        JsonObject obj = JsonParser.parseString(gson.toJson(config))
                                   .getAsJsonObject();
        writeTrace(obj, trace, System.currentTimeMillis());
        history.addFirst(gson.toJson(obj));
        while (history.size() > maxHistorySize) {
            history.removeLast();
        }
    }

    @Override
    public boolean canRestoreHistoryIndex(int historyIndex) {
        return historyIndex > 0 && history.size() > historyIndex;
    }

    @Override
    public CommonBaseConfig restoreHistoryIndex(Class<? extends CommonBaseConfig> clazz,
                                                Migrations migrations,
                                                int historyIndex) {
        for (int i = 0; i < historyIndex; i++) {
            history.removeFirst();
        }
        String snapshot = history.peekFirst();
        return gson.fromJson(JsonParser.parseString(snapshot)
                                       .getAsJsonObject(), clazz);
    }

    @Override
    public List<HistoryEntry> readHistory(Class<? extends CommonBaseConfig> clazz, Migrations migrations) {
        List<HistoryEntry> result = new ArrayList<>();
        for (String snapshot : history) {
            JsonObject obj = JsonParser.parseString(snapshot)
                                       .getAsJsonObject();
            long ts = obj.has(ChangeLogKeys.TIMESTAMP) ? obj.get(ChangeLogKeys.TIMESTAMP)
                                                            .getAsLong() : 0L;
            result.add(new HistoryEntry(ts, readTrace(obj), gson.fromJson(obj, clazz)));
        }
        return result;
    }

    @Override
    public void pushAudit(AuditEntry entry) {
        JsonObject obj = new JsonObject();
        writeAuditEntry(obj, entry, entry.timestamp());
        audit.addFirst(gson.toJson(obj));
        while (audit.size() > maxHistorySize) {
            audit.removeLast();
        }
    }

    @Override
    public List<AuditEntry> readAudit() {
        List<AuditEntry> result = new ArrayList<>();
        for (String snapshot : audit) {
            JsonObject obj = JsonParser.parseString(snapshot)
                                       .getAsJsonObject();
            long ts = obj.get(ChangeLogKeys.TIMESTAMP)
                         .getAsLong();
            result.add(readAuditEntry(obj, ts));
        }
        return result;
    }

    @Override
    public Optional<Migrations.MigrationResult> lastAppliedMigrationResult() {
        return Optional.ofNullable(lastAppliedMigrationResult);
    }

    /**
     * For testing: directly set the raw JSON string as stored content.
     */
    public void writeRaw(String json) {
        data = json;
    }

    /**
     * For testing: get the raw JSON string as currently stored.
     */
    public String readRaw() {
        return data;
    }

    @Override
    public Closeable startWatching(Runnable onChanged) {
        return () -> {
        };
    }

    private void logMigrationResult(Migrations.MigrationResult migrationResult) {
        logger.info("Applied config migrations from v" + migrationResult.storedVersion() + " to v" + migrationResult.targetVersion());
        for (Migrations.VersionReport versionReport : migrationResult.versionReports()) {
            for (Migrations.OperationReport operation : versionReport.operations()) {
                if (!operation.applied()) {
                    continue;
                }
                logger.info("  v" + versionReport.version() + ": " + formatOperation(operation));
            }
        }
    }

    private static String formatOperation(Migrations.OperationReport operation) {
        if (operation.targetPath() != null) {
            return operation.type()
                            .displayName() + " " + operation.path() + " -> " + operation.targetPath();
        }
        return operation.type()
                        .displayName() + " " + operation.path();
    }

    private static void writeTrace(JsonObject obj, ChangeTrace trace, long timestamp) {
        obj.addProperty(ChangeLogKeys.TIMESTAMP, timestamp);
        obj.addProperty(ChangeLogKeys.SOURCE,
                        trace.source()
                             .name());
        if (trace.actor()
                 .name() != null) {
            obj.addProperty(ChangeLogKeys.ACTOR_NAME,
                            trace.actor()
                                 .name());
        }
        if (trace.actor()
                 .uuid() != null) {
            obj.addProperty(ChangeLogKeys.ACTOR_UUID,
                            trace.actor()
                                 .uuid());
        }
        if (trace.reason() != null) {
            obj.addProperty(ChangeLogKeys.REASON, trace.reason());
        }
        if (!trace.paths()
                  .isEmpty()) {
            JsonArray array = new JsonArray();
            trace.paths()
                 .forEach(array::add);
            obj.add(ChangeLogKeys.PATHS, array);
        }
    }

    private static ChangeTrace readTrace(JsonObject obj) {
        ChangeSource source = obj.has(ChangeLogKeys.SOURCE) ? ChangeSource.valueOf(obj.get(ChangeLogKeys.SOURCE)
                                                                                      .getAsString()) : ChangeSource.PROGRAMMATIC;
        String actorName = obj.has(ChangeLogKeys.ACTOR_NAME) ? obj.get(ChangeLogKeys.ACTOR_NAME)
                                                                  .getAsString() : null;
        String actorUuid = obj.has(ChangeLogKeys.ACTOR_UUID) ? obj.get(ChangeLogKeys.ACTOR_UUID)
                                                                  .getAsString() : null;
        String reason = obj.has(ChangeLogKeys.REASON) ? obj.get(ChangeLogKeys.REASON)
                                                           .getAsString() : null;
        List<String> paths = new ArrayList<>();
        if (obj.has(ChangeLogKeys.PATHS)) {
            obj.getAsJsonArray(ChangeLogKeys.PATHS)
               .forEach(element -> paths.add(element.getAsString()));
        }
        return new ChangeTrace(source, new ChangeActor(actorName, actorUuid), reason, paths);
    }

    private static void writeAuditEntry(JsonObject obj, AuditEntry entry, long timestamp) {
        writeTrace(obj, entry.trace(), timestamp);
        if (!entry.changes()
                  .isEmpty()) {
            JsonArray changes = new JsonArray();
            for (AuditChange change : entry.changes()) {
                JsonObject item = new JsonObject();
                item.addProperty(ChangeLogKeys.CHANGE_PATH, change.path());
                item.addProperty(ChangeLogKeys.CHANGE_BEFORE, change.beforeText());
                item.addProperty(ChangeLogKeys.CHANGE_AFTER, change.afterText());
                changes.add(item);
            }
            obj.add(ChangeLogKeys.CHANGES, changes);
        }
    }

    private static AuditEntry readAuditEntry(JsonObject obj, long timestamp) {
        List<AuditChange> changes = new ArrayList<>();
        if (obj.has(ChangeLogKeys.CHANGES)) {
            obj.getAsJsonArray(ChangeLogKeys.CHANGES)
               .forEach(element -> {
                   JsonObject item = element.getAsJsonObject();
                   changes.add(new AuditChange(item.get(ChangeLogKeys.CHANGE_PATH)
                                                   .getAsString(),
                                               item.get(ChangeLogKeys.CHANGE_BEFORE)
                                                   .getAsString(),
                                               item.get(ChangeLogKeys.CHANGE_AFTER)
                                                   .getAsString()));
               });
        }
        return new AuditEntry(timestamp, readTrace(obj), changes);
    }
}
