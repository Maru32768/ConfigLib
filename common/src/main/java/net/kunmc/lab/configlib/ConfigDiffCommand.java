package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.CommonCommand;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.argument.CommonIntegerArgument;
import net.kunmc.lab.configlib.schema.ConfigSchemaEntry;
import net.kunmc.lab.configlib.schema.DisplayContext;
import net.kunmc.lab.configlib.store.HistoryEntry;
import net.kunmc.lab.configlib.util.ConfigUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Set;

final class ConfigDiffCommand {
    private static final String DIFF_ARROW = " -> ";

    static <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> T create(CommandFactory<C, T> commandFactory,
                                                                                          @NotNull Set<CommonBaseConfig> configs,
                                                                                          ConfigCommandDescriptions.Provider descriptions,
                                                                                          MaskedRevealPolicy maskedRevealPolicy) {
        T command = commandFactory.create(SubCommandType.Diff.name);
        command.description(ConfigCommandDescriptions.diff(descriptions));

        if (configs.isEmpty()) {
            throw new IllegalArgumentException("configs is empty");
        }

        if (configs.size() > 1) {
            // /config diff <name> default
            // /config diff <name> <index>
            // /config diff <name> <index1> <index2>
            configs.forEach(config -> {
                T child = commandFactory.create(config.entryName());
                child.description(ConfigCommandDescriptions.diffConfig(descriptions, config.entryName()));
                T defaultChild = commandFactory.create("default");
                defaultChild.description(ConfigCommandDescriptions.diffDefault(descriptions));
                defaultChild.execute(ctx -> execDefaultDiff(ctx, config, descriptions, maskedRevealPolicy));
                child.addChildren(defaultChild);
                child.argument(new CommonIntegerArgument<>("index", 1, Integer.MAX_VALUE))
                     .description(ConfigCommandDescriptions.diffIndex(descriptions))
                     .execute((index, ctx) -> execDiff(ctx, config, 0, index, descriptions, maskedRevealPolicy));
                child.argument(new CommonIntegerArgument<>("index1", 0, Integer.MAX_VALUE),
                               new CommonIntegerArgument<>("index2", 0, Integer.MAX_VALUE))
                     .description(ConfigCommandDescriptions.diffIndexPair(descriptions))
                     .execute((index1, index2, ctx) -> execDiff(ctx,
                                                                config,
                                                                index1,
                                                                index2,
                                                                descriptions,
                                                                maskedRevealPolicy));
                command.addChildren(child);
            });
            return command;
        }

        // /config diff default
        // /config diff <index>
        // /config diff <index1> <index2>
        CommonBaseConfig config = configs.iterator()
                                         .next();
        T defaultChild = commandFactory.create("default");
        defaultChild.description(ConfigCommandDescriptions.diffDefault(descriptions));
        defaultChild.execute(ctx -> execDefaultDiff(ctx, config, descriptions, maskedRevealPolicy));
        command.addChildren(defaultChild);
        command.argument(new CommonIntegerArgument<>("index", 1, Integer.MAX_VALUE))
               .description(ConfigCommandDescriptions.diffIndex(descriptions))
               .execute((index, ctx) -> execDiff(ctx, config, 0, index, descriptions, maskedRevealPolicy));
        command.argument(new CommonIntegerArgument<>("index1", 0, Integer.MAX_VALUE),
                         new CommonIntegerArgument<>("index2", 0, Integer.MAX_VALUE))
               .description(ConfigCommandDescriptions.diffIndexPair(descriptions))
               .execute((index1, index2, ctx) -> execDiff(ctx,
                                                          config,
                                                          index1,
                                                          index2,
                                                          descriptions,
                                                          maskedRevealPolicy));
        return command;
    }

    static void execDiff(CommonCommandContext<?, ?> ctx,
                         CommonBaseConfig config,
                         int index1,
                         int index2,
                         ConfigCommandDescriptions.Provider descriptions,
                         MaskedRevealPolicy maskedRevealPolicy) {
        config.inspect(() -> {
            if (index1 == index2) {
                ctx.sendFailure(descriptions.describe(ctx, ConfigCommandDescriptions.Key.DIFF_SAME_INDEX));
                return;
            }

            List<HistoryEntry> history = config.readHistory();
            if (history.size() <= 1) {
                ctx.sendFailure(descriptions.describe(ctx,
                                                      ConfigCommandDescriptions.Key.DIFF_NOT_ENOUGH_HISTORY,
                                                      config.entryName()));
                return;
            }

            int maxIndex = Math.max(index1, index2);
            if (maxIndex >= history.size()) {
                ctx.sendFailure(descriptions.describe(ctx,
                                                      ConfigCommandDescriptions.Key.INDEX_OUT_OF_RANGE,
                                                      maxIndex,
                                                      history.size() - 1));
                return;
            }

            int olderIndex = Math.max(index1, index2);
            int newerIndex = Math.min(index1, index2);
            CommonBaseConfig olderConfig = history.get(olderIndex)
                                                  .config();
            CommonBaseConfig newerConfig = history.get(newerIndex)
                                                  .config();

            ctx.sendMessage(ConfigUtil.configHeader(config));
            ctx.sendSuccess("[" + olderIndex + "]" + DIFF_ARROW + "[" + newerIndex + "]");
            emitDiff(ctx, config, olderConfig, newerConfig, descriptions, maskedRevealPolicy);
        });
    }

    static void execDefaultDiff(CommonCommandContext<?, ?> ctx,
                                CommonBaseConfig config,
                                ConfigCommandDescriptions.Provider descriptions,
                                MaskedRevealPolicy maskedRevealPolicy) {
        config.inspect(() -> {
            ctx.sendMessage(ConfigUtil.configHeader(config));
            ctx.sendSuccess("[" + descriptions.describe(ctx,
                                                        ConfigCommandDescriptions.Key.DIFF_DEFAULT_LABEL) + "]" + DIFF_ARROW + "[" + descriptions.describe(
                    ctx,
                    ConfigCommandDescriptions.Key.DIFF_CURRENT_LABEL) + "]");

            boolean anyDiff = false;
            for (ConfigSchemaEntry<?> entry : config.schema()
                                                    .entries()) {
                Object defaultValue = config.copyDefaultValue(entry);
                Object currentValue = entry.get();
                if (Objects.equals(defaultValue, currentValue)) {
                    continue;
                }
                anyDiff = true;
                String defaultFmt = entry.displayString(defaultValue,
                                                        DisplayContext.diff(ctx, config, maskedRevealPolicy));
                String currentFmt = entry.displayString(currentValue,
                                                        DisplayContext.diff(ctx, config, maskedRevealPolicy));
                ctx.sendSuccess(entry.entryName() + ": " + defaultFmt + DIFF_ARROW + currentFmt);
            }

            if (!anyDiff) {
                ctx.sendSuccess(descriptions.describe(ctx, ConfigCommandDescriptions.Key.DIFF_NONE));
            }
        });
    }

    private static void emitDiff(CommonCommandContext<?, ?> ctx,
                                 CommonBaseConfig liveConfig,
                                 CommonBaseConfig olderConfig,
                                 CommonBaseConfig newerConfig,
                                 ConfigCommandDescriptions.Provider descriptions,
                                 MaskedRevealPolicy maskedRevealPolicy) {
        boolean anyDiff = false;
        for (ConfigSchemaEntry<?> entry : liveConfig.schema()
                                                    .entries()) {
            Object older = entry.get(olderConfig);
            Object newer = entry.get(newerConfig);
            if (Objects.equals(older, newer)) {
                continue;
            }

            anyDiff = true;
            String olderFmt = entry.displayString(older, DisplayContext.diff(ctx, liveConfig, maskedRevealPolicy));
            String newerFmt = entry.displayString(newer, DisplayContext.diff(ctx, liveConfig, maskedRevealPolicy));
            ctx.sendSuccess(entry.entryName() + ": " + olderFmt + DIFF_ARROW + newerFmt);
        }

        if (!anyDiff) {
            ctx.sendSuccess(descriptions.describe(ctx, ConfigCommandDescriptions.Key.DIFF_NONE));
        }
    }
}
