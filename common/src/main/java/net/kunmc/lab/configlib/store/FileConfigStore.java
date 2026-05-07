package net.kunmc.lab.configlib.store;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kunmc.lab.configlib.CommonBaseConfig;
import net.kunmc.lab.configlib.ConfigKeys;
import net.kunmc.lab.configlib.migration.MigrationExecutionException;
import net.kunmc.lab.configlib.migration.Migrations;
import net.kunmc.lab.configlib.schema.ConfigSchema;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class FileConfigStore implements ConfigStore {
    private final File file;
    private final Gson gson;
    private final ConfigFormat format;
    private final Consumer<Exception> exceptionHandler;
    private final int maxHistorySize;
    private final transient Logger logger = Logger.getLogger(FileConfigStore.class.getName());
    private UnknownKeyPolicy unknownKeyPolicy;
    private JsonObject lastLoadedSnapshot;
    private JsonObject lastWrittenSnapshot;
    private transient Migrations.MigrationResult lastAppliedMigrationResult;

    public FileConfigStore(File file,
                           Gson gson,
                           ConfigFormat format,
                           Consumer<Exception> exceptionHandler,
                           int maxHistorySize,
                           UnknownKeyPolicy unknownKeyPolicy) {
        this.file = file;
        this.gson = gson;
        this.format = format;
        this.exceptionHandler = exceptionHandler;
        this.maxHistorySize = maxHistorySize;
        this.unknownKeyPolicy = Objects.requireNonNull(unknownKeyPolicy, "unknownKeyPolicy");
    }

    public File file() {
        return file;
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
        return file.exists();
    }

    @Override
    public CommonBaseConfig read(Class<? extends CommonBaseConfig> clazz,
                                 Migrations migrations,
                                 CommonBaseConfig defaults) {
        JsonObject jsonObject = format.parseObject(readString(file));
        int storedVersion = jsonObject.has(ConfigKeys.VERSION) ? jsonObject.get(ConfigKeys.VERSION)
                                                                           .getAsInt() : 0;

        Migrations.MigrationResult migrationResult;
        try {
            migrationResult = migrations.execute(storedVersion, gson, jsonObject);
        } catch (MigrationExecutionException e) {
            logger.severe("Config migration failed for " + file.getName() + ": " + e.getMessage());
            throw e;
        }
        lastAppliedMigrationResult = migrationResult.migrated() ? migrationResult : null;
        if (migrationResult.migrated()) {
            jsonObject = migrationResult.document();
            jsonObject.addProperty(ConfigKeys.VERSION, migrations.latestVersion());
            // Migration rewrites raw disk JSON/YAML, not a live config instance, so no schema metadata is available.
            writeStringAtomically(file, format.write(jsonObject, null));
            logMigrationResult(migrationResult);
        }

        ConfigSchema schema = schemaFor(defaults);
        unknownKeyPolicy.apply(jsonObject, schema);

        // The merge base must be the content that actually existed on disk.
        // Defaults are still applied to the returned config, but recording the filled config
        // as the base would make newly added fields look changed on disk from default to absent.
        lastLoadedSnapshot = jsonObject.deepCopy();
        JsonObject defaultObject = JsonConfigDefaults.fromConfig(defaults, gson);
        JsonObject merged = JsonConfigDefaults.fillMissing(jsonObject, defaultObject);
        return gson.fromJson(merged, clazz);
    }

    @Override
    public CommonBaseConfig write(CommonBaseConfig config,
                                  Class<? extends CommonBaseConfig> clazz,
                                  Migrations migrations) {
        file.getParentFile()
            .mkdirs();

        JsonObject memory = gson.toJsonTree(config)
                                .getAsJsonObject();
        JsonObject merged = memory;
        ConfigSchema schema = schemaFor(config);
        if (file.exists()) {
            JsonObject disk = format.parseObject(readString(file));
            int storedVersion = disk.has(ConfigKeys.VERSION) ? disk.get(ConfigKeys.VERSION)
                                                                   .getAsInt() : 0;
            Migrations.MigrationResult migrationResult = migrations.execute(storedVersion, gson, disk);
            if (migrationResult.migrated()) {
                disk = migrationResult.document();
                disk.addProperty(ConfigKeys.VERSION, migrations.latestVersion());
            }
            unknownKeyPolicy.apply(disk, schema);
            merged = mergeWithDiskPriority(lastLoadedSnapshot, memory, disk);
            unknownKeyPolicy.beforeWrite(merged, disk, schema);
        }
        unknownKeyPolicy.apply(merged, schema);
        String content = format.write(merged, schema);
        writeStringAtomically(file, content);
        lastLoadedSnapshot = merged.deepCopy();
        lastWrittenSnapshot = merged.deepCopy();
        return gson.fromJson(merged, clazz);
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

        JsonObject jsonObject = format.parseObject(readString(file));
        int storedVersion = jsonObject.has(ConfigKeys.VERSION) ? jsonObject.get(ConfigKeys.VERSION)
                                                                           .getAsInt() : 0;
        return Optional.of(migrations.execute(storedVersion, gson, jsonObject));
    }

    public void write(CommonBaseConfig config) {
        write(config, config.getClass(), Migrations.empty());
    }

    @Override
    public void pushHistory(CommonBaseConfig config, ChangeTrace trace) {
        File hf = historyFile();
        JsonArray array = hf.exists() ? readHistoryArray(hf) : new JsonArray();
        JsonObject newEntry = gson.toJsonTree(config)
                                  .getAsJsonObject();
        writeTrace(newEntry, trace, System.currentTimeMillis());
        JsonArray reordered = new JsonArray();
        reordered.add(newEntry);
        for (int i = 0; i < array.size() && i < maxHistorySize - 1; i++) {
            reordered.add(array.get(i));
        }
        try {
            hf.getParentFile()
              .mkdirs();
            hf.createNewFile();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // History files contain snapshots under a history wrapper, not the config root schema.
        writeStringAtomically(hf, format.write(wrapHistory(reordered), null));
    }

    public void pushHistory(CommonBaseConfig config) {
        pushHistory(config, new ChangeTrace(ChangeSource.PROGRAMMATIC, null, null, List.of()));
    }

    @Override
    public boolean canRestoreHistoryIndex(int historyIndex) {
        File hf = historyFile();
        if (!hf.exists()) {
            return false;
        }
        JsonArray array = readHistoryArray(hf);
        return historyIndex > 0 && array.size() > historyIndex;
    }

    @Override
    public CommonBaseConfig restoreHistoryIndex(Class<? extends CommonBaseConfig> clazz,
                                                Migrations migrations,
                                                int historyIndex) {
        File hf = historyFile();
        JsonArray array = readHistoryArray(hf);

        JsonObject snapshot = array.get(historyIndex)
                                   .getAsJsonObject();

        JsonArray remaining = new JsonArray();
        for (int i = historyIndex; i < array.size(); i++) {
            remaining.add(array.get(i));
        }
        // History files contain snapshots under a history wrapper, not the config root schema.
        writeStringAtomically(hf, format.write(wrapHistory(remaining), null));

        return gson.fromJson(snapshot, clazz);
    }

    @Override
    public List<HistoryEntry> readHistory(Class<? extends CommonBaseConfig> clazz, Migrations migrations) {
        File hf = historyFile();
        if (!hf.exists()) {
            return new ArrayList<>();
        }
        JsonArray historyArray = readHistoryArray(hf);

        List<HistoryEntry> result = new ArrayList<>();
        for (JsonElement element : historyArray) {
            JsonObject history = element.getAsJsonObject();
            long ts = history.has(ChangeLogKeys.TIMESTAMP) ? history.get(ChangeLogKeys.TIMESTAMP)
                                                                    .getAsLong() : 0L;
            result.add(new HistoryEntry(ts, readTrace(history), gson.fromJson(history, clazz)));
        }

        return result;
    }

    @Override
    public void pushAudit(AuditEntry entry) {
        File auditFile = auditFile();
        JsonArray array = auditFile.exists() ? readHistoryArray(auditFile) : new JsonArray();
        JsonObject newEntry = new JsonObject();
        writeAuditEntry(newEntry, entry, entry.timestamp());
        JsonArray reordered = new JsonArray();
        reordered.add(newEntry);
        for (int i = 0; i < array.size() && i < maxHistorySize - 1; i++) {
            reordered.add(array.get(i));
        }
        try {
            auditFile.getParentFile()
                     .mkdirs();
            auditFile.createNewFile();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        writeStringAtomically(auditFile, format.write(wrapHistory(reordered), null));
    }

    @Override
    public List<AuditEntry> readAudit() {
        File auditFile = auditFile();
        if (!auditFile.exists()) {
            return new ArrayList<>();
        }
        JsonArray array = readHistoryArray(auditFile);
        List<AuditEntry> result = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i)
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

    private File historyFile() {
        String name = file.getName();
        String suffix = "." + format.extension();
        String base = name.endsWith(suffix) ? name.substring(0, name.length() - suffix.length()) : name;
        return new File(file.getParentFile(), base + ".history." + format.extension());
    }

    private File auditFile() {
        String name = file.getName();
        String suffix = "." + format.extension();
        String base = name.endsWith(suffix) ? name.substring(0, name.length() - suffix.length()) : name;
        return new File(file.getParentFile(), base + ".audit." + format.extension());
    }

    private ConfigSchema schemaFor(CommonBaseConfig config) {
        ConfigSchema schema = config.schema();
        return schema == null ? ConfigSchema.fromConfig(config) : schema;
    }

    private JsonArray readHistoryArray(File hf) {
        com.google.gson.JsonElement element = format.parse(readString(hf));
        if (element.isJsonArray()) {
            return element.getAsJsonArray();
        }
        return element.getAsJsonObject()
                      .getAsJsonArray(ChangeLogKeys.HISTORY);
    }

    private com.google.gson.JsonElement wrapHistory(JsonArray history) {
        if (!format.wrapsHistory()) {
            return history;
        }
        JsonObject root = new JsonObject();
        root.add(ChangeLogKeys.HISTORY, history);
        return root;
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

    private static ChangeTrace readTrace(JsonObject history) {
        ChangeSource source = history.has(ChangeLogKeys.SOURCE) ? ChangeSource.valueOf(history.get(ChangeLogKeys.SOURCE)
                                                                                              .getAsString()) : ChangeSource.PROGRAMMATIC;
        String actorName = history.has(ChangeLogKeys.ACTOR_NAME) ? history.get(ChangeLogKeys.ACTOR_NAME)
                                                                          .getAsString() : null;
        String actorUuid = history.has(ChangeLogKeys.ACTOR_UUID) ? history.get(ChangeLogKeys.ACTOR_UUID)
                                                                          .getAsString() : null;
        String reason = history.has(ChangeLogKeys.REASON) ? history.get(ChangeLogKeys.REASON)
                                                                   .getAsString() : null;
        List<String> paths = new ArrayList<>();
        if (history.has(ChangeLogKeys.PATHS)) {
            history.getAsJsonArray(ChangeLogKeys.PATHS)
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

    @Override
    public Closeable startWatching(Runnable onChanged) {
        try {
            file.getParentFile()
                .mkdirs();
            WatchService watchService = FileSystems.getDefault()
                                                   .newWatchService();
            WatchKey watchKey = file.getParentFile()
                                    .toPath()
                                    .register(watchService, ENTRY_MODIFY, ENTRY_CREATE);

            Thread watcherThread = new Thread(() -> {
                try {
                    while (!Thread.currentThread()
                                  .isInterrupted()) {
                        WatchKey key = watchService.take();
                        for (WatchEvent<?> e : key.pollEvents()) {
                            Path changed = file.getParentFile()
                                               .toPath()
                                               .resolve((Path) e.context());
                            if (changed.equals(file.toPath())) {
                                try {
                                    if (isLastWrittenSnapshotOnDisk()) {
                                        continue;
                                    }
                                    onChanged.run();
                                } catch (Exception ex) {
                                    exceptionHandler.accept(ex);
                                }
                            }
                        }
                        if (!key.reset()) {
                            break;
                        }
                    }
                } catch (ClosedWatchServiceException ignored) {
                    // Closed by the watcher Closeable.
                } catch (InterruptedException e) {
                    Thread.currentThread()
                          .interrupt();
                }
            }, "ConfigLib file watcher - " + file.getName());
            watcherThread.setDaemon(true);
            watcherThread.start();

            return () -> {
                watchKey.cancel();
                try {
                    watchService.close();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                watcherThread.interrupt();
            };
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String readString(File file) {
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private JsonObject mergeWithDiskPriority(JsonObject base, JsonObject memory, JsonObject disk) {
        if (base == null) {
            return JsonConfigDefaults.fillMissing(disk, memory);
        }

        JsonObject merged = new JsonObject();
        Set<String> keys = new HashSet<>();
        keys.addAll(base.keySet());
        keys.addAll(memory.keySet());
        keys.addAll(disk.keySet());

        for (String key : keys) {
            JsonElementState baseValue = JsonElementState.of(base, key);
            JsonElementState memoryValue = JsonElementState.of(memory, key);
            JsonElementState diskValue = JsonElementState.of(disk, key);

            JsonElementState selected;
            // Three-way merge:
            // - base is the last content read from disk
            // - memory is the current in-process config
            // - disk is the latest file content
            // Missing keys are tracked separately from JSON null, so a field added in code
            // after the file was created is treated as a memory-only addition, not a conflict.
            if (baseValue.equals(memoryValue)) {
                selected = diskValue;
            } else if (baseValue.equals(diskValue)) {
                selected = memoryValue;
            } else if (memoryValue.equals(diskValue)) {
                selected = memoryValue;
            } else {
                logger.warning("Config field \"" + key + "\" was changed both in memory and on disk. Disk value wins. " + "discarded memory value: " + truncate(
                        gson.toJson(memoryValue.element)) + ", disk value: " + truncate(gson.toJson(diskValue.element)));
                selected = diskValue;
            }

            if (selected.present) {
                merged.add(key, selected.element.deepCopy());
            }
        }
        return merged;
    }

    private boolean isLastWrittenSnapshotOnDisk() {
        if (lastWrittenSnapshot == null || !file.exists()) {
            return false;
        }
        try {
            JsonObject disk = format.parseObject(readString(file));
            return disk.equals(lastWrittenSnapshot);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String truncate(String value) {
        if (value == null) {
            return "null";
        }
        return value.length() <= 300 ? value : value.substring(0, 300) + "...";
    }

    private static void writeStringAtomically(File file, String content) {
        try {
            Path path = file.toPath()
                            .toAbsolutePath();
            Path parent = path.getParent();
            Files.createDirectories(parent);
            Path tmp = Files.createTempFile(parent,
                                            path.getFileName()
                                                .toString(),
                                            ".tmp");
            try {
                Files.writeString(tmp, content, StandardCharsets.UTF_8);
                try {
                    Files.move(tmp, path, ATOMIC_MOVE, REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmp, path, REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(tmp);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void logMigrationResult(Migrations.MigrationResult migrationResult) {
        logger.info("Applied config migrations for " + file.getName() + " from v" + migrationResult.storedVersion() + " to v" + migrationResult.targetVersion());
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

    private static final class JsonElementState {
        private final boolean present;
        private final com.google.gson.JsonElement element;

        private JsonElementState(boolean present, com.google.gson.JsonElement element) {
            this.present = present;
            this.element = element;
        }

        private static JsonElementState of(JsonObject object, String key) {
            return new JsonElementState(object.has(key), object.get(key));
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof JsonElementState)) {
                return false;
            }
            JsonElementState other = (JsonElementState) obj;
            if (present != other.present) {
                return false;
            }
            if (!present) {
                return true;
            }
            return element.equals(other.element);
        }

        @Override
        public int hashCode() {
            return present ? element.hashCode() : 0;
        }
    }

}
