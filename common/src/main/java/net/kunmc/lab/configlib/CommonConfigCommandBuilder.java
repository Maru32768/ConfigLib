package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.CommonCommand;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.configlib.schema.ConfigSchemaEntry;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class CommonConfigCommandBuilder<C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>, B extends CommonConfigCommandBuilder<C, T, B>> {
    private final CommandFactory<C, T> commandFactory;
    private final List<CommonBaseConfig> configs = new ArrayList<>();
    private String name = "config";
    private boolean listEnabled = true;
    private boolean reloadEnabled = true;
    private boolean resetEnabled = true;
    private boolean historyEnabled = true;
    private boolean getEnabled = true;
    private boolean modifyEnabled = true;
    private ConfigCommandDescriptions.Provider descriptionProvider = ConfigCommandDescriptions.defaultProvider();
    private MaskedRevealPolicy maskedRevealPolicy = MaskedRevealPolicy.DEFAULT;

    protected CommonConfigCommandBuilder(@NotNull CommonBaseConfig config,
                                         @NotNull CommandFactory<C, T> commandFactory) {
        this.commandFactory = Objects.requireNonNull(commandFactory);
        configs.add(config);
    }

    @SuppressWarnings("unchecked")
    private B self() {
        return (B) this;
    }

    public B disableList() {
        listEnabled = false;
        return self();
    }

    public B disableReload() {
        reloadEnabled = false;
        return self();
    }

    public B disableReset() {
        resetEnabled = false;
        return self();
    }

    public B disableHistory() {
        historyEnabled = false;
        return self();
    }

    public B disableGet() {
        getEnabled = false;
        return self();
    }

    public B disableModify() {
        modifyEnabled = false;
        return self();
    }

    public B addConfig(@NotNull CommonBaseConfig config) {
        configs.add(config);
        return self();
    }

    public B name(@NotNull String name) {
        this.name = Objects.requireNonNull(name);
        return self();
    }

    public B descriptionProvider(@NotNull ConfigCommandDescriptions.Provider provider) {
        this.descriptionProvider = Objects.requireNonNull(provider);
        return self();
    }

    public B maskedRevealPermission(@NotNull String permission) {
        Objects.requireNonNull(permission, "permission");
        return maskedRevealPolicy((ctx, config, entry) -> {
            net.kunmc.lab.commandlib.CommandActor actor = ctx.getActor();
            return actor.isConsole() || actor.isOperator() || actor.hasPermission(permission);
        });
    }

    public B maskedRevealPolicy(@NotNull MaskedRevealPolicy policy) {
        this.maskedRevealPolicy = Objects.requireNonNull(policy);
        return self();
    }

    public B sort() {
        return sort(Comparator.comparing(CommonBaseConfig::entryName));
    }

    public B sort(Comparator<? super CommonBaseConfig> sorter) {
        configs.sort(sorter);
        return self();
    }

    public T build() {
        T configCommand = commandFactory.create(name);
        configCommand.description(ConfigCommandDescriptions.root(descriptionProvider));

        if (listEnabled) {
            createSubCommand(SubCommandType.List).ifPresent(configCommand::addChildren);
        }
        if (reloadEnabled) {
            createSubCommand(SubCommandType.Reload).ifPresent(configCommand::addChildren);
        }
        if (resetEnabled) {
            createSubCommand(SubCommandType.Reset).ifPresent(configCommand::addChildren);
        }
        if (historyEnabled) {
            createSubCommand(SubCommandType.History).ifPresent(configCommand::addChildren);
            createSubCommand(SubCommandType.Audit).ifPresent(configCommand::addChildren);
            createSubCommand(SubCommandType.Undo).ifPresent(configCommand::addChildren);
            createSubCommand(SubCommandType.Diff).ifPresent(configCommand::addChildren);
        }

        Set<String> conflictingFieldNames = detectConflictingFieldNames();
        configs.forEach(config -> addFieldCommandsFor(configCommand, config, conflictingFieldNames));

        return configCommand;
    }

    private Optional<T> createSubCommand(SubCommandType type) {
        Set<CommonBaseConfig> applicable = configs.stream()
                                                  .filter(type::isEnabledFor)
                                                  .filter(type::hasEntryFor)
                                                  .collect(Collectors.toCollection(LinkedHashSet::new));
        if (applicable.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(type.create(commandFactory, applicable, descriptionProvider, maskedRevealPolicy));
    }

    private Set<String> detectConflictingFieldNames() {
        Map<String, Integer> nameCount = new HashMap<>();
        for (CommonBaseConfig config : configs) {
            for (ConfigSchemaEntry<?> entry : getCommandEntries(config)) {
                nameCount.merge(entry.entryName(), 1, Integer::sum);
            }
        }
        return nameCount.entrySet()
                        .stream()
                        .filter(e -> e.getValue() > 1)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
    }

    private List<ConfigSchemaEntry<?>> getCommandEntries(CommonBaseConfig config) {
        return config.schema()
                     .entries()
                     .stream()
                     .filter(e -> getEnabled || (modifyEnabled && e.supportsModificationCommand()))
                     .collect(Collectors.toList());
    }

    private void addFieldCommandsFor(T configCommand, CommonBaseConfig config, Set<String> conflictingFieldNames) {
        T configNode = commandFactory.create(config.entryName());
        configNode.description(ConfigCommandDescriptions.config(descriptionProvider, config.entryName()));
        configNode.execute(ctx -> ConfigListCommand.listFields(ctx, config, maskedRevealPolicy));
        if (configs.size() > 1) {
            Set<CommonBaseConfig> singleton = Collections.singleton(config);
            if (historyEnabled && config.isHistoryEnabled()) {
                configNode.addChildren(ConfigHistoryCommand.create(commandFactory,
                                                                   singleton,
                                                                   descriptionProvider,
                                                                   maskedRevealPolicy));
                configNode.addChildren(ConfigAuditCommand.create(commandFactory,
                                                                 singleton,
                                                                 descriptionProvider,
                                                                 maskedRevealPolicy));
                configNode.addChildren(ConfigUndoCommand.create(commandFactory,
                                                                singleton,
                                                                descriptionProvider,
                                                                maskedRevealPolicy));
                configNode.addChildren(ConfigDiffCommand.create(commandFactory,
                                                                singleton,
                                                                descriptionProvider,
                                                                maskedRevealPolicy));
            }
        }
        configCommand.addChildren(configNode);

        T dottedConfigNode = commandFactory.create(config.entryName() + ".");
        dottedConfigNode.description(ConfigCommandDescriptions.config(descriptionProvider, config.entryName()));
        dottedConfigNode.execute(ctx -> ConfigListCommand.listFields(ctx, config, maskedRevealPolicy));
        configCommand.addChildren(dottedConfigNode);

        for (ConfigSchemaEntry<?> entry : getCommandEntries(config)) {
            String valueEntryName = entry.entryName();
            String prefixedName = config.entryName() + "." + valueEntryName;

            configCommand.addChildren(ConfigFieldCommand.create(commandFactory,
                                                                config,
                                                                prefixedName,
                                                                entry,
                                                                getEnabled,
                                                                modifyEnabled,
                                                                descriptionProvider,
                                                                maskedRevealPolicy));

            if (!conflictingFieldNames.contains(valueEntryName)) {
                configCommand.addChildren(ConfigFieldCommand.create(commandFactory,
                                                                    config,
                                                                    valueEntryName,
                                                                    entry,
                                                                    getEnabled,
                                                                    modifyEnabled,
                                                                    descriptionProvider,
                                                                    maskedRevealPolicy));
            }
        }
    }
}
