package net.kunmc.lab.configlib.util;

import org.bukkit.Bukkit;

import java.util.UUID;

public final class UUIDUtil {
    private UUIDUtil() {
    }

    public static String getNameOrUuid(UUID uuid) {
        String s = Bukkit.getOfflinePlayer(uuid)
                         .getName();
        if (s == null) {
            s = uuid.toString();
        }
        return s;
    }
}
