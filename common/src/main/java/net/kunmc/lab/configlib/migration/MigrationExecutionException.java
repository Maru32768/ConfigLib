package net.kunmc.lab.configlib.migration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Failure raised while executing one migration operation.
 * <p>
 * The exception includes the migration version, failed operation, completed
 * reports before the failure, and the failed operation report. ConfigLib does
 * not persist a partially migrated document when this exception is thrown.
 * </p>
 */
public final class MigrationExecutionException extends RuntimeException {
    private final int version;
    private final MigrationOperationType operationType;
    private final String path;
    private final String targetPath;
    private final List<Migrations.VersionReport> completedVersionReports;
    private final Migrations.OperationReport failedOperationReport;

    public MigrationExecutionException(int version,
                                       MigrationOperationType operationType,
                                       String path,
                                       String targetPath,
                                       List<Migrations.VersionReport> completedVersionReports,
                                       Migrations.OperationReport failedOperationReport,
                                       Throwable cause) {
        super(buildMessage(version, operationType, path, targetPath, cause), cause);
        this.version = version;
        this.operationType = operationType;
        this.path = path;
        this.targetPath = targetPath;
        this.completedVersionReports = List.copyOf(completedVersionReports);
        this.failedOperationReport = failedOperationReport;
    }

    private static String buildMessage(int version,
                                       MigrationOperationType operationType,
                                       String path,
                                       String targetPath,
                                       Throwable cause) {
        String operationName = operationType.displayName();
        String operation = targetPath == null ? operationName + " " + path : operationName + " " + path + " -> " + targetPath;
        String causeMessage = cause == null ? "" : cause.getMessage();
        if (causeMessage == null || causeMessage.isEmpty()) {
            return "Migration v" + version + " failed while applying " + operation;
        }
        return "Migration v" + version + " failed while applying " + operation + ": " + causeMessage;
    }

    public int version() {
        return version;
    }

    public MigrationOperationType operationType() {
        return operationType;
    }

    public String path() {
        return path;
    }

    public String targetPath() {
        return targetPath;
    }

    public List<Migrations.VersionReport> completedVersionReports() {
        return completedVersionReports;
    }

    public List<Migrations.OperationReport> completedOperationReports() {
        List<Migrations.OperationReport> reports = new ArrayList<>();
        for (Migrations.VersionReport versionReport : completedVersionReports) {
            reports.addAll(versionReport.operations());
        }
        return Collections.unmodifiableList(reports);
    }

    public Migrations.OperationReport failedOperationReport() {
        return failedOperationReport;
    }
}
