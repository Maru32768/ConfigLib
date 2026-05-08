package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.CommonCommand;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.argument.CommonIntegerArgument;
import net.kunmc.lab.commandlib.util.ChatColorUtil;
import net.kunmc.lab.configlib.schema.ConfigSchemaEntry;
import net.kunmc.lab.configlib.schema.DisplayContext;
import net.kunmc.lab.configlib.store.HistoryEntry;
import net.kunmc.lab.configlib.util.ConfigUtil;
import net.kunmc.lab.configlib.util.ReflectionUtil;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

final class ConfigHistoryCommand {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> T create(CommandFactory<C, T> commandFactory,
                                                                                          @NotNull Set<CommonBaseConfig> configs,
                                                                                          ConfigCommandDescriptions.Provider descriptions,
                                                                                          MaskedRevealPolicy maskedRevealPolicy) {
        T command = commandFactory.create(SubCommandType.History.name);
        command.description(ConfigCommandDescriptions.history(descriptions));

        if (configs.isEmpty()) {
            throw new IllegalArgumentException("configs is empty");
        }

        if (configs.size() > 1) {
            // /config history <index>              - show detail for all configs
            // /config history <name>               - list entries for that config
            // /config history <name> <index>       - show detail for that config
            // /config history <name> diff <index>  - diff current vs history[index]
            // /config history <name> undo          - restore history[1]
            // /config history <name> undo <index>  - restore history[index]
            command.argument(new CommonIntegerArgument<>("index", 0, Integer.MAX_VALUE))
                   .description(ConfigCommandDescriptions.historyIndex(descriptions))
                   .execute((index, ctx) -> configs.forEach(config -> execDetail(ctx,
                                                                                 config,
                                                                                 index,
                                                                                 descriptions,
                                                                                 maskedRevealPolicy)));
            configs.forEach(config -> command.addChildren(createConfigHistoryChild(commandFactory,
                                                                                   config,
                                                                                   descriptions,
                                                                                   maskedRevealPolicy)));
        } else {
            // /config history              - list all entries
            // /config history <index>      - show detail
            // /config history diff <index> - diff current vs history[index]
            // /config history undo         - restore history[1]
            // /config history undo <index> - restore history[index]
            CommonBaseConfig config = configs.iterator()
                                             .next();
            command.execute(ctx -> execList(ctx, config, descriptions, maskedRevealPolicy));
            command.argument(new CommonIntegerArgument<>("index", 0, Integer.MAX_VALUE))
                   .description(ConfigCommandDescriptions.historyIndex(descriptions))
                   .execute((index, ctx) -> execDetail(ctx, config, index, descriptions, maskedRevealPolicy));
            command.addChildren(createDiffChild(commandFactory, config, descriptions, maskedRevealPolicy));
            command.addChildren(createUndoChild(commandFactory,
                                                ConfigCommandDescriptions.undo(descriptions),
                                                config,
                                                descriptions,
                                                maskedRevealPolicy));
        }
        return command;
    }

    private static <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> T createConfigHistoryChild(
            CommandFactory<C, T> commandFactory,
            CommonBaseConfig config,
            ConfigCommandDescriptions.Provider descriptions,
            MaskedRevealPolicy maskedRevealPolicy) {
        T child = commandFactory.create(config.entryName());
        child.description(ConfigCommandDescriptions.historyConfig(descriptions, config.entryName()));
        child.execute(ctx -> execList(ctx, config, descriptions, maskedRevealPolicy));
        child.argument(new CommonIntegerArgument<>("index", 0, Integer.MAX_VALUE))
             .description(ConfigCommandDescriptions.historyIndex(descriptions))
             .execute((index, ctx) -> execDetail(ctx, config, index, descriptions, maskedRevealPolicy));
        child.addChildren(createDiffChild(commandFactory, config, descriptions, maskedRevealPolicy));
        child.addChildren(createUndoChild(commandFactory,
                                          ConfigCommandDescriptions.undoConfig(descriptions, config.entryName()),
                                          config,
                                          descriptions,
                                          maskedRevealPolicy));
        return child;
    }

    private static <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> T createDiffChild(
            CommandFactory<C, T> commandFactory,
            CommonBaseConfig config,
            ConfigCommandDescriptions.Provider descriptions,
            MaskedRevealPolicy maskedRevealPolicy) {
        T child = commandFactory.create("diff");
        child.description(ConfigCommandDescriptions.historyDiff(descriptions));
        child.argument(new CommonIntegerArgument<>("index", 1, Integer.MAX_VALUE))
             .description(ConfigCommandDescriptions.diffIndex(descriptions))
             .execute((index, ctx) -> ConfigDiffCommand.execDiff(ctx,
                                                                 config,
                                                                 0,
                                                                 index,
                                                                 descriptions,
                                                                 maskedRevealPolicy));
        child.argument(new CommonIntegerArgument<>("index1", 0, Integer.MAX_VALUE),
                       new CommonIntegerArgument<>("index2", 0, Integer.MAX_VALUE))
             .description(ConfigCommandDescriptions.diffIndexPair(descriptions))
             .execute((index1, index2, ctx) -> ConfigDiffCommand.execDiff(ctx,
                                                                          config,
                                                                          index1,
                                                                          index2,
                                                                          descriptions,
                                                                          maskedRevealPolicy));
        return child;
    }

    private static <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> T createUndoChild(
            CommandFactory<C, T> commandFactory,
            java.util.function.Function<CommonCommandContext<?, ?>, String> description,
            CommonBaseConfig config,
            ConfigCommandDescriptions.Provider descriptions,
            MaskedRevealPolicy maskedRevealPolicy) {
        T child = commandFactory.create("undo");
        child.description(ctx -> description.apply(ctx));
        child.execute(ctx -> ConfigUndoCommand.exec(ctx, config, 1, descriptions, maskedRevealPolicy));
        child.argument(new CommonIntegerArgument<>("index", 1, Integer.MAX_VALUE))
             .description(ConfigCommandDescriptions.undoIndex(descriptions))
             .execute((index, ctx) -> ConfigUndoCommand.exec(ctx, config, index, descriptions, maskedRevealPolicy));
        return child;
    }

    private static void execList(CommonCommandContext<?, ?> ctx,
                                 CommonBaseConfig config,
                                 ConfigCommandDescriptions.Provider descriptions,
                                 MaskedRevealPolicy maskedRevealPolicy) {
        config.inspect(() -> {
            List<HistoryEntry> history = config.readHistory();
            if (history.isEmpty()) {
                ctx.sendSuccess(descriptions.describe(ctx,
                                                      ConfigCommandDescriptions.Key.HISTORY_EMPTY,
                                                      config.entryName()));
                return;
            }
            ctx.sendMessage(ConfigUtil.configHeader(config));
            for (int i = 0; i < history.size(); i++) {
                HistoryEntry entry = history.get(i);
                boolean isLatest = (i == 0);
                String dateStr = dateText(entry, ctx, descriptions);
                String label = "[" + i + "]: " + dateStr + (isLatest ? descriptions.describe(ctx,
                                                                                             ConfigCommandDescriptions.Key.HISTORY_LATEST_SUFFIX) : "");
                String hoverText = buildFieldsText(ctx, config, entry.config(), maskedRevealPolicy);
                ctx.sendMessageWithOption(label, opt -> {
                    opt.rgb(ChatColorUtil.GREEN.getRGB());
                    if (!hoverText.isEmpty()) {
                        opt.hoverText(hoverText);
                    }
                });
            }
        });
    }

    private static void execDetail(CommonCommandContext<?, ?> ctx,
                                   CommonBaseConfig config,
                                   int index,
                                   ConfigCommandDescriptions.Provider descriptions,
                                   MaskedRevealPolicy maskedRevealPolicy) {
        config.inspect(() -> {
            List<HistoryEntry> history = config.readHistory();
            if (history.isEmpty()) {
                ctx.sendFailure(descriptions.describe(ctx,
                                                      ConfigCommandDescriptions.Key.HISTORY_EMPTY,
                                                      config.entryName()));
                return;
            }
            if (index >= history.size()) {
                ctx.sendFailure(descriptions.describe(ctx,
                                                      ConfigCommandDescriptions.Key.HISTORY_INDEX_OUT_OF_RANGE,
                                                      index,
                                                      history.size() - 1));
                return;
            }
            HistoryEntry entry = history.get(index);
            String dateStr = dateText(entry, ctx, descriptions);
            ctx.sendMessage(ConfigUtil.configHeader(config));
            ctx.sendSuccess("[" + index + "]: " + dateStr);
            listFields(ctx, config, entry.config(), maskedRevealPolicy);
        });
    }

    private static String buildFieldsText(CommonCommandContext<?, ?> ctx,
                                          CommonBaseConfig liveConfig,
                                          CommonBaseConfig histConfig,
                                          MaskedRevealPolicy maskedRevealPolicy) {
        StringBuilder sb = new StringBuilder();
        for (Field field : ReflectionUtil.getFieldsIncludingSuperclasses(liveConfig.getClass())) {
            if (!ConfigUtil.isConfigFieldModifier(field)) {
                continue;
            }
            ConfigSchemaEntry<?> entry = liveConfig.schema()
                                                   .findEntry(field.getName())
                                                   .orElse(null);
            if (entry == null) {
                continue;
            }
            field.setAccessible(true);
            try {
                Object hist = field.get(histConfig);
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(entry.entryName())
                  .append(": ")
                  .append(entry.displayString(hist, DisplayContext.history(ctx, liveConfig, maskedRevealPolicy)));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return sb.toString();
    }

    private static String dateText(HistoryEntry entry,
                                   CommonCommandContext<?, ?> ctx,
                                   ConfigCommandDescriptions.Provider descriptions) {
        if (entry.timestamp() > 0) {
            return DATE_FORMAT.format(new Date(entry.timestamp()));
        }
        return descriptions.describe(ctx, ConfigCommandDescriptions.Key.HISTORY_UNKNOWN_TIMESTAMP);
    }

    private static void listFields(CommonCommandContext<?, ?> ctx,
                                   CommonBaseConfig liveConfig,
                                   CommonBaseConfig histConfig,
                                   MaskedRevealPolicy maskedRevealPolicy) {
        for (Field field : ReflectionUtil.getFieldsIncludingSuperclasses(liveConfig.getClass())) {
            if (!ConfigUtil.isConfigFieldModifier(field)) {
                continue;
            }
            ConfigSchemaEntry<?> entry = liveConfig.schema()
                                                   .findEntry(field.getName())
                                                   .orElse(null);
            if (entry == null) {
                continue;
            }
            field.setAccessible(true);
            try {
                Object hist = field.get(histConfig);
                ctx.sendSuccess(entry.entryName() + ": " + entry.displayString(hist,
                                                                               DisplayContext.history(ctx,
                                                                                                      liveConfig,
                                                                                                      maskedRevealPolicy)));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
