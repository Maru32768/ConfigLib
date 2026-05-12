package net.kunmc.lab.testplugin;

import net.kunmc.lab.configlib.value.IntegerValue;
import net.kunmc.lab.configlib.value.UUIDValue;
import net.kunmc.lab.configlib.value.collection.TeamSetValue;
import net.kunmc.lab.configlib.value.collection.UUIDSetValue;
import net.kunmc.lab.configlib.value.map.UUID2LocationMapValue;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Config extends AbstractConfig {
    public final UUIDValue uuidValue = new UUIDValue().description("test description");
    public final UUIDSetValue uuidSetValue = new UUIDSetValue().onModify(TestPlugin::broadcast);
    public final UUID2LocationMapValue uuid2LocationMapValue = new UUID2LocationMapValue();
    public final List<String> strings = new ArrayList<>();
    public final TeamSetValue teams = new TeamSetValue().entryName("チーム");
    public final IntegerValue conflict = new IntegerValue(1);
    public final int intField = 1;
    public final Inner innerClass = new Inner("default");


    public Config(@NotNull Plugin plugin) {
        super(plugin, opt -> {
            opt.migrateTo(1, migration -> migration.defaultValue("innerClass", new Inner("default")));
        });

        strings.add("hogehoge");

        initialize();
    }

    public static final class Inner {
        private final String str;

        public Inner(String str) {
            this.str = str;
        }
    }
}
