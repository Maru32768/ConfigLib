package net.kunmc.lab.configlib;

import com.google.gson.Gson;
import net.kunmc.lab.commandlib.TestCommand;
import net.kunmc.lab.commandlib.CommandTester;
import net.kunmc.lab.commandlib.FakeSender;
import net.kunmc.lab.configlib.annotation.Range;
import net.kunmc.lab.configlib.store.ConfigStore;
import net.kunmc.lab.configlib.store.InMemoryConfigStore;
import net.kunmc.lab.configlib.value.IntegerValue;
import net.kunmc.lab.configlib.value.StringValue;
import net.kunmc.lab.configlib.value.collection.StringListValue;
import net.kunmc.lab.configlib.value.map.String2IntegerMapValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ConfigCommandTestSupport {
    private ConfigCommandTestSupport() {
    }

    static TestConfig init(TestConfig cfg) {
        cfg.init(option());
        cfg.close();
        return cfg;
    }

    static OtherConfig init(OtherConfig cfg) {
        cfg.init(option());
        cfg.close();
        return cfg;
    }

    static <T extends CommonBaseConfig> T initConfig(T cfg) {
        cfg.init(option());
        cfg.close();
        return cfg;
    }

    static CommonBaseConfig.Option option() {
        return new CommonBaseConfig.Option().modifyDetectionTimerPeriod(10_000);
    }

    static TestCommand commandFor(CommonBaseConfig cfg) {
        return new ConfigCommandBuilder(cfg).build();
    }

    static TestCommand commandFor(CommonBaseConfig first, CommonBaseConfig second) {
        return new ConfigCommandBuilder(first).addConfig(second)
                                              .build();
    }

    static void execute(TestCommand command, String input, FakeSender sender) {
        try (CommandTester tester = new CommandTester(command, "configlib.test")) {
            tester.execute(input, sender);
        }
    }

    static void makeHistory(TestConfig cfg) {
        cfg.count.value(15);
        cfg.pushHistory();
        cfg.count.value(25);
        cfg.message.value("changed");
        cfg.pushHistory();
    }

    static List<String> messages(FakeSender sender) {
        return sender.getSentMessageLegacyTexts();
    }

    static class TestConfig extends CommonBaseConfig {
        final IntegerValue count = new IntegerValue(10, 0, 100);
        final StringValue message = new StringValue("hello");
        final StringListValue names = new StringListValue().addAllowableString("alex")
                                                           .addAllowableString("steve");
        final String2IntegerMapValue scores = new String2IntegerMapValue(new LinkedHashMap<>()).addAllowableKeyString(
                                                                                                       "alice")
                                                                                               .addAllowableKeyString(
                                                                                                       "bob");
        final transient InMemoryConfigStore store = new InMemoryConfigStore(new Gson());

        @Override
        protected ConfigStore createConfigStore() {
            return store;
        }
    }

    static class OtherConfig extends CommonBaseConfig {
        final IntegerValue count = new IntegerValue(20, 0, 100);
        final StringValue title = new StringValue("other");
        final String2IntegerMapValue scores = new String2IntegerMapValue(Map.of("bob", 2)).addAllowableKeyString("bob");
        final transient InMemoryConfigStore store = new InMemoryConfigStore(new Gson());

        @Override
        protected ConfigStore createConfigStore() {
            return store;
        }
    }

    static class CustomNamedConfig extends TestConfig {
        CustomNamedConfig() {
            entryName("primary");
            count.entryName("amount");
        }
    }

    static class CustomNamedOtherConfig extends OtherConfig {
        CustomNamedOtherConfig() {
            entryName("secondary");
            title.entryName("amount");
        }
    }

    static class PlainFieldConfig extends CommonBaseConfig {
        final String label = "plain";
        final int number = 42;
        final transient InMemoryConfigStore store = new InMemoryConfigStore(new Gson());

        @Override
        protected ConfigStore createConfigStore() {
            return store;
        }
    }

    static class PojoModifyConfig extends CommonBaseConfig {
        @Range(min = 0, max = 100)
        int maxPlayers = 20;
        String motd = "hello";
        boolean enabled = true;
        Mode mode = Mode.EASY;
        List<String> names = List.of("alex");
        final transient InMemoryConfigStore store = new InMemoryConfigStore(new Gson());

        @Override
        protected ConfigStore createConfigStore() {
            return store;
        }
    }

    enum Mode {
        EASY,
        HARD
    }

    static class NestedPojoModifyConfig extends CommonBaseConfig {
        ArenaSettings arena = new ArenaSettings();
        final transient InMemoryConfigStore store = new InMemoryConfigStore(new Gson());

        @Override
        protected ConfigStore createConfigStore() {
            return store;
        }

        static class ArenaSettings {
            @Range(min = 1, max = 50)
            int maxArenas = 5;
            String defaultName = "arena";
        }
    }

    static class ImmutableNestedPojoModifyConfig extends CommonBaseConfig {
        ArenaSettings arena = new ArenaSettings(5, "arena");
        final transient InMemoryConfigStore store = new InMemoryConfigStore(new Gson());

        @Override
        protected ConfigStore createConfigStore() {
            return store;
        }

        static class ArenaSettings {
            private final int maxArenas;
            private final String defaultName;

            ArenaSettings(int maxArenas, String defaultName) {
                this.maxArenas = maxArenas;
                this.defaultName = defaultName;
            }

            int maxArenas() {
                return maxArenas;
            }

            String defaultName() {
                return defaultName;
            }
        }
    }
}
