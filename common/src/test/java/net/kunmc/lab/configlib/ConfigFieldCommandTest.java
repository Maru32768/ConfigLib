package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.CommandTester;
import net.kunmc.lab.commandlib.FakeSender;
import net.kunmc.lab.commandlib.TestCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static net.kunmc.lab.configlib.ConfigCommandTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigFieldCommandTest {
    private TestConfig config;

    @AfterEach
    void tearDown() {
        if (config != null) {
            config.close();
        }
    }

    @Test
    void getFieldShowsSingleValue() {
        config = init(new TestConfig());
        FakeSender sender = FakeSender.console();

        execute(commandFor(config), "config count", sender);

        SnapshotAssertions.assertMatchesSnapshot("config-field-get-single-value.txt", messages(sender));
    }

    @Test
    void prefixedFieldNameIsGeneratedForSingleConfig() {
        config = init(new TestConfig());
        FakeSender sender = FakeSender.console();

        execute(commandFor(config), "config testConfig.count 26", sender);

        assertEquals(26, config.count.value());
    }

    @Test
    void customValueNameReplacesFieldName() {
        CustomNamedConfig cfg = initConfig(new CustomNamedConfig());
        config = cfg;
        FakeSender sender = FakeSender.console();

        try (CommandTester tester = new CommandTester(commandFor(cfg), "configlib.test")) {
            tester.execute("config amount 28", sender);
            tester.execute("config primary.amount 30", sender);

            assertEquals(30, cfg.count.value());
            assertThrows(RuntimeException.class, () -> tester.execute("config count 32", sender));
            assertThrows(RuntimeException.class, () -> tester.execute("config primary.count 34", sender));
        }
    }

    @Test
    void plainFieldsGenerateGetOnlyCommands() {
        PlainFieldConfig cfg = initConfig(new PlainFieldConfig());
        FakeSender sender = FakeSender.console();
        TestCommand command = commandFor(cfg);

        try (CommandTester tester = new CommandTester(command, "configlib.test")) {
            tester.execute("config label", sender);
            tester.execute("config number", sender);

            assertThrows(RuntimeException.class, () -> tester.execute("config label changed", sender));
        }

        SnapshotAssertions.assertMatchesSnapshot("config-field-plain-fields.txt", messages(sender));
    }

    @Test
    void plainFieldsAreNotGeneratedWhenGetIsDisabled() {
        PlainFieldConfig cfg = initConfig(new PlainFieldConfig());
        FakeSender sender = FakeSender.console();
        TestCommand command = new ConfigCommandBuilder(cfg).disableGet()
                                                           .build();

        try (CommandTester tester = new CommandTester(command, "configlib.test")) {
            assertThrows(RuntimeException.class, () -> tester.execute("config label", sender));
            assertThrows(RuntimeException.class, () -> tester.execute("config number", sender));
        }
    }

    @Test
    void supportedPojoFieldsAllowSetModifyCommands() {
        PojoModifyConfig cfg = initConfig(new PojoModifyConfig());
        FakeSender sender = FakeSender.console();

        try (CommandTester tester = new CommandTester(commandFor(cfg), "configlib.test")) {
            tester.execute("config maxPlayers 35", sender);
            tester.execute("config motd changed", sender);
            tester.execute("config enabled false", sender);
            tester.execute("config mode HARD", sender);
        }

        assertEquals(35, cfg.maxPlayers);
        assertEquals("changed", cfg.motd);
        assertEquals(false, cfg.enabled);
        assertEquals(Mode.HARD, cfg.mode);
    }

    @Test
    void unsupportedPojoFieldsStayGetOnly() {
        PojoModifyConfig cfg = initConfig(new PojoModifyConfig());
        FakeSender sender = FakeSender.console();

        try (CommandTester tester = new CommandTester(commandFor(cfg), "configlib.test")) {
            tester.execute("config names", sender);
            assertThrows(RuntimeException.class, () -> tester.execute("config names add steve", sender));
        }
    }

    @Test
    void pojoModifyCommandsAreNotGeneratedWhenGetIsDisabledForUnsupportedTypes() {
        PojoModifyConfig cfg = initConfig(new PojoModifyConfig());
        FakeSender sender = FakeSender.console();
        TestCommand command = new ConfigCommandBuilder(cfg).disableGet()
                                                           .build();

        try (CommandTester tester = new CommandTester(command, "configlib.test")) {
            tester.execute("config maxPlayers 45", sender);
            assertEquals(45, cfg.maxPlayers);
            assertThrows(RuntimeException.class, () -> tester.execute("config names", sender));
        }
    }

    @Test
    void nestedPojoLeafFieldsAllowSetModifyCommands() {
        NestedPojoModifyConfig cfg = initConfig(new NestedPojoModifyConfig());
        FakeSender sender = FakeSender.console();

        try (CommandTester tester = new CommandTester(commandFor(cfg), "configlib.test")) {
            tester.execute("config arena.maxArenas 10", sender);
            tester.execute("config arena.defaultName battle", sender);
        }

        assertEquals(10, cfg.arena.maxArenas);
        assertEquals("battle", cfg.arena.defaultName);
    }

    @Test
    void immutableNestedPojoLeafFieldsAllowSetModifyCommands() {
        ImmutableNestedPojoModifyConfig cfg = initConfig(new ImmutableNestedPojoModifyConfig());
        FakeSender sender = FakeSender.console();

        try (CommandTester tester = new CommandTester(commandFor(cfg), "configlib.test")) {
            tester.execute("config arena.maxArenas 10", sender);
            tester.execute("config arena.defaultName battle", sender);
        }

        assertEquals(10, cfg.arena.maxArenas());
        assertEquals("battle", cfg.arena.defaultName());
    }

    @Test
    void shorthandSetAndExplicitSetUpdateSingleValue() {
        config = init(new TestConfig());
        FakeSender sender = FakeSender.console();

        try (CommandTester tester = new CommandTester(commandFor(config), "configlib.test")) {
            tester.execute("config count 25", sender);
            assertEquals(25, config.count.value());

            tester.execute("config count set 35", sender);
            assertEquals(35, config.count.value());
        }
    }

    @Test
    void fieldResetRestoresDefaultValue() {
        config = init(new TestConfig());
        FakeSender sender = FakeSender.console();
        config.count.value(44);

        execute(commandFor(config), "config count reset", sender);

        assertEquals(10, config.count.value());
    }

    @Test
    void pojoFieldResetRestoresDefaultValue() {
        PojoModifyConfig cfg = initConfig(new PojoModifyConfig());
        FakeSender sender = FakeSender.console();
        cfg.maxPlayers = 44;

        execute(commandFor(cfg), "config maxPlayers reset", sender);

        assertEquals(20, cfg.maxPlayers);
    }

    @Test
    void nestedPojoFieldResetRestoresDefaultValue() {
        NestedPojoModifyConfig cfg = initConfig(new NestedPojoModifyConfig());
        FakeSender sender = FakeSender.console();
        cfg.arena.maxArenas = 12;

        execute(commandFor(cfg), "config arena.maxArenas reset", sender);

        assertEquals(5, cfg.arena.maxArenas);
    }

    @Test
    void numericIncAndDecSupportDefaultAndExplicitAmounts() {
        config = init(new TestConfig());
        FakeSender sender = FakeSender.console();

        try (CommandTester tester = new CommandTester(commandFor(config), "configlib.test")) {
            tester.execute("config count inc", sender);
            assertEquals(11, config.count.value());

            tester.execute("config count inc 4", sender);
            assertEquals(15, config.count.value());

            tester.execute("config count dec", sender);
            assertEquals(14, config.count.value());

            tester.execute("config count dec 6", sender);
            assertEquals(8, config.count.value());
        }
    }

    @Test
    void numericIncAndDecClampToConfiguredBounds() {
        config = init(new TestConfig());
        FakeSender sender = FakeSender.console();

        try (CommandTester tester = new CommandTester(commandFor(config), "configlib.test")) {
            tester.execute("config count inc 500", sender);
            assertEquals(100, config.count.value());

            tester.execute("config count dec 500", sender);
            assertEquals(0, config.count.value());
        }
    }

    @Test
    void getDisabledStillAllowsModifyWhenValueIsModifiable() {
        config = init(new TestConfig());
        FakeSender sender = FakeSender.console();
        TestCommand command = new ConfigCommandBuilder(config).disableGet()
                                                              .build();

        try (CommandTester tester = new CommandTester(command, "configlib.test")) {
            tester.execute("config count 22", sender);
            assertEquals(22, config.count.value());

            tester.execute("config count", sender);
            SnapshotAssertions.assertMatchesSnapshot("config-field-get-disabled-modify-output.txt", messages(sender));
        }
    }

    @Test
    void modifyDisabledStillAllowsGet() {
        config = init(new TestConfig());
        FakeSender sender = FakeSender.console();
        TestCommand command = new ConfigCommandBuilder(config).disableModify()
                                                              .build();

        try (CommandTester tester = new CommandTester(command, "configlib.test")) {
            tester.execute("config count", sender);
            SnapshotAssertions.assertMatchesSnapshot("config-field-modify-disabled-get-output.txt", messages(sender));

            assertThrows(RuntimeException.class, () -> tester.execute("config count 22", sender));
        }
    }
}
