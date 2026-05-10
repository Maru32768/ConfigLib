package net.kunmc.lab.configlib.store;

import com.google.gson.Gson;
import net.kunmc.lab.configlib.CommonBaseConfig;
import net.kunmc.lab.configlib.annotation.Description;
import net.kunmc.lab.configlib.migration.Migrations;
import net.kunmc.lab.configlib.value.IntegerValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class YamlFileConfigStoreTest {
    @TempDir
    File tempDir;

    private File configFile;
    private Gson gson;
    private YamlFileConfigStore store;

    @BeforeEach
    void setUp() {
        configFile = new File(tempDir, "config.yml");
        gson = new Gson();
        store = new YamlFileConfigStore(configFile, gson);
    }

    private Migrations noMigrations() {
        return Migrations.empty();
    }

    private void writeFile(String yaml) throws IOException {
        Files.writeString(configFile.toPath(), yaml, StandardCharsets.UTF_8);
    }

    private String readFile() throws IOException {
        return Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
    }

    @Test
    void writeCreatesYamlFile() throws IOException {
        SimpleConfig cfg = new SimpleConfig();
        cfg.value = 42;

        store.write(cfg);

        assertTrue(configFile.exists());
        assertTrue(readFile().contains("value: 42"), readFile());
    }

    @Test
    void readDeserializesYaml() throws IOException {
        writeFile("value: 99\n_version_: 0\n");

        SimpleConfig loaded = (SimpleConfig) store.read(SimpleConfig.class, noMigrations(), new SimpleConfig());

        assertEquals(99, loaded.value);
    }

    @Test
    void readEmptyFileAsEmptyObjectAndKeepsDefaults() throws IOException {
        writeFile("");

        SimpleConfig loaded = (SimpleConfig) store.read(SimpleConfig.class, noMigrations(), new SimpleConfig(7));

        assertEquals(7, loaded.value);
    }

    @Test
    void readRejectsNonObjectRoot() throws IOException {
        writeFile("- value: 1\n");

        InvalidConfigFormatException ex = assertThrows(InvalidConfigFormatException.class,
                                                       () -> store.read(SimpleConfig.class,
                                                                        noMigrations(),
                                                                        new SimpleConfig()));

        assertEquals("Config root must be an object.", ex.getMessage());
    }

    @Test
    void readAppliesMigrationAndUpdatesYaml() throws IOException {
        writeFile("value: 3\n_version_: 0\n");

        SimpleConfig loaded = (SimpleConfig) store.read(SimpleConfig.class,
                                                        Migrations.builder()
                                                                  .migrateTo(1,
                                                                             migration -> migration.convert("value",
                                                                                                            Integer.class,
                                                                                                            Integer.class,
                                                                                                            value -> value * 10))
                                                                  .build(),
                                                        new SimpleConfig());

        assertEquals(30, loaded.value);
        assertTrue(readFile().contains("_version_: 1"), readFile());
    }

    @Test
    void writeAddsDescriptionCommentForValueFields() throws IOException {
        ValueFieldConfig cfg = new ValueFieldConfig();

        store.write(cfg);

        assertTrue(readFile().contains("# Maximum number of active arenas."), readFile());
        assertTrue(readFile().contains("maxArenas:"), readFile());
        assertFalse(readFile().contains("description:"), readFile());
    }

    @Test
    void writeAddsDescriptionCommentForPojoFields() throws IOException {
        PojoFieldConfig cfg = new PojoFieldConfig();

        store.write(cfg);

        assertTrue(readFile().contains("# Maximum player count."), readFile());
        assertTrue(readFile().contains("maxPlayers: 20"), readFile());
    }

    @Test
    void writeAddsDescriptionCommentForNestedPojoFields() throws IOException {
        NestedPojoConfig cfg = new NestedPojoConfig();

        store.write(cfg);

        assertTrue(readFile().contains("# Arena settings."), readFile());
        assertTrue(readFile().contains("arena:"), readFile());
        assertTrue(readFile().contains("  # Maximum number of arenas."), readFile());
        assertTrue(readFile().contains("  maxArenas: 5"), readFile());
    }

    @Test
    void writeKeepsExternalDiskChangeWhenMemoryDidNotChangeThatField() throws IOException {
        writeFile("value: 1\nother: 2\n_version_: 0\n");
        TwoFieldConfig loaded = (TwoFieldConfig) store.read(TwoFieldConfig.class, noMigrations(), new TwoFieldConfig());

        loaded.value = 10;
        writeFile("value: 1\nother: 20\n_version_: 0\n");
        TwoFieldConfig saved = (TwoFieldConfig) store.write(loaded, TwoFieldConfig.class, noMigrations());

        assertEquals(10, saved.value);
        assertEquals(20, saved.other);
        assertTrue(readFile().contains("value: 10"), readFile());
        assertTrue(readFile().contains("other: 20"), readFile());
    }

    @Test
    void removeUnknownKeyPolicyRemovesUnknownKeysOnWrite() throws IOException {
        YamlFileConfigStore removing = new YamlFileConfigStore(configFile,
                                                               gson,
                                                               Throwable::printStackTrace,
                                                               50,
                                                               UnknownKeyPolicy.REMOVE);
        writeFile("value: 1\nunknown: 2\n_version_: 0\n");
        SimpleConfig loaded = (SimpleConfig) removing.read(SimpleConfig.class, noMigrations(), new SimpleConfig());

        loaded.value = 10;
        removing.write(loaded, SimpleConfig.class, noMigrations());

        assertFalse(readFile().contains("unknown:"), readFile());
        assertTrue(readFile().contains("value: 10"), readFile());
    }

    @Test
    void failUnknownKeyPolicyRejectsUnknownKeysOnRead() throws IOException {
        YamlFileConfigStore failing = new YamlFileConfigStore(configFile,
                                                              gson,
                                                              Throwable::printStackTrace,
                                                              50,
                                                              UnknownKeyPolicy.FAIL);
        writeFile("value: 1\nunknown: 2\n_version_: 0\n");

        UnknownConfigKeyException ex = assertThrows(UnknownConfigKeyException.class,
                                                    () -> failing.read(SimpleConfig.class,
                                                                       noMigrations(),
                                                                       new SimpleConfig()));

        assertEquals("Unknown config key: unknown", ex.getMessage());
    }

    @Test
    void historyAndRestoreHistoryIndexUseYamlHistoryFile() throws IOException {
        store.pushHistory(new SimpleConfig(10));
        store.pushHistory(new SimpleConfig(20));

        List<HistoryEntry> history = store.readHistory(SimpleConfig.class, noMigrations());
        assertEquals(2, history.size());
        assertEquals(20,
                     ((SimpleConfig) history.get(0)
                                            .config()).value);
        assertTrue(store.canRestoreHistoryIndex(1));

        SimpleConfig restored = (SimpleConfig) store.restoreHistoryIndex(SimpleConfig.class, noMigrations(), 1);

        assertEquals(10, restored.value);
        File historyFile = new File(tempDir, "config.history.yml");
        assertTrue(historyFile.exists());
        String historyYaml = Files.readString(historyFile.toPath(), StandardCharsets.UTF_8);
        assertTrue(historyYaml.startsWith("history:"), historyYaml);
    }

    @Test
    void auditFileStoresChangesInYaml() throws IOException {
        store.pushAudit(new AuditEntry(123L,
                                       new ChangeTrace(ChangeSource.COMMAND,
                                                       new ChangeActor("console", null),
                                                       "set value",
                                                       List.of("value")),
                                       List.of(new AuditChange("value", "10", "20"))));

        List<AuditEntry> audit = store.readAudit();
        assertEquals(1, audit.size());
        assertEquals("10",
                     audit.get(0)
                          .changes()
                          .get(0)
                          .beforeText());

        File auditFile = new File(tempDir, "config.audit.yml");
        assertTrue(auditFile.exists());
        String raw = Files.readString(auditFile.toPath(), StandardCharsets.UTF_8);
        assertTrue(raw.contains("changes:"), raw);
        assertTrue(raw.contains("before: '10'") || raw.contains("before: \"10\"") || raw.contains("before: 10"), raw);
        assertTrue(raw.contains("after: '20'") || raw.contains("after: \"20\"") || raw.contains("after: 20"), raw);
    }

    static class SimpleConfig extends CommonBaseConfig {
        int value = 0;

        SimpleConfig() {
        }

        SimpleConfig(int value) {
            this.value = value;
        }

        @Override
        protected ConfigStore createConfigStore() {
            return new InMemoryConfigStore(new Gson());
        }
    }

    static class TwoFieldConfig extends CommonBaseConfig {
        int value;
        int other;

        @Override
        protected ConfigStore createConfigStore() {
            return new InMemoryConfigStore(new Gson());
        }
    }

    static class ValueFieldConfig extends CommonBaseConfig {
        public final IntegerValue maxArenas = new IntegerValue(3).description("Maximum number of active arenas.");

        @Override
        protected ConfigStore createConfigStore() {
            return new InMemoryConfigStore(new Gson());
        }
    }

    static class PojoFieldConfig extends CommonBaseConfig {
        @Description("Maximum player count.")
        public int maxPlayers = 20;

        @Override
        protected ConfigStore createConfigStore() {
            return new InMemoryConfigStore(new Gson());
        }
    }

    static class NestedPojoConfig extends CommonBaseConfig {
        @Description("Arena settings.")
        public ArenaSettings arena = new ArenaSettings();

        @Override
        protected ConfigStore createConfigStore() {
            return new InMemoryConfigStore(new Gson());
        }

        static class ArenaSettings {
            @Description("Maximum number of arenas.")
            public int maxArenas = 5;
        }
    }
}
