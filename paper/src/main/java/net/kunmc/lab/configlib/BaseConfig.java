package net.kunmc.lab.configlib;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kunmc.lab.commandlib.argument.Nameable;
import net.kunmc.lab.configlib.gson.*;
import net.kunmc.lab.configlib.store.ConfigStore;
import net.kunmc.lab.configlib.store.UnknownKeyPolicy;
import net.kunmc.lab.configlib.store.YamlFileConfigStore;
import net.kunmc.lab.configlib.value.tuple.ConfigPair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.function.Consumer;

public abstract class BaseConfig extends CommonBaseConfig implements Listener {
    private final transient Plugin plugin;
    private final transient Consumer<Option> options;
    private transient Gson gson;
    private transient Consumer<Exception> exceptionHandler;

    public BaseConfig(@NotNull Plugin plugin) {
        this(plugin, option -> {
        });
    }

    public BaseConfig(@NotNull Plugin plugin, Consumer<Option> options) {
        this.plugin = plugin;
        this.options = options;
    }

    /**
     * Starts the automatic config synchronization. Must be called at the end of the subclass constructor.
     * The plugin must already be enabled (i.e., called from {@code onEnable}).
     */
    public BaseConfig initialize() {
        Bukkit.getPluginManager()
              .registerEvents(new Listener() {
                  @EventHandler
                  public void onPluginDisable(PluginDisableEvent e) {
                      if (e.getPlugin() == plugin) {
                          close();
                      }
                  }
              }, plugin);
        Option opt = new Option();
        options.accept(opt);
        gson = opt.buildGson();
        exceptionHandler = opt.exceptionHandler;
        init(opt);
        return this;
    }

    public Plugin plugin() {
        return plugin;
    }

    protected final Gson gson() {
        return gson;
    }

    protected final Consumer<Exception> exceptionHandler() {
        return exceptionHandler;
    }

    protected File getConfigFolder() {
        return plugin.getDataFolder();
    }

    private File defaultConfigFile() {
        return new File(getConfigFolder(), entryName() + ".yml");
    }

    @Override
    protected ConfigStore createConfigStore() {
        return new YamlFileConfigStore(defaultConfigFile(), gson, exceptionHandler, 50, UnknownKeyPolicy.PRESERVE);
    }

    public static final class Option extends CommonBaseConfig.Option {
        private Consumer<GsonBuilder> gsonCustomizer = builder -> {
        };
        private Consumer<Exception> exceptionHandler = Throwable::printStackTrace;

        public Option gsonCustomizer(Consumer<GsonBuilder> gsonCustomizer) {
            this.gsonCustomizer = gsonCustomizer;
            return this;
        }

        public Option exceptionHandler(Consumer<Exception> exceptionHandler) {
            this.exceptionHandler = exceptionHandler;
            return this;
        }

        private Gson buildGson() {
            GsonBuilder builder = new GsonBuilder().setPrettyPrinting()
                                                   .enableComplexMapKeySerialization()
                                                   .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
                                                   .registerTypeAdapter(ConfigPair.class, new PairTypeAdapter<>())
                                                   .registerTypeHierarchyAdapter(Team.class, new TeamTypeAdapter())
                                                   .registerTypeHierarchyAdapter(BlockData.class,
                                                                                 new BlockDataTypeAdapter())
                                                   .registerTypeHierarchyAdapter(ItemStack.class,
                                                                                 new ItemStackTypeAdapter())
                                                   .registerTypeHierarchyAdapter(Location.class,
                                                                                 new LocationTypeAdapter())
                                                   .registerTypeHierarchyAdapter(Value.class, new ValueTypeAdapter())
                                                   .registerTypeHierarchyAdapter(Nameable.class,
                                                                                 new NameableTypeAdapter())
                                                   .registerTypeHierarchyAdapter(Set.class, new SetTypeAdapter());
            gsonCustomizer.accept(builder);
            return builder.create();
        }
    }
}
