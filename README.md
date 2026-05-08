# ConfigLib - A Type-Safe Configuration API for Bukkit and Forge

[![](https://jitpack.io/v/Maru32768/ConfigLib.svg)](https://jitpack.io/#Maru32768/ConfigLib)

ConfigLib is an advanced, type-safe Configuration API designed to simplify configuration management for Bukkit and Forge
developers.

## Features

1. **Type-Safety Configuration Handling**  
   Ensures configuration values are used in a type-safe manner directly within your code, reducing potential runtime
   errors and improving maintainability.
2. **Two Configuration APIs**  
   Choose the style that fits your needs:
    - **Value API** — wrap each field in a typed `Value` object for full control over commands, validation,
      tab-completion, and display.
    - **POJO API** — declare plain Java fields, nested immutable classes, and nested Java 16+ `record`s, then annotate
      with `@Description`, `@Range`, `@ConfigNullable`. Nested POJOs are expanded automatically.
3. **YAML and JSON Storage**  
   YAML is the default format for Bukkit and Forge. `description()` and `@Description` annotations are written as YAML
   comments. JSON is available by overriding `createConfigStore()`.
4. **Automatic Configuration Reloading**  
   Monitors configuration files and reloads them automatically when changes are detected, ensuring your application
   always works with the latest settings.
5. **Automatic Configuration Saving**  
   Automatically saves updated configuration values to disk, preventing data loss and ensuring persistence.
6. **Command Generation for Configuration Management**  
   Seamlessly integrates with [CommandLib](https://github.com/Maru32768/CommandLib) to generate commands for managing
   configurations via the command line.
7. **Schema Migration**  
   Built-in versioned migration system allows safe evolution of configuration structure across releases — handling field
   renames, type changes, and validation constraint changes without breaking existing user data.
8. **Change History & Undo**  
   Every configuration change is automatically recorded with a timestamp. You can browse the history via command and
   revert to any previous state.

## Requirements

- Java 11 or later

## Installation

To ensure stability, we recommend replacing `latest.release` with a specific version such as `0.16.0`.  
You can find the latest version on
the [ConfigLib Release Page](https://github.com/Maru32768/ConfigLib/releases).

ConfigLib also provides an optional annotation processor for compile-time checks of POJO annotations:

```kotlin
dependencies {
    annotationProcessor("com.github.Maru32768.ConfigLib:processor:latest.release")
}
```

The processor is a lint layer only. Runtime validation still runs without it.

<details>
  <summary>Bukkit (Groovy DSL)</summary>

```groovy
plugins {
    id "com.gradleup.shadow" version "8.3.5"
}

repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.Maru32768.ConfigLib:bukkit:latest.release'
}

shadowJar {
    archiveFileName = "${rootProject.name}-${project.version}.jar"
    relocate "net.kunmc.lab.commandlib", "${project.group}.${project.name.toLowerCase()}.commandlib"
    relocate "net.kunmc.lab.configlib", "${project.group}.${project.name.toLowerCase()}.configlib"
    relocate "com.google.gson", "${project.group}.${project.name.toLowerCase()}.gson"
    relocate "org.snakeyaml.engine", "${project.group}.${project.name.toLowerCase()}.snakeyaml.engine"
}
tasks.build.dependsOn tasks.shadowJar
```

</details>

<details>
  <summary>Bukkit (Kotlin DSL)</summary>

```kotlin
plugins {
    id("com.gradleup.shadow") version "8.3.5"
}

repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.Maru32768.ConfigLib:bukkit:latest.release")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveFileName.set("${rootProject.name}-${project.version}.jar")
    relocate("net.kunmc.lab.commandlib", "${project.group}.${project.name.lowercase()}.commandlib")
    relocate("net.kunmc.lab.configlib", "${project.group}.${project.name.lowercase()}.configlib")
    relocate("com.google.gson", "${project.group}.${project.name.lowercase()}.gson")
    relocate("org.snakeyaml.engine", "${project.group}.${project.name.lowercase()}.snakeyaml.engine")
}
tasks.named("build") { dependsOn(tasks.named("shadowJar")) }
```

</details>

<details>
  <summary>Forge (Groovy DSL)</summary>

```groovy
plugins {
    id "com.gradleup.shadow" version "8.3.5"
}

repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation "com.github.Maru32768.ConfigLib:forge:latest.release"
}

shadowJar {
    archiveFileName = "${rootProject.name}-${project.version}.jar"
    dependencies {
        include(dependency("com.github.Maru32768.CommandLib:forge:.*"))
        include(dependency("com.github.Maru32768.ConfigLib:forge:.*"))
        include(dependency("com.google.code.gson:gson:.*"))
        include(dependency("org.snakeyaml:snakeyaml-engine:.*"))
    }
    relocate "net.kunmc.lab.commandlib", "${project.group}.${project.name.toLowerCase()}.commandlib"
    relocate "net.kunmc.lab.configlib", "${project.group}.${project.name.toLowerCase()}.configlib"
    relocate "com.google.gson", "${project.group}.${project.name.toLowerCase()}.gson"
    relocate "org.snakeyaml.engine", "${project.group}.${project.name.toLowerCase()}.snakeyaml.engine"
    finalizedBy("reobfShadowJar")
}

reobf {
    shadowJar {
    }
}
```

</details>

<details>
  <summary>Forge (Kotlin DSL)</summary>

```kotlin
plugins {
    id("com.gradleup.shadow") version "8.3.5"
}

repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.Maru32768.ConfigLib:forge:latest.release")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveFileName.set("${rootProject.name}-${project.version}.jar")
    dependencies {
        include(dependency("com.github.Maru32768.CommandLib:forge:.*"))
        include(dependency("com.github.Maru32768.ConfigLib:forge:.*"))
        include(dependency("com.google.code.gson:gson:.*"))
        include(dependency("org.snakeyaml:snakeyaml-engine:.*"))
    }
    relocate("net.kunmc.lab.commandlib", "${project.group}.${project.name.lowercase()}.commandlib")
    relocate("net.kunmc.lab.configlib", "${project.group}.${project.name.lowercase()}.configlib")
    relocate("com.google.gson", "${project.group}.${project.name.lowercase()}.gson")
    relocate("org.snakeyaml.engine", "${project.group}.${project.name.lowercase()}.snakeyaml.engine")
    finalizedBy("reobfShadowJar")
}

reobf {
    create("shadowJar")
}
```

</details>

## Code Examples

<details>
<summary>POJO API — plain Java fields as configuration</summary>

Use plain fields instead of `Value` objects when you want a simpler, annotation-driven style.
Non-`static`, non-`transient` fields are automatically included. Use `transient` to exclude a field.

**Flat POJO config:**

```java
public final class ServerConfig extends BaseConfig {
    @Description("Maximum number of players.")
    @Range(min = 1, max = 100)
    public int maxPlayers = 20;

    @Description("Message shown on join.")
    public String motd = "Welcome!";

    @ConfigNullable
    public String adminContact = null;   // null allowed because of @ConfigNullable

    public transient RuntimeCache cache = new RuntimeCache(); // excluded from config

    public ServerConfig(Plugin plugin) {
        super(plugin);
        initialize();
    }
}
```

**Nested POJO (mutable inner class):**

```java
public final class PluginConfig extends BaseConfig {
    public ArenaSettings arena = new ArenaSettings();

    public PluginConfig(Plugin plugin) {
        super(plugin);
        initialize();
    }

    public static final class ArenaSettings {
        @Description("Maximum number of arenas.")
        @Range(min = 1, max = 50)
        public int maxArenas = 5;

        public String defaultName = "arena";
    }
}
```

Entries appear as `arena.maxArenas` and `arena.defaultName`.

**Nested immutable class (constructor/accessor model):**

```java
public final class PluginConfig extends BaseConfig {
    public ArenaSettings arena = new ArenaSettings(5, "arena");

    public PluginConfig(Plugin plugin) {
        super(plugin);
        initialize();
    }

    public static final class ArenaSettings {
        @Description("Maximum number of arenas.")
        @Range(min = 1, max = 50)
        private final int maxArenas;
        private final String defaultName;

        public ArenaSettings(int maxArenas, String defaultName) {
            this.maxArenas = maxArenas;
            this.defaultName = defaultName;
        }

        public int maxArenas() {
            return maxArenas;
        }

        public String defaultName() {
            return defaultName;
        }
    }
}
```

The all-args constructor is used to write values; field declaration order must match constructor parameter order.

**Nested record (Java 16+ runtime):**

```java
public final class PluginConfig extends BaseConfig {
    public ArenaSettings arena = new ArenaSettings(5, "arena");

    public PluginConfig(Plugin plugin) {
        super(plugin);
        initialize();
    }

    public record ArenaSettings(@Description("Maximum number of arenas.") @Range(min = 1, max = 50) int maxArenas,
                                String defaultName) {
    }
}
```

Nesting is supported to any depth. Records can be nested inside records; immutable classes can be nested inside
immutable classes. For records, annotations are written on record components and are handled like POJO field metadata.
Top-level custom object fields are treated as opaque leaf values rather than expanded POJO sections. Use nested static
classes or nested records for structured POJO config sections.

**Available annotations:**

| Annotation        | Target                  | Effect                                                |
|-------------------|-------------------------|-------------------------------------------------------|
| `@Description`    | POJO field              | Written as a YAML comment; shown on hover in commands |
| `@Range`          | numeric POJO leaf field | Validates `min ≤ value ≤ max` on load and command set |
| `@ConfigNullable` | POJO field              | Allows `null`; non-annotated fields reject `null`     |

**Generated commands for POJO fields:**

- All POJO fields are listed and can be read with `/config <field>`.
- Mutable `String`, `boolean`, `int`, `long`, `float`, `double`, `enum`, and boxed equivalents support scalar set
  commands:
  `/config <field> <value>` and `/config <field> set <value>`.
- Any schema entry, including mutable POJO scalar fields and nested POJO leaves such as `arena.maxArenas`, also
  supports `/config <field> reset`.
- `@Range` limits numeric command input as well as file load validation.
- Collection, map, object-valued leaf fields, and top-level `final` POJO fields are read-only in generated per-field
  commands. Nested POJO, immutable class, and record leaf fields are still modifiable when their leaf type is supported.
- POJO fields do not generate per-field `inc`, `dec`, `add`, `remove`, `clear`, or `put` commands. Use the Value API
  when those operations, custom tab-completion, custom command parsing, or command listeners are needed.
- The POJO scalar command type set is fixed in v1.0. For custom scalar parsing, Minecraft-specific types, or additional
  command behavior, define a custom `Value` type instead of extending POJO scalar support.

Value API and POJO fields can be mixed freely in the same config class.

</details>

<details>
<summary>Defining Configuration Classes (Value API)</summary>

Use the Value API when a config field needs command-aware behavior: custom command parsing, tab completion, display
formatting, validation, execution guards, listeners, arithmetic commands, collection commands, or map commands.

```java
public final class TestConfig extends BaseConfig {
    public final IntegerValue maxPlayers = new IntegerValue(20, 1, 100).description("Maximum players per arena.")
                                                                       .entryName("maxPlayers")
                                                                       .displayFormatter(v -> v + " players")
                                                                       .addValidator(v -> {
                                                                           if (v % 2 != 0) {
                                                                               throw new InvalidValueException(
                                                                                       "maxPlayers must be even.");
                                                                           }
                                                                       })
                                                                       .executableIf(ctx -> ctx.getSender()
                                                                                               .hasPermission(
                                                                                                       "myplugin.config"));

    public final StringValue welcomeMessage = new StringValue("Welcome!");

    public TestConfig(Plugin plugin) {
        super(plugin);
        initialize();
    }
}
```

`Value` API public surface for v1.0:

| Method                             | Status | Purpose                                                                                                   |
|------------------------------------|--------|-----------------------------------------------------------------------------------------------------------|
| `description(String)`              | Stable | User-facing description for generated commands and YAML comments                                          |
| `entryName(String)`                | Stable | Override the Java field name used in schema paths and generated commands                                  |
| `displayFormatter(Function)`       | Stable | Custom display string for list/get/history/diff/audit command output                                      |
| `nullable()`                       | Stable | Allow `null`; values are non-null by default unless constructed with a `null` default                     |
| `addValidator(Validator<E>)`       | Stable | Validate file load, command mutation, and accepted programmatic changes through the schema pipeline       |
| `executableIf(ExecutionCondition)` | Stable | Guard generated per-field command execution; throw `CommandPrerequisiteException` to block with a message |
| `executableIf(Predicate)`          | Stable | Boolean command guard with ConfigLib's default denial message                                             |
| `onInitialize(Consumer<E>)`        | Stable | Run after initial load; runs immediately if initialization has already completed                          |
| `onModify(Consumer<E>)`            | Stable | Run after accepted file reload, command mutation, or programmatic mutation                                |
| `onModify(listener, true)`         | Stable | Same as `onModify`, and also run after initialization                                                     |

`SingleValue` also keeps `disableModify()` and `onSet(...)` as command-specific APIs.
`CollectionValue` keeps `disableAdd/remove/clear` and `onAdd/remove/clear`.
`MapValue` keeps `disablePut/remove/clear` and `onPut/remove/clear`.

Customize generated command text, including operation success messages, with
`ConfigCommandBuilder#descriptionProvider(...)`. If one field needs special wording, branch on the message key and
the named `entry` argument in the provider.

</details>

<details>
<summary>Generating and Registering Configuration Commands</summary>

```java
public final class TestPlugin extends JavaPlugin {
    public void onEnable() {
        TestConfig testConfig = new TestConfig(this);
        Command root = new Command("test") {
        };

        // The generated tree includes:
        // /test config list
        // /test config reload
        // /test config reset
        // /test config history / undo / diff
        // /test config <field> ...
        root.addChildren(new ConfigCommandBuilder(testConfig).build());

        CommandLib.register(this, root);
    }
}
```

</details>

<details>
<summary>Registering Multiple Configurations to Commands</summary>

```java
public final class TestPlugin extends JavaPlugin {
    public void onEnable() {
        TestConfigA testConfigA = new TestConfigA(this);
        TestConfigB testConfigB = new TestConfigB(this);
        Command root = new Command("test") {
        };
        root.addChildren(new ConfigCommandBuilder(testConfigA).addConfig(testConfigB)
                                                              .build());

        CommandLib.register(this, root);
    }
}
```

</details>

<details>
<summary>Listening to Configuration Changes</summary>

```java
public final class TestConfig extends BaseConfig {
    public final IntegerValue integerValue = new IntegerValue(10).onModify(x -> {
        System.out.println("Changed integerValue to " + x);
    });

    public TestConfig(Plugin plugin) {
        super(plugin);
        initialize();
    }
}
```

</details>

<details>
<summary>Defining Custom Value Classes</summary>

In this section, we explain how to implement a custom `SingleValue` class and a custom `ListValue` class, based on the
following example class.

```java
// Represents a custom data structure with an integer and a string.
// Used as a value in the configuration.
class TestClass {
    private final int n;
    private final String s;

    public TestClass(int n, String s) {
        this.n = n;
        this.s = s;
    }

    @Override
    public String toString() {
        return "TestClass{" + "n=" + n + ", s='" + s + '\'' + '}';
    }
}
```

#### `SingleValue` Implementation

```java
import java.util.List;

// Custom SingleValue implementation for TestClass.
// Allows storing and manipulating a single instance of TestClass in configurations.
public final class TestClassValue extends SingleValue<TestClass, TestClassValue> {
    public TestClassValue(TestClass initialValue) {
        super(initialValue);
    }

    @Override
    protected List<ArgumentDefinition<TestClass>> argumentDefinitions() {
        List<ArgumentDefinition<TestClass>> res = new ArrayList<>();

        // Defines the arguments required to construct a TestClass instance.
        res.add(new ArgumentDefinition(new IntegerArgument("n"), new StringArgument("s"), (n, s, ctx) -> {
            // Converts command arguments to a TestClass instance.
            return new TestClass(n, s);
        }));

        return res;
    }

    // Converts a TestClass instance to its string representation.
    // This string will be used for command completion suggestions and for get/list command results.
    @Override
    protected String valueToString(TestClass testClass) {
        return testClass.toString();
    }
}
```

#### `ListValue` Implementation

```java
// Custom ListValue implementation for TestClass.
// Allows managing a list of TestClass instances in configurations.
public final class TestClassListValue extends ListValue<TestClass, TestClassListValue> {
    public TestClassListValue(List<TestClass> initialValue) {
        super(initialValue);
    }

    @Override
    protected List<ArgumentDefinition<List<TestClass>>> argumentDefinitionsForAdd() {
        List<ArgumentDefinition<List<TestClass>>> res = new ArrayList<>();

        // Defines the arguments required for the Add command.
        // These arguments will be used to construct a new TestClass instance and add it to the list.
        res.add(new ArgumentDefinition(new IntegerArgument("n"), new StringArgument("s"), (n, s, ctx) -> {
            // Converts command arguments to a new TestClass instance and adds it to the list.
            return Collections.singletonList(new TestClass(n, s));
        }));

        return res;
    }

    @Override
    protected List<ArgumentDefinition<List<TestClass>>> argumentDefinitionsForRemove() {
        List<ArgumentDefinition<List<TestClass>>> res = new ArrayList<>();

        // Defines the arguments required for the Remove command.
        // These arguments will be used to identify which TestClass instance to remove from the list.
        res.add(new ArgumentDefinition(new StringArgument("target", opt -> {
            opt.suggestionAction(sb -> {
                for (TestClass v : value) {
                    sb.suggest(v.toString());
                }
            });
        }, StringArgument.Type.PHRASE_QUOTED), (input, ctx) -> {
            // Finds a TestClass instance to remove based on user input.
            TestClass target = value.stream()
                                    .filter(x -> x.toString()
                                                  .equals(input))
                                    .findFirst()
                                    .orElseThrow(() -> new InvalidArgumentException(input + " is invalid"));
            return Collections.singletonList(target);
        }));

        return res;
    }

    // Converts a TestClass instance to its string representation.
    // This string will be used for command completion suggestions and for get/list command results.
    @Override
    protected String elementToString(TestClass testClass) {
        return testClass.toString();
    }
}
```

These custom Value classes can be used in the same way as built-in Value classes.

```java
public final class TestConfig extends BaseConfig {
    public final TestClassValue testClassValue = new TestClassValue(null);
    public final TestClassListValue testClassListValue = new TestClassListValue(new ArrayList<>());

    public TestConfig(Plugin plugin) {
        super(plugin);
        initialize();
    }
}
```

</details>

<details>
<summary>Schema Migration</summary>

ConfigLib provides a versioned migration system to evolve your configuration schema safely across releases.
Migrations are registered via `Option#migrateTo(...)` and run automatically when an older config file is loaded.
The current version is stored as `_version_` in the config file.

```java
public final class MyConfig extends BaseConfig {
    public final StringValue message = new StringValue("hello");
    public final EnumSetValue<EntityType> spawnTypes = new EnumSetValue<>(EntityType.class);

    public MyConfig(Plugin plugin) {
        super(plugin, opt -> opt
                // Version 1: rename field
                .migrateTo(1, migration -> migration.rename("msg", "message"))
                // Version 2: type change (number -> string)
                .migrateTo(2,
                           migration -> migration.convert("cooldown",
                                                          Number.class,
                                                          String.class,
                                                          value -> String.valueOf(value.intValue())))
                // Version 3: add a missing nested value
                .migrateTo(3, migration -> migration.defaultValue("limits.maxPlayers", 20)));
        initialize();
    }
}
```

`MigrationDsl` provides the following operations:

| Method                                      | Description                                                             |
|---------------------------------------------|-------------------------------------------------------------------------|
| `rename(from, to)`                          | Rename a field in place                                                 |
| `move(from, to)`                            | Move a field to another path                                            |
| `delete(path)`                              | Remove a field                                                          |
| `set(path, value)`                          | Overwrite a value                                                       |
| `defaultValue(path, value)`                 | Set a value only when the path is currently missing                     |
| `convert(path, sourceType, targetType, fn)` | Read the current value as `sourceType`, transform it, and write it back |

Migrations only run on existing files that have an older version. New installations start at the latest version and
skip all migrations.

Paths can be nested using dot notation such as `limits.maxPlayers`.

**Adding new fields:**

When a config class gains a new field, existing config files usually do not contain that key yet. ConfigLib keeps the
field's Java-side default value for missing keys during load, so adding a field does not require a migration by itself.

This only applies to missing keys. If a config file explicitly contains `null`, the loaded `null` is validated normally:
POJO fields require `@ConfigNullable`. Value fields are non-null by default; call `.nullable()` or construct the value
with a `null` default to allow `null`.

Use a migration when an existing key must be renamed, moved, removed, converted to another type, or changed to a
different value based on old file contents.

</details>

## Usage Notes

1. Calling `initialize()`  
   `initialize()` must be called at the end of the concrete config class constructor, after all fields have been
   assigned. This ensures that the initial config load and file watching start only after the subclass is fully
   constructed.
    ```java
    public final class TestConfig extends BaseConfig {
        public final IntegerValue integerValue = new IntegerValue(10);

        public TestConfig(Plugin plugin) {
            super(plugin);
            initialize(); // Must be the last statement
        }
    }
    ```
2. Asynchronous Change Detection  
   Change detection, including when modifying values with the `set` method, is handled asynchronously. Keep this in mind
   to avoid race conditions in your application logic.

## Generated Commands

`ConfigCommandBuilder` automatically generates a command tree from your config classes.
The examples below use `/config` as the root command name (set via `.name("config")`).

<details>
<summary>Config-level subcommands</summary>

These subcommands operate on the config as a whole.

| Command          | Description                              |
|------------------|------------------------------------------|
| `/config list`   | Show all field values                    |
| `/config reload` | Reload from file                         |
| `/config reset`  | Reset all fields to their default values |

These commands are generated when the config has at least one schema entry. That includes POJO-only configs.

Use `.disableList()` / `.disableReload()` / `.disableReset()` on `ConfigCommandBuilder` to suppress any of these.

**With multiple configs**, each subcommand also accepts a config name:

| Command                       | Description                         |
|-------------------------------|-------------------------------------|
| `/config list <configName>`   | List fields for that config         |
| `/config reload <configName>` | Reload that config                  |
| `/config reset <configName>`  | Reset that config                   |
| `/config <configName>`        | List fields for that config (alias) |

</details>

<details>
<summary>Per-field subcommands</summary>

These subcommands are generated for config fields.
For Value fields, `<field>` is the Java field name or the name set via `.entryName()`.
For POJO fields, nested entries use dotted paths such as `arena.maxArenas`.

**Get**

| Command                        | Description                                       |
|--------------------------------|---------------------------------------------------|
| `/config <field>`              | Show the current value (hover for description)    |
| `/config <configName>.<field>` | Same, always available even with multiple configs |

**SingleValue — set**

| Command                       | Description                |
|-------------------------------|----------------------------|
| `/config <field> <value>`     | Set the value (shorthand)  |
| `/config <field> set <value>` | Set the value              |
| `/config <field> reset`       | Reset to the default value |

**POJO field — set**

Mutable `String`, `boolean`, `int`, `long`, `float`, `double`, `enum`, and boxed equivalents support scalar set
commands.
Collection, map, object-valued leaf fields, and top-level `final` POJO fields are get-only. Nested POJO, immutable
class, and record leaf fields are still modifiable when their leaf type is supported.

| Command                       | Description                |
|-------------------------------|----------------------------|
| `/config <field> <value>`     | Set the POJO field value   |
| `/config <field> set <value>` | Set the POJO field value   |
| `/config <field> reset`       | Reset to the default value |

**NumericValue — arithmetic** (IntegerValue, DoubleValue, FloatValue) — extends SingleValue, so `set` and `reset` also
apply

| Command                        | Description                          |
|--------------------------------|--------------------------------------|
| `/config <field> inc`          | Increment by 1                       |
| `/config <field> inc <amount>` | Increment by amount (capped at max)  |
| `/config <field> dec`          | Decrement by 1                       |
| `/config <field> dec <amount>` | Decrement by amount (floored at min) |

**CollectionValue — add / remove / clear** (ListValue, SetValue)

| Command                            | Description                |
|------------------------------------|----------------------------|
| `/config <field> add <element>`    | Add an element             |
| `/config <field> remove <element>` | Remove an element          |
| `/config <field> clear`            | Remove all elements        |
| `/config <field> reset`            | Reset to the default value |

Disable individual operations with `.disableAdd()` / `.disableRemove()` / `.disableClear()`.

**MapValue — put / remove / clear**

| Command                             | Description                |
|-------------------------------------|----------------------------|
| `/config <field> put <key> <value>` | Add or update an entry     |
| `/config <field> remove <key>`      | Remove an entry            |
| `/config <field> clear`             | Remove all entries         |
| `/config <field> reset`             | Reset to the default value |

Disable individual operations with `.disablePut()` / `.disableRemove()` / `.disableClear()`.

Use `.disableGet()` / `.disableModify()` on `ConfigCommandBuilder` to suppress get or modify commands globally.

</details>

<details>
<summary>Change history, undo, and diff</summary>

Every time a configuration value is modified, the new state is automatically saved to a history file
(`<configName>.history.yml` by default) alongside the config file. The history persists across server restarts and is
capped at
50 entries by default (override `createConfigStore()` to change this).

Audit entries are stored separately in `<configName>.audit.yml` / `.json`.

Each history entry also records its source:

- `INITIAL`: initial snapshot for a brand-new config
- `MIGRATION`: initial snapshot created immediately after applying migrations on load
- `COMMAND`: generated command changes
- `FILE`: file reload changes
- `PROGRAMMATIC`: explicit programmatic mutations
- `UNDO`: undo operations

History uses **0-based indexing** where `[0]` is the current (latest) state.

**Single config**

| Command                                  | Description                                                |
|------------------------------------------|------------------------------------------------------------|
| `/config history`                        | List all entries with timestamps (hover to preview values) |
| `/config history <index>`                | Show field values at that index                            |
| `/config history diff <index>`           | Diff current state vs history entry                        |
| `/config history diff <index1> <index2>` | Diff between two history entries                           |
| `/config history undo`                   | Restore `history[1]`                                       |
| `/config history undo <index>`           | Restore `history[index]`                                   |
| `/config undo`                           | Restore `history[1]`                                       |
| `/config undo <index>`                   | Restore `history[index]`                                   |
| `/config diff default`                   | Diff current state vs declared default values              |
| `/config diff <index>`                   | Diff current state vs history entry                        |
| `/config diff <index1> <index2>`         | Diff between two history entries                           |
| `/config audit`                          | List audit entries                                         |
| `/config audit <index>`                  | Show one audit entry with change details                   |

**Multiple configs** — prefix with the config name using either order:

| Command                                     | Description                          |
|---------------------------------------------|--------------------------------------|
| `/config history <configName>`              | List entries for that config         |
| `/config history <configName> <index>`      | Show field values at that index      |
| `/config history <configName> diff <index>` | Diff for that config                 |
| `/config history <configName> undo [index]` | Undo for that config                 |
| `/config undo <configName> [index]`         | Undo for that config                 |
| `/config diff <configName> default`         | Diff for that config vs defaults     |
| `/config diff <configName> <index>`         | Diff for that config                 |
| `/config audit <configName>`                | List audit entries for that config   |
| `/config audit <configName> <index>`        | Show one audit entry for that config |
| `/config <configName> history [index]`      | Alternative prefix order             |
| `/config <configName> undo [index]`         | Alternative prefix order             |
| `/config <configName> diff default`         | Alternative prefix order             |
| `/config <configName> diff <index>`         | Alternative prefix order             |
| `/config <configName> audit [index]`        | Alternative prefix order             |

`diff` shows only fields that differ, formatted as `fieldName: <old> → <new>`.

`diff default` compares the current in-memory config against the defaults declared in Java code.
Diff output is formatted as `fieldName: <old> -> <new>`.

Audit entries are stored separately from history snapshots in `<configName>.audit.yml` / `.json`.
They record the accepted change event with timestamp, source, actor, optional reason, changed paths, and before/after
display text for each changed field.

Use `@Masked` on a config field to mask its value in command-oriented output such as list/get/history/diff/audit. The
stored config file and history snapshots remain unmasked so they can still be restored exactly. Masked values are
revealed by default only to console senders, operators, or senders with `configlib.masked.reveal`.

You can override the reveal rule for generated commands:

```java
public final class TestPlugin extends JavaPlugin {
    public void onEnable() {
        new ConfigCommandBuilder(config).maskedRevealPolicy((ctx, cfg, entry) -> ctx.getActor()
                                                                                    .isConsole() || ctx.getActor()
                                                                                                       .hasPermission(
                                                                                                               "myplugin.config.secret.view"));
    }
}
```

To hide history commands from the generated command tree:

```java
public final class MyConfig extends BaseConfig {
    public MyConfig(Plugin plugin) {
        super(plugin);
        disableHistory();
        initialize();
    }
    // ...
}
```

Use `.disableHistory()` on `ConfigCommandBuilder` to suppress history commands globally.

</details>

## Claude Code Integration

If you use [Claude Code](https://claude.ai/code), a `/configlib` skill is available to help you write ConfigLib code.
It loads the API into context and provides usage conventions tailored to library consumers.

**Setup:** Copy `.claude/skills/configlib/` from this repository into your project's `.claude/skills/` directory.

**Usage:**

```
/configlib How do I define a config with an IntegerValue and a StringValue?
/configlib Write a custom SingleValue for org.bukkit.World
```

## Sample Projects

[Bukkit](./sample/bukkit/src/main/java/net/kunmc/lab/sampleplugin)  
[Forge](./sample/forge/src/main/java/net/kunmc/lab/samplemod)

## License

ConfigLib is licensed under the MIT License.

Spigot, Paper, Bukkit, Minecraft, Brigadier, Forge, and other platform APIs or runtimes are separate projects and may
be governed by their own license terms. They are not bundled with ConfigLib unless explicitly stated otherwise.
