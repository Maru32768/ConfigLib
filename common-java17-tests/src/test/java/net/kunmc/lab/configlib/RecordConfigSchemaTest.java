package net.kunmc.lab.configlib;

import com.google.gson.Gson;
import net.kunmc.lab.commandlib.CommandTester;
import net.kunmc.lab.commandlib.FakeSender;
import net.kunmc.lab.configlib.annotation.ConfigNullable;
import net.kunmc.lab.configlib.annotation.Description;
import net.kunmc.lab.configlib.annotation.Range;
import net.kunmc.lab.configlib.exception.ConfigValidationException;
import net.kunmc.lab.configlib.exception.InvalidValueException;
import net.kunmc.lab.configlib.schema.ConfigSchemaEntry;
import net.kunmc.lab.configlib.store.ConfigStore;
import net.kunmc.lab.configlib.store.InMemoryConfigStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecordConfigSchemaTest {

    static final class RecordConfig extends CommonBaseConfig {
        final transient InMemoryConfigStore store = new InMemoryConfigStore(new Gson());
        public ArenaSettings arena = new ArenaSettings(5, "default");

        @Override
        protected ConfigStore createConfigStore() {
            return store;
        }

        record ArenaSettings(@Description("Maximum number of arenas.") @Range(min = 1, max = 50) int maxArenas,
                             @ConfigNullable String name) {
        }
    }

    @Test
    void recordComponentsAppearWithDottedPaths() {
        RecordConfig cfg = new RecordConfig();
        cfg.init(new CommonBaseConfig.Option());

        assertTrue(cfg.schema()
                      .findEntry("arena.maxArenas")
                      .isPresent());
        assertTrue(cfg.schema()
                      .findEntry("arena.name")
                      .isPresent());
        assertFalse(cfg.schema()
                       .findEntry("arena")
                       .isPresent());
    }

    @Test
    void recordDescriptionAnnotationBecomesMetadata() {
        RecordConfig cfg = new RecordConfig();
        cfg.init(new CommonBaseConfig.Option());

        ConfigSchemaEntry<?> entry = cfg.schema()
                                        .findEntry("arena.maxArenas")
                                        .orElseThrow(AssertionError::new);
        assertEquals("Maximum number of arenas.",
                     entry.metadata()
                          .description());
    }

    @Test
    void recordRangeAnnotationBecomesValidator() {
        RecordConfig cfg = new RecordConfig();
        cfg.init(new CommonBaseConfig.Option());

        @SuppressWarnings("unchecked") ConfigSchemaEntry<Integer> entry = (ConfigSchemaEntry<Integer>) cfg.schema()
                                                                                                          .findEntry(
                                                                                                                  "arena.maxArenas")
                                                                                                          .orElseThrow(
                                                                                                                  AssertionError::new);
        assertDoesNotThrow(() -> entry.validate(10));
        assertThrows(InvalidValueException.class, () -> entry.validate(51));
    }

    @Test
    void recordAccessorReadsCurrentValue() {
        RecordConfig cfg = new RecordConfig();
        cfg.init(new CommonBaseConfig.Option());

        @SuppressWarnings("unchecked") ConfigSchemaEntry<Integer> entry = (ConfigSchemaEntry<Integer>) cfg.schema()
                                                                                                          .findEntry(
                                                                                                                  "arena.maxArenas")
                                                                                                          .orElseThrow(
                                                                                                                  AssertionError::new);
        assertEquals(5, entry.get());
    }

    @Test
    void recordSetReconstructsRecordAndPreservesOtherComponents() {
        RecordConfig cfg = new RecordConfig();
        cfg.init(new CommonBaseConfig.Option());

        @SuppressWarnings("unchecked") ConfigSchemaEntry<Integer> entry = (ConfigSchemaEntry<Integer>) cfg.schema()
                                                                                                          .findEntry(
                                                                                                                  "arena.maxArenas")
                                                                                                          .orElseThrow(
                                                                                                                  AssertionError::new);
        entry.set(10);

        assertEquals(10, cfg.arena.maxArenas());
        assertEquals("default", cfg.arena.name());
    }

    @Test
    void recordLeafFieldsAllowSetModifyCommands() {
        RecordConfig cfg = new RecordConfig();
        cfg.init(new CommonBaseConfig.Option().modifyDetectionTimerPeriod(10_000));
        cfg.close();
        FakeSender sender = FakeSender.console();

        try (CommandTester tester = new CommandTester(new ConfigCommandBuilder(cfg).build(), "configlib.test")) {
            tester.execute("config arena.maxArenas 10", sender);
            tester.execute("config arena.name battle", sender);
        }

        assertEquals(10, cfg.arena.maxArenas());
        assertEquals("battle", cfg.arena.name());
    }

    @Test
    void recordLoadRoundTrip() {
        RecordConfig cfg = new RecordConfig();
        cfg.init(new CommonBaseConfig.Option());
        cfg.store.writeRaw("{\"arena\":{\"maxArenas\":12,\"name\":\"test\"},\"_version_\":0}");

        cfg.loadConfig();

        assertEquals(12, cfg.arena.maxArenas());
        assertEquals("test", cfg.arena.name());
    }

    @Test
    void recordLoadValidatesRangeAnnotation() {
        RecordConfig cfg = new RecordConfig();
        cfg.init(new CommonBaseConfig.Option());
        cfg.store.writeRaw("{\"arena\":{\"maxArenas\":99,\"name\":\"bad\"},\"_version_\":0}");

        assertThrows(ConfigValidationException.class, cfg::loadConfig);
    }

    @Test
    void recordNullableAllowsNull() {
        RecordConfig cfg = new RecordConfig();
        cfg.init(new CommonBaseConfig.Option());
        cfg.store.writeRaw("{\"arena\":{\"maxArenas\":5,\"name\":null},\"_version_\":0}");

        cfg.loadConfig();

        assertNull(cfg.arena.name());
    }

    @Test
    void recordMutationDetectionSavesChanges() {
        RecordConfig cfg = new RecordConfig();
        cfg.init(new CommonBaseConfig.Option());

        cfg.mutate(() -> cfg.arena = new RecordConfig.ArenaSettings(15, "arena"));

        assertTrue(cfg.store.readRaw()
                            .contains("\"maxArenas\":15"));
    }

    // --- deep nesting (record-within-record) ---

    static final class DeepRecordConfig extends CommonBaseConfig {
        final transient InMemoryConfigStore store = new InMemoryConfigStore(new Gson());
        public Outer outer = new Outer(new Outer.Inner(5, "hello"), 1);

        @Override
        protected ConfigStore createConfigStore() {
            return store;
        }

        record Outer(Inner inner, int x) {
            record Inner(int y, String z) {
            }
        }
    }

    @Test
    void deepRecordNestingFieldsAppearWithDottedPaths() {
        DeepRecordConfig cfg = new DeepRecordConfig();
        cfg.init(new CommonBaseConfig.Option());

        assertTrue(cfg.schema()
                      .findEntry("outer.inner.y")
                      .isPresent());
        assertTrue(cfg.schema()
                      .findEntry("outer.inner.z")
                      .isPresent());
        assertTrue(cfg.schema()
                      .findEntry("outer.x")
                      .isPresent());
        assertFalse(cfg.schema()
                       .findEntry("outer.inner")
                       .isPresent());
        assertFalse(cfg.schema()
                       .findEntry("outer")
                       .isPresent());
    }

    @Test
    void deepRecordNestingGetReadsCorrectValue() {
        DeepRecordConfig cfg = new DeepRecordConfig();
        cfg.init(new CommonBaseConfig.Option());

        @SuppressWarnings("unchecked") ConfigSchemaEntry<Integer> entry = (ConfigSchemaEntry<Integer>) cfg.schema()
                                                                                                          .findEntry(
                                                                                                                  "outer.inner.y")
                                                                                                          .orElseThrow(
                                                                                                                  AssertionError::new);
        assertEquals(5, entry.get());
    }

    @Test
    void deepRecordNestingSetReconstructsChainAndPreservesOtherComponents() {
        DeepRecordConfig cfg = new DeepRecordConfig();
        cfg.init(new CommonBaseConfig.Option());

        @SuppressWarnings("unchecked") ConfigSchemaEntry<Integer> entry = (ConfigSchemaEntry<Integer>) cfg.schema()
                                                                                                          .findEntry(
                                                                                                                  "outer.inner.y")
                                                                                                          .orElseThrow(
                                                                                                                  AssertionError::new);
        entry.set(99);

        assertEquals(99,
                     cfg.outer.inner()
                              .y());
        assertEquals("hello",
                     cfg.outer.inner()
                              .z());
        assertEquals(1, cfg.outer.x());
    }

    @Test
    void deepRecordNestingLoadRoundTrip() {
        DeepRecordConfig cfg = new DeepRecordConfig();
        cfg.init(new CommonBaseConfig.Option());
        cfg.store.writeRaw("{\"outer\":{\"inner\":{\"y\":42,\"z\":\"world\"},\"x\":7},\"_version_\":0}");

        cfg.loadConfig();

        assertEquals(42,
                     cfg.outer.inner()
                              .y());
        assertEquals("world",
                     cfg.outer.inner()
                              .z());
        assertEquals(7, cfg.outer.x());
    }
}
