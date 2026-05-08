package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.CommonCommand;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.argument.CommonIntegerArgument;
import net.kunmc.lab.configlib.schema.ConfigSchemaEntry;
import net.kunmc.lab.configlib.schema.DisplayContext;
import net.kunmc.lab.configlib.store.AuditChange;
import net.kunmc.lab.configlib.store.AuditEntry;
import net.kunmc.lab.configlib.util.ConfigUtil;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

final class ConfigAuditCommand {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> T create(CommandFactory<C, T> commandFactory,
                                                                                          @NotNull Set<CommonBaseConfig> configs,
                                                                                          ConfigCommandDescriptions.Provider descriptions,
                                                                                          MaskedRevealPolicy maskedRevealPolicy) {
        T command = commandFactory.create(SubCommandType.Audit.name);
        command.description(ConfigCommandDescriptions.audit(descriptions));

        if (configs.isEmpty()) {
            throw new IllegalArgumentException("configs is empty");
        }

        if (configs.size() == 1) {
            CommonBaseConfig config = configs.iterator()
                                             .next();
            command.execute(ctx -> execList(ctx, config, descriptions));
            command.argument(new CommonIntegerArgument<>("index", 0, Integer.MAX_VALUE))
                   .description(ConfigCommandDescriptions.auditIndex(descriptions))
                   .execute((index, ctx) -> execDetail(ctx, config, index, descriptions, maskedRevealPolicy));
            return command;
        }

        configs.forEach(config -> {
            T child = commandFactory.create(config.entryName());
            child.description(ConfigCommandDescriptions.auditConfig(descriptions, config.entryName()));
            child.execute(ctx -> execList(ctx, config, descriptions));
            child.argument(new CommonIntegerArgument<>("index", 0, Integer.MAX_VALUE))
                 .description(ConfigCommandDescriptions.auditIndex(descriptions))
                 .execute((index, ctx) -> execDetail(ctx, config, index, descriptions, maskedRevealPolicy));
            command.addChildren(child);
        });
        return command;
    }

    private static void execList(CommonCommandContext<?, ?> ctx,
                                 CommonBaseConfig config,
                                 ConfigCommandDescriptions.Provider descriptions) {
        config.inspect(() -> {
            List<AuditEntry> audit = config.readAudit();
            if (audit.isEmpty()) {
                ctx.sendFailure(descriptions.describe(ctx,
                                                      ConfigCommandDescriptions.Key.AUDIT_EMPTY,
                                                      config.entryName()));
                return;
            }

            ctx.sendMessage(ConfigUtil.configHeader(config));
            for (int i = 0; i < audit.size(); i++) {
                AuditEntry entry = audit.get(i);
                ctx.sendSuccess("[" + i + "]: " + summary(ctx, descriptions, entry));
            }
        });
    }

    private static void execDetail(CommonCommandContext<?, ?> ctx,
                                   CommonBaseConfig config,
                                   int index,
                                   ConfigCommandDescriptions.Provider descriptions,
                                   MaskedRevealPolicy maskedRevealPolicy) {
        config.inspect(() -> {
            List<AuditEntry> audit = config.readAudit();
            if (audit.isEmpty()) {
                ctx.sendFailure(descriptions.describe(ctx,
                                                      ConfigCommandDescriptions.Key.AUDIT_EMPTY,
                                                      config.entryName()));
                return;
            }
            if (index >= audit.size()) {
                ctx.sendFailure(descriptions.describe(ctx,
                                                      ConfigCommandDescriptions.Key.INDEX_OUT_OF_RANGE,
                                                      index,
                                                      audit.size() - 1));
                return;
            }

            AuditEntry entry = audit.get(index);
            ctx.sendMessage(ConfigUtil.configHeader(config));
            ctx.sendSuccess("[" + index + "]: " + summary(ctx, descriptions, entry));
            if (entry.trace()
                     .reason() != null) {
                ctx.sendSuccess(descriptions.describe(ctx,
                                                      ConfigCommandDescriptions.Key.AUDIT_REASON_LABEL) + ": " + entry.trace()
                                                                                                                      .reason());
            }
            if (entry.trace()
                     .actor()
                     .isKnown()) {
                ctx.sendSuccess(descriptions.describe(ctx,
                                                      ConfigCommandDescriptions.Key.AUDIT_ACTOR_LABEL) + ": " + actorText(
                        entry));
            }
            if (!entry.trace()
                      .paths()
                      .isEmpty()) {
                ctx.sendSuccess(descriptions.describe(ctx,
                                                      ConfigCommandDescriptions.Key.AUDIT_PATHS_LABEL) + ": " + String.join(
                        ", ",
                        entry.trace()
                             .paths()));
            }
            for (AuditChange change : entry.changes()) {
                ctx.sendSuccess(change.path() + ": " + displayChangeValue(config,
                                                                          ctx,
                                                                          change.path(),
                                                                          change.beforeText(),
                                                                          maskedRevealPolicy) + " -> " + displayChangeValue(
                        config,
                        ctx,
                        change.path(),
                        change.afterText(),
                        maskedRevealPolicy));
            }
        });
    }

    static String summary(CommonCommandContext<?, ?> ctx,
                          ConfigCommandDescriptions.Provider descriptions,
                          AuditEntry entry) {
        return dateText(entry) + " " + entry.trace()
                                            .source() + actorSuffix(ctx,
                                                                    descriptions,
                                                                    entry) + reasonSuffix(entry) + pathsSuffix(entry);
    }

    private static String dateText(AuditEntry entry) {
        return DATE_FORMAT.format(new Date(entry.timestamp()));
    }

    private static String actorSuffix(CommonCommandContext<?, ?> ctx,
                                      ConfigCommandDescriptions.Provider descriptions,
                                      AuditEntry entry) {
        if (!entry.trace()
                  .actor()
                  .isKnown()) {
            return "";
        }
        return descriptions.describe(ctx, ConfigCommandDescriptions.Key.AUDIT_ACTOR_SUFFIX, actorText(entry));
    }

    private static String pathsSuffix(AuditEntry entry) {
        if (entry.trace()
                 .paths()
                 .isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(", ");
        entry.trace()
             .paths()
             .forEach(joiner::add);
        return " [" + joiner + "]";
    }

    private static String reasonSuffix(AuditEntry entry) {
        if (entry.trace()
                 .reason() == null) {
            return "";
        }
        return " (" + entry.trace()
                           .reason() + ")";
    }

    private static String actorText(AuditEntry entry) {
        if (entry.trace()
                 .actor()
                 .name() != null) {
            return entry.trace()
                        .actor()
                        .name();
        }
        return entry.trace()
                    .actor()
                    .uuid();
    }

    static String displayChangeValue(CommonBaseConfig config,
                                     CommonCommandContext<?, ?> ctx,
                                     String path,
                                     String rawText,
                                     MaskedRevealPolicy maskedRevealPolicy) {
        ConfigSchemaEntry<?> entry = config.schema()
                                           .findEntry(path)
                                           .orElse(null);
        if (entry == null) {
            return rawText;
        }
        return DisplayContext.audit(ctx, config, maskedRevealPolicy)
                             .display(rawText, entry);
    }
}
