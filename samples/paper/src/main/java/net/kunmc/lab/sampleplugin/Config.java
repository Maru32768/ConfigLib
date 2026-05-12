package net.kunmc.lab.sampleplugin;

import net.kunmc.lab.configlib.BaseConfig;
import net.kunmc.lab.configlib.annotation.ConfigNullable;
import net.kunmc.lab.configlib.annotation.Description;
import net.kunmc.lab.configlib.annotation.Masked;
import net.kunmc.lab.configlib.annotation.Range;
import net.kunmc.lab.configlib.value.IntegerValue;
import net.kunmc.lab.configlib.value.LocationValue;
import net.kunmc.lab.configlib.value.TeamValue;
import net.kunmc.lab.configlib.value.collection.BlockDataSetValue;
import net.kunmc.lab.configlib.value.map.Enum2DoubleMapValue;
import net.kunmc.lab.configlib.value.tuple.Integer2IntegerPairValue;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class Config extends BaseConfig {
    public final IntegerValue maxPlayers = new IntegerValue(20, 1, 100).description("Maximum players per arena.");
    public final LocationValue spawnLocation = new LocationValue().description("Default spawn location.");
    public final BlockDataSetValue protectedBlocks = new BlockDataSetValue().description(
            "Blocks protected by this plugin.");
    public final Enum2DoubleMapValue<Material> enum2DoubleMapValue = new Enum2DoubleMapValue<>(Material.class);
    public final TeamValue teamValue = new TeamValue().onModify(team -> {
        Bukkit.getOnlinePlayers()
              .forEach(p -> {
                  p.sendMessage("teamValue modified!");
              });
    });
    public final Integer2IntegerPairValue integer2IntegerPairValue = new Integer2IntegerPairValue(1, 100).leftMin(1)
                                                                                                         .leftMax(99)
                                                                                                         .rightMin(2)
                                                                                                         .rightMax(100);

    @Description("Simple POJO field edited with scalar set commands.")
    @Range(min = 1, max = 50)
    public int maxArenas = 5;

    @Description("Message shown by the sample plugin.")
    public String motd = "Welcome to the sample server.";

    @ConfigNullable
    public String adminContact = null;

    @Masked
    public String token = "change-me";

    public Config(@NotNull Plugin plugin) {
        super(plugin);
        initialize();
    }
}
