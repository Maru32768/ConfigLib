package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.CommandActor;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.configlib.schema.ConfigSchemaEntry;
import org.jetbrains.annotations.NotNull;

/**
 * Decides whether a command sender may see values annotated with {@code @Masked}.
 */
@FunctionalInterface
public interface MaskedRevealPolicy {
    String DEFAULT_REVEAL_PERMISSION = "configlib.masked.reveal";

    MaskedRevealPolicy DEFAULT = (ctx, config, entry) -> {
        CommandActor actor = ctx.getActor();
        return actor.isConsole() || actor.isOperator() || actor.hasPermission(DEFAULT_REVEAL_PERMISSION);
    };

    boolean canReveal(@NotNull CommonCommandContext<?, ?> ctx,
                      @NotNull CommonBaseConfig config,
                      @NotNull ConfigSchemaEntry<?> entry);
}
