package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.CommonCommand;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.argument.CommonIntegerArgument;
import net.kunmc.lab.configlib.exception.ConfigValidationException;
import net.kunmc.lab.configlib.store.ChangeTrace;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

final class ConfigUndoCommand {
    static <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> T create(CommandFactory<C, T> commandFactory,
                                                                                          @NotNull Set<CommonBaseConfig> configs,
                                                                                          ConfigCommandDescriptions.Provider descriptions,
                                                                                          MaskedRevealPolicy maskedRevealPolicy) {
        T command = commandFactory.create(SubCommandType.Undo.name);
        command.description(ConfigCommandDescriptions.undo(descriptions));

        if (configs.isEmpty()) {
            throw new IllegalArgumentException("configs is empty");
        }

        // /config undo - restore history[1]
        command.execute(ctx -> configs.forEach(config -> exec(ctx, config, 1, descriptions, maskedRevealPolicy)));

        // /config undo <index> - restore history[index]
        command.argument(new CommonIntegerArgument<>("index", 1, Integer.MAX_VALUE))
               .description(ConfigCommandDescriptions.undoIndex(descriptions))
               .execute((index, ctx) -> {
                   configs.forEach(config -> exec(ctx, config, index, descriptions, maskedRevealPolicy));
               });

        if (configs.size() > 1) {
            configs.forEach(config -> {
                T child = commandFactory.create(config.entryName());
                child.description(ConfigCommandDescriptions.undoConfig(descriptions, config.entryName()));
                child.execute(ctx -> exec(ctx, config, 1, descriptions, maskedRevealPolicy));
                child.argument(new CommonIntegerArgument<>("index", 1, Integer.MAX_VALUE))
                     .description(ConfigCommandDescriptions.undoIndex(descriptions))
                     .execute((index, ctx) -> exec(ctx, config, index, descriptions, maskedRevealPolicy));
                command.addChildren(child);
            });
        }
        return command;
    }

    static void exec(CommonCommandContext<?, ?> ctx,
                     CommonBaseConfig config,
                     int index,
                     ConfigCommandDescriptions.Provider descriptions,
                     MaskedRevealPolicy maskedRevealPolicy) {
        if (config.inspect(() -> config.readHistory()
                                       .size()) <= 1) {
            ctx.sendFailure(descriptions.describe(ctx,
                                                  ConfigCommandDescriptions.Key.UNDO_NO_RESTORABLE_HISTORY,
                                                  config.entryName()));
            return;
        }

        boolean applied;
        try {
            applied = config.applyUndo(index,
                                       ChangeTrace.command(ctx,
                                                           "undo " + config.entryName(),
                                                           "history[" + index + "]"));
        } catch (ConfigValidationException e) {
            e.sendMessage(ctx, descriptions);
            return;
        }

        if (applied) {
            ctx.sendSuccess(descriptions.describe(ctx,
                                                  ConfigCommandDescriptions.Key.UNDO_SUCCESS,
                                                  config.entryName(),
                                                  index));
            ConfigListCommand.listFields(ctx, config, maskedRevealPolicy);
        } else {
            ctx.sendFailure(descriptions.describe(ctx,
                                                  ConfigCommandDescriptions.Key.UNDO_NOT_RESTORABLE,
                                                  index,
                                                  config.entryName()));
        }
    }
}
