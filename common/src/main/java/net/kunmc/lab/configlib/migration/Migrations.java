package net.kunmc.lab.configlib.migration;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Immutable set of versioned config migrations.
 * <p>
 * Application code normally registers migrations through
 * {@link net.kunmc.lab.configlib.CommonBaseConfig.Option#migrateTo(int, Consumer)}.
 * Custom stores and tests may use this type directly to preview or execute
 * migration plans against raw JSON documents.
 * </p>
 */
public final class Migrations {
    private final NavigableMap<Integer, MigrationPlan> migrations;

    private Migrations(NavigableMap<Integer, MigrationPlan> migrations) {
        this.migrations = Collections.unmodifiableNavigableMap(new TreeMap<>(migrations));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Migrations empty() {
        return builder().build();
    }

    public int latestVersion() {
        return migrations.isEmpty() ? 0 : migrations.lastKey();
    }

    /**
     * Executes all pending migrations against a copy of the supplied document and returns the result.
     * <p>
     * This method is pure with respect to {@code document}: the input object is never mutated.
     * Callers decide whether to persist {@link MigrationResult#document()} or to inspect it only.
     * </p>
     *
     * @param storedVersion schema version currently stored in the document
     * @param gson          serializer used by conversion operations
     * @param document      source document to migrate
     * @return migration result including the migrated document copy and execution reports
     */
    public MigrationResult execute(int storedVersion, Gson gson, JsonObject document) {
        Objects.requireNonNull(gson, "gson");
        Objects.requireNonNull(document, "document");

        if (migrations.isEmpty()) {
            return new MigrationResult(document, storedVersion, latestVersion(), false, List.of());
        }

        int latestVersion = latestVersion();
        if (storedVersion >= latestVersion) {
            return new MigrationResult(document, storedVersion, latestVersion, false, List.of());
        }

        JsonObject current = document.deepCopy();
        boolean migrated = false;
        List<VersionReport> reports = new ArrayList<>();
        for (var entry : migrations.entrySet()) {
            if (entry.getKey() <= storedVersion) {
                continue;
            }

            JsonObject candidate = current.deepCopy();
            JsonMigrationContext context = new JsonMigrationContext(gson, candidate);
            VersionReport report = entry.getValue()
                                        .apply(entry.getKey(), context, reports);
            current = candidate;
            reports.add(report);
            if (report.applied()) {
                migrated = true;
            }
        }

        return new MigrationResult(current, storedVersion, latestVersion, migrated, reports);
    }

    public static final class Builder {
        private final TreeMap<Integer, MigrationPlan> migrations = new TreeMap<>();

        /**
         * Registers one migration step for a specific target version.
         */
        public Builder migrateTo(int version, Consumer<MigrationDsl> migration) {
            if (version <= 0) {
                throw new IllegalArgumentException("version must be positive");
            }
            if (migrations.containsKey(version)) {
                throw new IllegalArgumentException("Migration version already registered: " + version);
            }

            MigrationDslImpl dsl = new MigrationDslImpl();
            migration.accept(dsl);
            migrations.put(version, dsl.build());
            return this;
        }

        public Migrations build() {
            return new Migrations(migrations);
        }
    }

    public static final class MigrationResult {
        private final JsonObject document;
        private final int storedVersion;
        private final int targetVersion;
        private final boolean migrated;
        private final List<VersionReport> versionReports;

        private MigrationResult(JsonObject document,
                                int storedVersion,
                                int targetVersion,
                                boolean migrated,
                                List<VersionReport> versionReports) {
            this.document = document;
            this.storedVersion = storedVersion;
            this.targetVersion = targetVersion;
            this.migrated = migrated;
            this.versionReports = List.copyOf(versionReports);
        }

        /**
         * Returns the migrated document copy. The original input document passed to
         * {@link Migrations#execute(int, Gson, JsonObject)} is never mutated.
         */
        public JsonObject document() {
            return document;
        }

        /**
         * Returns {@code true} when at least one operation reported an applied change.
         */
        public boolean migrated() {
            return migrated;
        }

        public int storedVersion() {
            return storedVersion;
        }

        public int targetVersion() {
            return targetVersion;
        }

        public List<VersionReport> versionReports() {
            return versionReports;
        }

        /**
         * Flattens all version reports into one operation list in execution order.
         */
        public List<OperationReport> reports() {
            List<OperationReport> reports = new ArrayList<>();
            for (VersionReport versionReport : versionReports) {
                reports.addAll(versionReport.operations());
            }
            return Collections.unmodifiableList(reports);
        }
    }

    private static final class MigrationPlan {
        private final List<MigrationOperation> operations;

        private MigrationPlan(List<MigrationOperation> operations) {
            this.operations = List.copyOf(operations);
        }

        private VersionReport apply(int version, JsonMigrationContext context, List<VersionReport> completedReports) {
            List<OperationReport> reports = new ArrayList<>();
            for (MigrationOperation operation : operations) {
                reports.add(operation.apply(version, context, completedReports));
            }
            return new VersionReport(version, reports);
        }
    }

    private interface MigrationOperation {
        OperationReport apply(int version, JsonMigrationContext context, List<VersionReport> completedReports);
    }

    private static final class MigrationDslImpl implements MigrationDsl {
        private final List<MigrationOperation> operations = new ArrayList<>();

        private MigrationPlan build() {
            return new MigrationPlan(operations);
        }

        @Override
        public MigrationDsl rename(String from, String to) {
            operations.add((version, context, completedReports) -> applyOperation(version,
                                                                                  MigrationOperationType.RENAME,
                                                                                  from,
                                                                                  to,
                                                                                  completedReports,
                                                                                  () -> OperationReport.move(
                                                                                          MigrationOperationType.RENAME,
                                                                                          from,
                                                                                          to,
                                                                                          context.rename(from, to))));
            return this;
        }

        @Override
        public MigrationDsl move(String from, String to) {
            operations.add((version, context, completedReports) -> applyOperation(version,
                                                                                  MigrationOperationType.MOVE,
                                                                                  from,
                                                                                  to,
                                                                                  completedReports,
                                                                                  () -> OperationReport.move(
                                                                                          MigrationOperationType.MOVE,
                                                                                          from,
                                                                                          to,
                                                                                          context.move(from, to))));
            return this;
        }

        @Override
        public MigrationDsl delete(String path) {
            operations.add((version, context, completedReports) -> applyOperation(version,
                                                                                  MigrationOperationType.DELETE,
                                                                                  path,
                                                                                  null,
                                                                                  completedReports,
                                                                                  () -> OperationReport.single(
                                                                                          MigrationOperationType.DELETE,
                                                                                          path,
                                                                                          context.delete(path))));
            return this;
        }

        @Override
        public MigrationDsl set(String path, Object value) {
            operations.add((version, context, completedReports) -> applyOperation(version,
                                                                                  MigrationOperationType.SET,
                                                                                  path,
                                                                                  null,
                                                                                  completedReports,
                                                                                  () -> OperationReport.single(
                                                                                          MigrationOperationType.SET,
                                                                                          path,
                                                                                          context.set(path, value))));
            return this;
        }

        @Override
        public MigrationDsl defaultValue(String path, Object value) {
            operations.add((version, context, completedReports) -> applyOperation(version,
                                                                                  MigrationOperationType.DEFAULT_VALUE,
                                                                                  path,
                                                                                  null,
                                                                                  completedReports,
                                                                                  () -> OperationReport.single(
                                                                                          MigrationOperationType.DEFAULT_VALUE,
                                                                                          path,
                                                                                          context.defaultValue(path,
                                                                                                               value))));
            return this;
        }

        @Override
        public <S, T> MigrationDsl convert(String path,
                                           Class<S> sourceType,
                                           Class<T> targetType,
                                           Function<? super S, ? extends T> converter) {
            operations.add((version, context, completedReports) -> applyOperation(version,
                                                                                  MigrationOperationType.CONVERT,
                                                                                  path,
                                                                                  null,
                                                                                  completedReports,
                                                                                  () -> OperationReport.single(
                                                                                          MigrationOperationType.CONVERT,
                                                                                          path,
                                                                                          context.convert(path,
                                                                                                          sourceType,
                                                                                                          targetType,
                                                                                                          converter))));
            return this;
        }

        private OperationReport applyOperation(int version,
                                               MigrationOperationType operationType,
                                               String path,
                                               String targetPath,
                                               List<VersionReport> completedReports,
                                               Supplier<OperationReport> supplier) {
            try {
                return supplier.get();
            } catch (MigrationExecutionException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new MigrationExecutionException(version,
                                                      operationType,
                                                      path,
                                                      targetPath,
                                                      completedReports,
                                                      OperationReport.failure(operationType, path, targetPath),
                                                      e);
            }
        }
    }

    public static final class VersionReport {
        private final int version;
        private final List<OperationReport> operations;

        private VersionReport(int version, List<OperationReport> operations) {
            this.version = version;
            this.operations = List.copyOf(operations);
        }

        public int version() {
            return version;
        }

        public List<OperationReport> operations() {
            return operations;
        }

        /**
         * Returns {@code true} when this version step changed the document.
         */
        public boolean applied() {
            return operations.stream()
                             .anyMatch(OperationReport::applied);
        }
    }

    public static final class OperationReport {
        private final MigrationOperationType type;
        private final String path;
        private final String targetPath;
        private final boolean applied;

        private OperationReport(MigrationOperationType type, String path, String targetPath, boolean applied) {
            this.type = type;
            this.path = path;
            this.targetPath = targetPath;
            this.applied = applied;
        }

        static OperationReport single(MigrationOperationType type, String path, boolean applied) {
            return new OperationReport(type, path, null, applied);
        }

        static OperationReport move(MigrationOperationType type, String from, String to, boolean applied) {
            return new OperationReport(type, from, to, applied);
        }

        static OperationReport failure(MigrationOperationType type, String path, String targetPath) {
            return new OperationReport(type, path, targetPath, false);
        }

        public MigrationOperationType type() {
            return type;
        }

        public String path() {
            return path;
        }

        public String targetPath() {
            return targetPath;
        }

        public boolean applied() {
            return applied;
        }
    }
}
