package net.kunmc.lab.sampleplugin;

import net.kunmc.lab.commandlib.CommandLib;
import net.kunmc.lab.configlib.ConfigCommandBuilder;
import net.kunmc.lab.commandlib.Command;
import org.bukkit.plugin.java.JavaPlugin;

public final class SamplePlugin extends JavaPlugin {
    public static Config config;

    @Override
    public void onEnable() {
        config = new Config(this);

        Command configCommand = new ConfigCommandBuilder(config).build();
        CommandLib.register(this, new TestCommand(configCommand));
    }
}
