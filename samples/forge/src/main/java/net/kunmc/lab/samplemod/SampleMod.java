package net.kunmc.lab.samplemod;

import net.kunmc.lab.commandlib.CommandLib;
import net.kunmc.lab.configlib.ConfigCommandBuilder;
import net.kunmc.lab.commandlib.Command;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

@Mod(SampleMod.MOD_ID)
public class SampleMod {
    public static final String MOD_ID = "samplemod";
    public static final CommonConfig commonConfig = new CommonConfig(MOD_ID);
    public static final ClientConfig clientConfig = new ClientConfig(MOD_ID);
    public static final ServerConfig serverConfig = new ServerConfig(MOD_ID);
    private final ConfigCommandBuilder builder;

    public SampleMod() {
        builder = new ConfigCommandBuilder(commonConfig).addConfig(serverConfig)
                                                        .sort();

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    @OnlyIn(Dist.CLIENT)
    public void onServerStartingOnClient(FMLServerStartingEvent event) {
        builder.addConfig(clientConfig);
    }

    @SubscribeEvent
    public void onServerStartingOnDedicated(FMLServerStartingEvent event) {
        Command configCommand = builder.build();
        CommandLib.register("samplemod.command", new TestCommand(configCommand));
    }
}
