package net.kunmc.lab.samplemod;

import net.kunmc.lab.configlib.BaseConfig;
import net.kunmc.lab.configlib.annotation.ConfigNullable;
import net.kunmc.lab.configlib.annotation.Description;
import net.kunmc.lab.configlib.annotation.Masked;
import net.kunmc.lab.configlib.annotation.Range;
import net.kunmc.lab.configlib.value.IntegerValue;
import net.kunmc.lab.configlib.value.LocationValue;
import net.kunmc.lab.configlib.value.TeamValue;
import net.kunmc.lab.configlib.value.collection.BlockStateSetValue;
import net.kunmc.lab.configlib.value.map.Enum2DoubleMapValue;
import net.kunmc.lab.configlib.value.tuple.Integer2IntegerPairValue;
import net.minecraft.item.ItemTier;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ServerConfig extends BaseConfig {
    public final IntegerValue maxPlayers = new IntegerValue(20, 1, 100).description("Maximum players per arena.");
    public final LocationValue spawnLocation = new LocationValue().description("Default spawn location.");
    public final BlockStateSetValue protectedBlocks = new BlockStateSetValue().description(
            "Blocks protected by this mod.");
    public final Enum2DoubleMapValue<ItemTier> enum2DoubleMapValue = new Enum2DoubleMapValue<>(ItemTier.class);
    public final TeamValue teamValue = new TeamValue().onModify(team -> {
        ServerLifecycleHooks.getCurrentServer()
                            .getPlayerList()
                            .getPlayers()
                            .forEach(p -> {
                                p.sendMessage(new StringTextComponent("teamValue modified!"), UUID.randomUUID());
                            });
    });
    public final Integer2IntegerPairValue integer2IntegerPairValue = new Integer2IntegerPairValue(1, 100).leftMin(1)
                                                                                                         .leftMax(99)
                                                                                                         .rightMin(2)
                                                                                                         .rightMax(100);

    @Description("Simple POJO field edited with scalar set commands.")
    @Range(min = 1, max = 50)
    public int maxArenas = 5;

    @Description("Message shown by the sample mod.")
    public String motd = "Welcome to the sample server.";

    @ConfigNullable
    public String adminContact = null;

    @Masked
    public String token = "change-me";

    public ServerConfig(@NotNull String modId) {
        super(modId, Type.SERVER);
    }
}
