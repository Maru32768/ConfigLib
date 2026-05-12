package net.kunmc.lab.configlib.value;

import net.kunmc.lab.commandlib.argument.UUIDArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;
import net.kunmc.lab.configlib.SingleValue;
import net.kunmc.lab.configlib.util.UUIDUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class UUIDValue extends SingleValue<UUID, UUIDValue> {
    public UUIDValue() {
        this(((UUID) null));
    }

    public UUIDValue(Player player) {
        this(player.getUniqueId());
    }

    public UUIDValue(UUID value) {
        super(value);
    }

    @Nullable
    public OfflinePlayer toOfflinePlayer() {
        if (value == null) {
            return null;
        }
        return Bukkit.getOfflinePlayer(value);
    }

    @Nullable
    public Player toPlayer() {
        if (value == null) {
            return null;
        }
        return Bukkit.getPlayer(value);
    }

    @Nullable
    public String playerName() {
        if (value == null) {
            return "";
        }
        return valueToString(value);
    }

    @Override
    protected List<ArgumentDefinition<UUID>> argumentDefinitions() {
        return List.of(new ArgumentDefinition<>(new UUIDArgument("target"), (target, ctx) -> target));
    }

    @Override
    protected String valueToString(UUID uuid) {
        return UUIDUtil.getNameOrUuid(uuid);
    }

}
