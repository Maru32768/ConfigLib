package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.CommandTester;
import net.kunmc.lab.commandlib.FakeSender;
import net.kunmc.lab.commandlib.TestCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static net.kunmc.lab.configlib.ConfigCommandTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class ConfigMultiConfigCommandTest {
    private TestConfig first;
    private OtherConfig second;

    @AfterEach
    void tearDown() {
        if (first != null) {
            first.close();
        }
        if (second != null) {
            second.close();
        }
    }

    @Test
    void listReloadAndResetUseMultiConfigForms() {
        first = init(new TestConfig());
        second = init(new OtherConfig());
        FakeSender sender = FakeSender.console();
        TestCommand command = commandFor(first, second);

        try (CommandTester tester = new CommandTester(command, "configlib.test")) {
            tester.execute("config list", sender);
            tester.execute("config list testConfig", sender);

            first.store.writeRaw("{\"count\":{\"value\":31},\"_version_\":0}");
            second.store.writeRaw("{\"count\":{\"value\":41},\"_version_\":0}");
            tester.execute("config reload", sender);
            assertEquals(31, first.count.value());
            assertEquals(41, second.count.value());

            first.store.writeRaw("{\"count\":{\"value\":32},\"_version_\":0}");
            second.store.writeRaw("{\"count\":{\"value\":42},\"_version_\":0}");
            tester.execute("config reload testConfig", sender);
            assertEquals(32, first.count.value());
            assertEquals(41, second.count.value());

            first.count.value(99);
            second.count.value(88);
            tester.execute("config reset testConfig", sender);
            assertEquals(10, first.count.value());
            assertEquals(88, second.count.value());

            // Bulk reset is not possible, and a help message is displayed
            first.count.value(99);
            second.count.value(88);
            tester.execute("config reset", sender);
            assertEquals(99, first.count.value());
            assertEquals(88, second.count.value());
        }

        SnapshotAssertions.assertMatchesSnapshot("config-multi-list-reload-reset.txt", messages(sender));
    }

    @Test
    void configNameAndTrailingDotListAliasesAreGenerated() {
        first = init(new TestConfig());
        second = init(new OtherConfig());
        FakeSender sender = FakeSender.console();

        try (CommandTester tester = new CommandTester(commandFor(first, second), "configlib.test")) {
            tester.execute("config testConfig", sender);
            tester.execute("config testConfig.", sender);
        }

        SnapshotAssertions.assertMatchesSnapshot("config-multi-config-aliases.txt", messages(sender));
    }

    @Test
    void prefixedFieldNamesAreAlwaysAvailableAndConflictingUnprefixedNamesAreOmitted() {
        first = init(new TestConfig());
        second = init(new OtherConfig());
        FakeSender sender = FakeSender.console();

        try (CommandTester tester = new CommandTester(commandFor(first, second), "configlib.test")) {
            tester.execute("config testConfig.count 30", sender);
            tester.execute("config otherConfig.count 40", sender);
            assertEquals(30, first.count.value());
            assertEquals(40, second.count.value());

            assertThrows(RuntimeException.class, () -> tester.execute("config count 50", sender));
        }
    }

    @Test
    void customValueNamesParticipateInConflictDetection() {
        CustomNamedConfig customFirst = initConfig(new CustomNamedConfig());
        CustomNamedOtherConfig customSecond = initConfig(new CustomNamedOtherConfig());
        first = customFirst;
        second = customSecond;
        FakeSender sender = FakeSender.console();

        try (CommandTester tester = new CommandTester(commandFor(customFirst, customSecond), "configlib.test")) {
            tester.execute("config primary.amount 30", sender);
            tester.execute("config secondary.amount renamed", sender);

            assertEquals(30, customFirst.count.value());
            assertEquals("renamed", customSecond.title.value());
            assertThrows(RuntimeException.class, () -> tester.execute("config amount 50", sender));
        }
    }

    @Test
    void nonConflictingUnprefixedFieldNamesRemainAvailable() {
        first = init(new TestConfig());
        second = init(new OtherConfig());
        FakeSender sender = FakeSender.console();

        try (CommandTester tester = new CommandTester(commandFor(first, second), "configlib.test")) {
            tester.execute("config message updated", sender);
            tester.execute("config title renamed", sender);
        }

        assertEquals("updated", first.message.value());
        assertEquals("renamed", second.title.value());
    }

    @Test
    void multiConfigHistoryDiffAndUndoTopLevelFormsAreGenerated() {
        first = init(new TestConfig());
        second = init(new OtherConfig());
        makeHistory(first);
        second.count.value(21);
        second.pushHistory();
        FakeSender sender = FakeSender.console();

        try (CommandTester tester = new CommandTester(commandFor(first, second), "configlib.test")) {
            tester.execute("config history 1", sender);
            tester.execute("config undo 1", sender);
            assertEquals(15, first.count.value());
            assertEquals(20, second.count.value());

            makeHistory(first);
            second.count.value(21);
            second.pushHistory();
            tester.execute("config undo testConfig", sender);
            assertEquals(15, first.count.value());
            assertEquals(21, second.count.value());

            makeHistory(first);
            tester.execute("config undo testConfig 2", sender);
            assertEquals(15, first.count.value());

            makeHistory(first);
            second.count.value(22);
            second.pushHistory();
            tester.execute("config diff testConfig 1", sender);
            tester.execute("config diff testConfig 0 2", sender);
        }

        assertTrue(messages(sender).stream()
                                   .anyMatch(x -> x.contains("count:")), messages(sender).toString());
    }

    @Test
    void multiConfigDiffDefaultSupportsConfigNamePrefixes() {
        first = init(new TestConfig());
        second = init(new OtherConfig());
        first.count.value(30);
        FakeSender sender = FakeSender.console();

        try (CommandTester tester = new CommandTester(commandFor(first, second), "configlib.test")) {
            tester.execute("config diff testConfig default", sender);
        }

        assertTrue(messages(sender).stream()
                                   .anyMatch(x -> x.contains("count: 10")), messages(sender).toString());
    }

    @Test
    void multiConfigHistoryConfigSpecificFormsAreGenerated() {
        first = init(new TestConfig());
        second = init(new OtherConfig());
        makeHistory(first);
        FakeSender sender = FakeSender.console();

        try (CommandTester tester = new CommandTester(commandFor(first, second), "configlib.test")) {
            tester.execute("config history testConfig", sender);
            tester.execute("config history testConfig 1", sender);
            tester.execute("config history testConfig diff 1", sender);
            tester.execute("config history testConfig diff 0 2", sender);
            tester.execute("config history testConfig undo", sender);
            assertEquals(15, first.count.value());

            makeHistory(first);
            tester.execute("config history testConfig undo 2", sender);
            assertEquals(15, first.count.value());
        }
    }

    @Test
    void multiConfigDirectConfigChildHistoryUndoAndDiffFormsAreGenerated() {
        first = init(new TestConfig());
        second = init(new OtherConfig());
        makeHistory(first);
        FakeSender sender = FakeSender.console();

        try (CommandTester tester = new CommandTester(commandFor(first, second), "configlib.test")) {
            tester.execute("config testConfig history", sender);
            tester.execute("config testConfig history 1", sender);
            tester.execute("config testConfig history diff 1", sender);
            tester.execute("config testConfig history diff 0 2", sender);
            tester.execute("config testConfig history undo", sender);
            assertEquals(15, first.count.value());

            makeHistory(first);
            tester.execute("config testConfig history undo 2", sender);
            assertEquals(15, first.count.value());

            makeHistory(first);
            tester.execute("config testConfig diff 1", sender);
            tester.execute("config testConfig diff 0 2", sender);
            tester.execute("config testConfig undo", sender);
            assertEquals(15, first.count.value());

            makeHistory(first);
            tester.execute("config testConfig undo 2", sender);
            assertEquals(15, first.count.value());
        }
    }
}
