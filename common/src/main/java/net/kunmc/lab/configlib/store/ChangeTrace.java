package net.kunmc.lab.configlib.store;

import net.kunmc.lab.commandlib.CommandActor;
import net.kunmc.lab.commandlib.CommonCommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Metadata that explains an accepted config change.
 * <p>
 * Change traces are part of ConfigLib's public history and audit API. Use the
 * static factories for normal application code; the constructor is kept for
 * integrations that need to restore traces from their own stores.
 * </p>
 */
public final class ChangeTrace {
    private final ChangeSource source;
    private final ChangeActor actor;
    private final String reason;
    private final List<String> paths;

    public ChangeTrace(@NotNull ChangeSource source,
                       @Nullable ChangeActor actor,
                       @Nullable String reason,
                       @Nullable List<String> paths) {
        this.source = Objects.requireNonNull(source, "source");
        this.actor = actor == null ? new ChangeActor(null, null) : actor;
        this.reason = reason;
        this.paths = paths == null ? List.of() : List.copyOf(paths);
    }

    /**
     * Trace used for the first snapshot of a newly created config.
     */
    public static ChangeTrace initial() {
        return new ChangeTrace(ChangeSource.INITIAL, null, null, List.of());
    }

    /**
     * Trace used for an initial snapshot of a migrated config.
     */
    public static ChangeTrace migration() {
        return new ChangeTrace(ChangeSource.MIGRATION, null, null, List.of());
    }

    /**
     * Trace used for accepted application code changes with vararg paths.
     */
    public static ChangeTrace programmatic(String... paths) {
        return new ChangeTrace(ChangeSource.PROGRAMMATIC, null, null, Arrays.asList(paths));
    }

    /**
     * Trace used for accepted application code changes with an explicit path list.
     */
    public static ChangeTrace programmatic(List<String> paths) {
        return new ChangeTrace(ChangeSource.PROGRAMMATIC, null, null, paths);
    }

    /**
     * Trace used for accepted changes loaded from the backing store.
     */
    public static ChangeTrace file(List<String> paths) {
        return new ChangeTrace(ChangeSource.FILE, null, null, paths);
    }

    /**
     * Trace used for undo operations.
     */
    public static ChangeTrace undo(String path) {
        return new ChangeTrace(ChangeSource.UNDO, null, null, List.of(path));
    }

    /**
     * Trace used for generated command changes with vararg paths.
     */
    public static ChangeTrace command(CommonCommandContext<?, ?> ctx, String reason, String... paths) {
        return new ChangeTrace(ChangeSource.COMMAND, actor(ctx), reason, Arrays.asList(paths));
    }

    /**
     * Trace used for generated command changes with an explicit path list.
     */
    public static ChangeTrace command(CommonCommandContext<?, ?> ctx, String reason, List<String> paths) {
        return new ChangeTrace(ChangeSource.COMMAND, actor(ctx), reason, paths);
    }

    public ChangeSource source() {
        return source;
    }

    public ChangeActor actor() {
        return actor;
    }

    @Nullable
    public String reason() {
        return reason;
    }

    public List<String> paths() {
        return paths;
    }

    public boolean hasPaths() {
        return !paths.isEmpty();
    }

    public ChangeTrace withPaths(List<String> paths) {
        return new ChangeTrace(source, actor, reason, paths);
    }

    public ChangeTrace withReason(String reason) {
        return new ChangeTrace(source, actor, reason, paths);
    }

    public ChangeTrace withActor(ChangeActor actor) {
        return new ChangeTrace(source, actor, reason, paths);
    }

    private static ChangeActor actor(CommonCommandContext<?, ?> ctx) {
        CommandActor actor = ctx.getActor();
        String uuid = actor.getUniqueId()
                           .map(UUID::toString)
                           .orElse(null);
        return new ChangeActor(actor.getName(), uuid);
    }
}
