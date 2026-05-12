---
description:
  Generate, test, or explain code that uses the ConfigLib library in a Spigot, Paper, or Forge project. Usage: /configlib <request>
allowed-tools: Bash Read Glob Grep
---

## Purpose

Use this skill when the user is writing a plugin, mod, or library that depends
on ConfigLib.

Do not use this skill for maintaining the ConfigLib repository itself unless the
user explicitly asks for downstream usage examples.

## Step 1 - Read downstream usage references

Read and apply the conventions in `.claude/skills/configlib/references/conventions.md`.

## Step 2 - Extract ConfigLib sources when needed

Run the extraction script:

```bash
bash .claude/skills/configlib/scripts/extract-sources.sh
```

The script prints one of:

- A JAR path: sources were extracted to `/tmp/configlib-sources/`
- `LOCAL_BUILD`: ConfigLib source tree is available at the project root

## Step 3 - Read key source files if API detail is needed

If extracted JAR, use `/tmp/configlib-sources/` as the base path. If
`LOCAL_BUILD`, use the project root as the base path.

Read only what the request needs. Common public API files include:

- `README.md`
- `common/src/main/java/net/kunmc/lab/configlib/CommonBaseConfig.java`
- `common/src/main/java/net/kunmc/lab/configlib/ConfigKeys.java`
- `common/src/main/java/net/kunmc/lab/configlib/migration/MigrationDsl.java`
- `common/src/main/java/net/kunmc/lab/configlib/migration/Migrations.java`
- `spigot/src/main/java/net/kunmc/lab/configlib/BaseConfig.java`
- `paper/src/main/java/net/kunmc/lab/configlib/BaseConfig.java`
- `common/src/main/java/net/kunmc/lab/configlib/Value.java`
- `common/src/main/java/net/kunmc/lab/configlib/SingleValue.java`
- `common/src/main/java/net/kunmc/lab/configlib/CollectionValue.java`
- `common/src/main/java/net/kunmc/lab/configlib/MapValue.java`
- `common/src/main/java/net/kunmc/lab/configlib/NumericValue.java`
- `common/src/main/java/net/kunmc/lab/configlib/ConfigCommandBuilder.java`
- `common/src/main/java/net/kunmc/lab/configlib/store/ConfigStore.java`
- `common/src/main/java/net/kunmc/lab/configlib/store/FileConfigStore.java`
- `common/src/main/java/net/kunmc/lab/configlib/store/YamlFileConfigStore.java`
- `common/src/main/java/net/kunmc/lab/configlib/store/HistoryEntry.java`
- `common/src/main/java/net/kunmc/lab/configlib/store/AuditEntry.java`
- `common/src/main/java/net/kunmc/lab/configlib/store/ChangeTrace.java`
- `common/src/main/java/net/kunmc/lab/configlib/value/`
- `spigot/src/main/java/net/kunmc/lab/configlib/value/`
- `paper/src/main/java/net/kunmc/lab/configlib/value/`
- `common/src/main/java/net/kunmc/lab/configlib/annotation/` (field annotations; prefer each annotation's Javadoc for
  exact applicability)

For extracted JAR sources, map those paths to the extracted package layout.

## Step 4 - Respond to the request

Respond in the same language as the user's request. If asked to generate code,
output a complete working implementation that follows the public ConfigLib API.
If asked how to do something, explain with a code example.

$ARGUMENTS
