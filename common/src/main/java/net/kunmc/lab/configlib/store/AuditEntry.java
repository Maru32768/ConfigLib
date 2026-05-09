package net.kunmc.lab.configlib.store;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One persisted audit event for an accepted config change.
 * <p>
 * Audit entries record the change trace and, when available, per-field before
 * and after display text. They are returned newest-first by
 * {@link net.kunmc.lab.configlib.CommonBaseConfig#readAudit()}.
 * </p>
 */
public final class AuditEntry {
    private final long timestamp;
    private final ChangeTrace trace;
    private final List<AuditChange> changes;

    public AuditEntry(long timestamp, ChangeTrace trace) {
        this(timestamp, trace, List.of());
    }

    public AuditEntry(long timestamp, ChangeTrace trace, List<AuditChange> changes) {
        this.timestamp = timestamp;
        this.trace = Objects.requireNonNull(trace, "trace");
        this.changes = List.copyOf(Objects.requireNonNull(changes, "changes"));
    }

    public long timestamp() {
        return timestamp;
    }

    public ChangeTrace trace() {
        return trace;
    }

    public List<AuditChange> changes() {
        return changes;
    }

    public Optional<AuditChange> findChange(String path) {
        return changes.stream()
                      .filter(change -> change.path()
                                              .equals(path))
                      .findFirst();
    }
}
