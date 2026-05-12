package net.kunmc.lab.testplugin;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandLib;
import net.kunmc.lab.commandlib.argument.IntegerArgument;
import net.kunmc.lab.configlib.ConfigCommandBuilder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class TestPlugin extends JavaPlugin {
    public static void print(Object obj) {
        if (Objects.equals(System.getProperty("plugin.env"), "DEV")) {
            System.out.printf("[%s] %s%n", TestPlugin.class.getSimpleName(), obj);
        }
    }

    public static void broadcast(Object obj) {
        if (Objects.equals(System.getProperty("plugin.env"), "DEV")) {
            Bukkit.broadcastMessage(String.format("[%s] %s", TestPlugin.class.getSimpleName(), obj));
        }
    }

    @Override
    public void onEnable() {
        Config config = new Config(this);
        Config2 config2 = new Config2(this);
        AddValidatorTestConfig addValidatorTestConfig = new AddValidatorTestConfig(this);
        Command configCommand = new ConfigCommandBuilder(config).addConfig(config2)
                                                                .addConfig(addValidatorTestConfig)
                                                                .build();

        CommandLib.register(this, new Command("test") {{
            addChildren(configCommand);
            addChildren(new Command("addinitializelistener") {{
                execute(ctx -> {
                    config2.n.onInitialize(x -> Bukkit.broadcastMessage(x.toString()));
                });
            }});

            argument(new IntegerArgument("integer")).execute((integer, ctx) -> {
                ctx.sendSuccess(config.strings);
                config.strings.add(integer.toString());
            });
        }}, new Command("testsingle") {{
            addChildren(new ConfigCommandBuilder(config).build());
        }});
    }

    @Override
    public void onDisable() {
    }
}
