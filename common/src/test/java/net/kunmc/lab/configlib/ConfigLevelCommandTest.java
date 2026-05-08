package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.CommandTester;
import net.kunmc.lab.commandlib.FakeSender;
import net.kunmc.lab.commandlib.TestCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static net.kunmc.lab.configlib.ConfigCommandTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigLevelCommandTest {
    private TestConfig config;

    @AfterEach
    void tearDown() {
        if (config != null) {
            config.close();
        }
    }

    @Test
    void listReloadAndResetAreGeneratedForSingleConfig() {
        config = init(new TestConfig());
        FakeSender sender = FakeSender.console();

        try (CommandTester tester = new CommandTester(commandFor(config), "configlib.test")) {
            tester.execute("config list", sender);

            config.store.writeRaw("{\"count\":{\"value\":33},\"message\":{\"value\":\"loaded\"},\"_version_\":0}");
            tester.execute("config reload", sender);
            assertEquals(33, config.count.value());
            assertEquals("loaded", config.message.value());

            tester.execute("config reset", sender);
            assertEquals(10, config.count.value());
            assertEquals("hello", config.message.value());
        }

        SnapshotAssertions.assertMatchesSnapshot("config-level-list-reload-reset.txt", messages(sender));
    }

    @Test
    void configNameAndTrailingDotListAliasesAreGeneratedForSingleConfig() {
        config = init(new TestConfig());
        FakeSender sender = FakeSender.console();

        try (CommandTester tester = new CommandTester(commandFor(config), "configlib.test")) {
            tester.execute("config testConfig", sender);
            tester.execute("config testConfig.", sender);
        }

        SnapshotAssertions.assertMatchesSnapshot("config-level-config-aliases.txt", messages(sender));
    }

    @Test
    void customRootAndConfigNamesAreUsedWhenGeneratingCommands() {
        CustomNamedConfig cfg = initConfig(new CustomNamedConfig());
        config = cfg;
        FakeSender sender = FakeSender.console();
        TestCommand command = new ConfigCommandBuilder(cfg).name("settings")
                                                           .build();

        try (CommandTester tester = new CommandTester(command, "configlib.test")) {
            tester.execute("settings primary", sender);
            tester.execute("settings primary.", sender);
            tester.execute("settings primary.amount 22", sender);

            assertEquals(22, cfg.count.value());
            assertThrows(RuntimeException.class, () -> tester.execute("config primary", sender));
            assertThrows(RuntimeException.class, () -> tester.execute("settings testConfig", sender));
        }

        SnapshotAssertions.assertMatchesSnapshot("config-level-custom-root-and-config-names.txt", messages(sender));
    }

    @Test
    void builderCanDisableConfigLevelCommands() {
        config = init(new TestConfig());
        FakeSender sender = FakeSender.console();
        TestCommand command = new ConfigCommandBuilder(config).disableList()
                                                              .disableReload()
                                                              .disableReset()
                                                              .disableHistory()
                                                              .build();

        try (CommandTester tester = new CommandTester(command, "configlib.test")) {
            assertThrows(RuntimeException.class, () -> tester.execute("config list", sender));
            assertThrows(RuntimeException.class, () -> tester.execute("config reload", sender));
            assertThrows(RuntimeException.class, () -> tester.execute("config reset", sender));
            assertThrows(RuntimeException.class, () -> tester.execute("config history", sender));
            assertThrows(RuntimeException.class, () -> tester.execute("config undo", sender));
            assertThrows(RuntimeException.class, () -> tester.execute("config diff 1", sender));
        }
    }

    @Test
    void configCanDisableItsOwnConfigLevelCommands() {
        DisabledConfig cfg = new DisabledConfig();
        config = cfg;
        init(cfg);
        FakeSender sender = FakeSender.console();
        TestCommand command = commandFor(cfg);

        try (CommandTester tester = new CommandTester(command, "configlib.test")) {
            assertThrows(RuntimeException.class, () -> tester.execute("config list", sender));
            assertThrows(RuntimeException.class, () -> tester.execute("config reload", sender));
            assertThrows(RuntimeException.class, () -> tester.execute("config reset", sender));
            assertThrows(RuntimeException.class, () -> tester.execute("config history", sender));
        }
    }

    @Test
    void configLevelResetRestoresPojoDefaults() {
        PojoModifyConfig cfg = initConfig(new PojoModifyConfig());
        config = null;
        FakeSender sender = FakeSender.console();

        cfg.maxPlayers = 45;
        cfg.motd = "changed";
        cfg.enabled = false;
        cfg.mode = Mode.HARD;
        cfg.names = java.util.List.of("steve");

        try (CommandTester tester = new CommandTester(commandFor(cfg), "configlib.test")) {
            tester.execute("config reset", sender);
        }

        assertEquals(20, cfg.maxPlayers);
        assertEquals("hello", cfg.motd);
        assertEquals(true, cfg.enabled);
        assertEquals(Mode.EASY, cfg.mode);
        assertEquals(java.util.List.of("alex"), cfg.names);
        cfg.close();
    }

    static class DisabledConfig extends TestConfig {
        DisabledConfig() {
            disableList();
            disableReload();
            disableReset();
            disableHistory();
        }
    }
}
