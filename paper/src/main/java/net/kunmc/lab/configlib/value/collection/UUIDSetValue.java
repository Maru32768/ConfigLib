package net.kunmc.lab.configlib.value.collection;

import net.kunmc.lab.commandlib.argument.UUIDArgument;
import net.kunmc.lab.commandlib.argument.UUIDsArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;
import net.kunmc.lab.configlib.util.UUIDUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class UUIDSetValue extends SetValue<UUID, UUIDSetValue> {
    public UUIDSetValue(UUID... uuids) {
        this(Set.of(uuids));
    }

    public UUIDSetValue(Collection<Player> players) {
        this(players.stream()
                    .map(Player::getUniqueId)
                    .collect(Collectors.toSet()));
    }

    public UUIDSetValue(Set<UUID> value) {
        super(value);
    }

    @Override
    protected List<ArgumentDefinition<Set<UUID>>> argumentDefinitionsForAdd() {
        return List.of(new ArgumentDefinition<>(new UUIDsArgument("targets").validator(x -> {
            return x.size() > 1 || !value.contains(x.get(0));
        }), (targets, ctx) -> new HashSet<>(targets)));
    }

    @Override
    protected List<ArgumentDefinition<Set<UUID>>> argumentDefinitionsForRemove() {
        return List.of(new ArgumentDefinition<>(new UUIDArgument("target").validator(x -> {
                                                                              return value.contains(x);
                                                                          })
                                                                          .additionalSuggestionAction(sb -> {
                                                                              List<UUID> offlinePlayerUUIDs = Arrays.stream(
                                                                                                                            Bukkit.getOfflinePlayers())
                                                                                                                    .map(OfflinePlayer::getUniqueId)
                                                                                                                    .collect(
                                                                                                                            Collectors.toList());
                                                                              value.stream()
                                                                                   .filter(offlinePlayerUUIDs::contains)
                                                                                   .forEach(x -> sb.suggest(x.toString()));
                                                                          }), (target, ctx) -> Set.of(target)));
    }

    @Override
    protected String elementToString(UUID uuid) {
        return UUIDUtil.getNameOrUuid(uuid);
    }
}
