# ConfigLib Sample Projects

This directory contains small, buildable sample projects for the supported
platforms.

- [Spigot](./spigot)
- [Paper](./paper)
- [Forge](./forge)

When these samples are built from this repository, Gradle uses `includeBuild`
and dependency substitution so the samples compile against the local ConfigLib
source and the sibling CommandLib source. When copied outside the repository,
they resolve `com.github.Maru32768.ConfigLib:*:latest.release` and
`com.github.Maru32768.CommandLib:*:latest.release` from the configured
repositories.

Expected local checkout layout:

```text
IdeaProjects/
  CommandLib/
  ConfigLib/
```

## What The Samples Show

Each platform sample registers generated ConfigLib commands under `/test config`.

| Area                        | Demonstrates                                                                 |
|-----------------------------|------------------------------------------------------------------------------|
| Value API                   | Typed `Value` fields with bounds, descriptions, platform values, maps, sets. |
| POJO API                    | Plain fields with `@Description`, `@Range`, `@ConfigNullable`, `@Masked`.    |
| YAML output                 | Default YAML file generation and description comments.                       |
| Generated commands          | list, reload, reset, per-field get/set, history, undo, diff, and audit.      |
| Multiple configs            | Forge registers common, server, and client configs when available.           |
| Shaded plugin/mod packaging | Shadow relocation for ConfigLib, CommandLib, Gson, and SnakeYAML Engine.     |

## Build

Build a sample from its platform directory:

```bash
cd samples/spigot
./gradlew build
```

```bash
cd samples/paper
./gradlew build
```

```bash
cd samples/forge
./gradlew build
```

The sample code is intentionally small and meant to show public API usage rather
than complete plugin or mod behavior. Behavioral coverage belongs in the test
modules.
