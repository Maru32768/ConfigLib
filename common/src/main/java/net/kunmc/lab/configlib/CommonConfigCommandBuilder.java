package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.CommonCommand;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.configlib.schema.ConfigSchemaEntry;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Base builder for generated ConfigLib command trees.
 * <p>
 * Platform modules expose concrete subclasses that supply the platform command
 * factory. The fluent configuration API on this builder controls command group
 * visibility, registered configs, the root command name, command descriptions,
 * masked-value reveal rules, config ordering, and final command creation.
 * </p>
 * <p>
 * The generated command tree includes config-level list/reload/reset/history,
 * undo, diff, audit commands and per-field get/modify commands when those
 * command groups are enabled and applicable to the supplied configs.
 * </p>
 */
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

    /**
     * Removes the generated list command group from the root command tree.
     */
    public B disableList() {
        listEnabled = false;
        return self();
    }

    /**
     * Removes the generated reload command group from the root command tree.
     */
    public B disableReload() {
        reloadEnabled = false;
        return self();
    }

    /**
     * Removes the generated reset command group from the root command tree.
     */
    public B disableReset() {
        resetEnabled = false;
        return self();
    }

    /**
     * Removes generated history, undo, diff, and audit command groups.
     */
    public B disableHistory() {
        historyEnabled = false;
        return self();
    }

    /**
     * Removes generated per-field get commands.
     */
    public B disableGet() {
        getEnabled = false;
        return self();
    }

    /**
     * Removes generated per-field modification commands.
     */
    public B disableModify() {
        modifyEnabled = false;
        return self();
    }

    /**
     * Adds another config to the same generated command tree.
     * <p>
     * With multiple configs, commands can be addressed through config-qualified
     * paths such as {@code <configName>.<fieldName>}.
     * </p>
     */
    public B addConfig(@NotNull CommonBaseConfig config) {
        configs.add(config);
        return self();
    }

    /**
     * Sets the generated root command name. The default is {@code config}.
     */
    public B name(@NotNull String name) {
        this.name = Objects.requireNonNull(name);
        return self();
    }

    /**
     * Replaces command descriptions and user-facing command messages.
     */
    public B descriptionProvider(@NotNull ConfigCommandDescriptions.Provider provider) {
        this.descriptionProvider = Objects.requireNonNull(provider);
        return self();
    }

    /**
     * Reveals {@code @Masked} values to console senders, operators, and senders
     * with the supplied permission.
     */
    public B maskedRevealPermission(@NotNull String permission) {
        Objects.requireNonNull(permission, "permission");
        return maskedRevealPolicy((ctx, config, entry) -> {
            net.kunmc.lab.commandlib.CommandActor actor = ctx.getActor();
            return actor.isConsole() || actor.isOperator() || actor.hasPermission(permission);
        });
    }

    /**
     * Replaces the policy used to decide whether masked command output may show
     * the real value.
     */
    public B maskedRevealPolicy(@NotNull MaskedRevealPolicy policy) {
        this.maskedRevealPolicy = Objects.requireNonNull(policy);
        return self();
    }

    /**
     * Sorts configs by {@link CommonBaseConfig#entryName()} before building.
     */
    public B sort() {
        return sort(Comparator.comparing(CommonBaseConfig::entryName));
    }

    /**
     * Sorts configs with a custom comparator before building.
     */
    public B sort(Comparator<? super CommonBaseConfig> sorter) {
        configs.sort(sorter);
        return self();
    }

    /**
     * Builds the command tree. Builders are mutable; create a new builder when a
     * different command tree configuration is needed.
     */
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
