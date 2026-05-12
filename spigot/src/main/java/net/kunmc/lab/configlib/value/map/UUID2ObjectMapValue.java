package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.argument.UUIDArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;
import net.kunmc.lab.configlib.MapValue;
import net.kunmc.lab.configlib.util.UUIDUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.stream.Collectors;

public abstract class UUID2ObjectMapValue<V, T extends UUID2ObjectMapValue<V, T>> extends MapValue<UUID, V, T> {
    public UUID2ObjectMapValue() {
        this(new HashMap<>());
    }

    public UUID2ObjectMapValue(Map<UUID, V> value) {
        super(value);
    }

    protected ArgumentDefinition<UUID> keyArgumentDefinitionForPut() {
        return new ArgumentDefinition<>(new UUIDArgument("key"), (uuid, ctx) -> uuid);
    }

    @Override
    protected List<ArgumentDefinition<UUID>> argumentDefinitionsForRemove() {
        return List.of(new ArgumentDefinition<>(new UUIDArgument("key").validator(x -> {
                                                                           return value.containsKey(x);
                                                                       })
                                                                       .additionalSuggestionAction(sb -> {
                                                                           List<UUID> offlinePlayerUUIDs = Arrays.stream(
                                                                                                                         Bukkit.getOfflinePlayers())
                                                                                                                 .map(OfflinePlayer::getUniqueId)
                                                                                                                 .collect(
                                                                                                                         Collectors.toList());
                                                                           value.keySet()
                                                                                .stream()
                                                                                .filter(x -> !offlinePlayerUUIDs.contains(
                                                                                        x))
                                                                                .map(Object::toString)
                                                                                .forEach(sb::suggest);
                                                                       }), (uuid, ctx) -> uuid));
    }

    @Override
    protected String keyToString(UUID uuid) {
        return UUIDUtil.getNameOrUuid(uuid);
    }
}
