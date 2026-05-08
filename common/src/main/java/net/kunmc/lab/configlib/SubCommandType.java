package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.CommonCommand;
import net.kunmc.lab.commandlib.CommonCommandContext;

import java.util.Set;
import java.util.function.Predicate;

enum SubCommandType {
    Reload("reload",
           CommonBaseConfig::isReloadEnabled,
           x -> !x.schema()
                  .entries()
                  .isEmpty(),
           ConfigReloadCommand::create),
    Reset("reset",
          CommonBaseConfig::isResetEnabled,
          x -> !x.schema()
                 .entries()
                 .isEmpty(),
          ConfigResetCommand::create),
    History("history", CommonBaseConfig::isHistoryEnabled, x -> true, ConfigHistoryCommand::create),
    Audit("audit", CommonBaseConfig::isHistoryEnabled, x -> true, ConfigAuditCommand::create),
    Undo("undo", CommonBaseConfig::isHistoryEnabled, x -> true, ConfigUndoCommand::create),
    Diff("diff", CommonBaseConfig::isHistoryEnabled, x -> true, ConfigDiffCommand::create),
    List("list",
         CommonBaseConfig::isListEnabled,
         x -> !x.schema()
                .entries()
                .isEmpty(),
         ConfigListCommand::create);

    public final String name;
    private final Predicate<CommonBaseConfig> isEnabledFor;
    private final Predicate<CommonBaseConfig> hasEntryFor;
    private final Instantiator instantiator;

    SubCommandType(String name,
                   Predicate<CommonBaseConfig> isEnabledFor,
                   Predicate<CommonBaseConfig> hasEntryFor,
                   Instantiator instantiator) {
        this.name = name;
        this.isEnabledFor = isEnabledFor;
        this.hasEntryFor = hasEntryFor;
        this.instantiator = instantiator;
    }

    public boolean hasEntryFor(CommonBaseConfig config) {
        return hasEntryFor.test(config);
    }

    public boolean isEnabledFor(CommonBaseConfig config) {
        return isEnabledFor.test(config);
    }

    public <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> T create(CommandFactory<C, T> commandFactory,
                                                                                          Set<CommonBaseConfig> configs,
                                                                                          ConfigCommandDescriptions.Provider descriptions,
                                                                                          MaskedRevealPolicy maskedRevealPolicy) {
        return instantiator.apply(commandFactory, configs, descriptions, maskedRevealPolicy);
    }

    @FunctionalInterface
    private interface Instantiator {
        <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> T apply(CommandFactory<C, T> commandFactory,
                                                                                      Set<CommonBaseConfig> configs,
                                                                                      ConfigCommandDescriptions.Provider descriptions,
                                                                                      MaskedRevealPolicy maskedRevealPolicy);
    }
}
