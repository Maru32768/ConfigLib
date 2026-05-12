# ConfigLib Repository Notes

## Scope

This document is for agents editing the ConfigLib repository itself. The Claude
Code skills under `.claude/skills` are for downstream library users and should
not be used as the main source of repository maintenance instructions.

## Project Overview

ConfigLib is a type-safe configuration management library for Minecraft servers
on Bukkit/Paper and Forge. It provides reflection-based config field discovery,
Gson-based JSON serialization, file watching and auto-reload, validation, and
auto-generated commands through CommandLib integration.

## Project Structure

- `common`: platform-agnostic core, including `CommonBaseConfig`, all generic
  `Value` types, command generation, Gson adapters, history, migrations, and
  utilities.
- `spigot`: Spigot integration, platform `BaseConfig`, Bukkit API values, and
  Bukkit Gson adapters.
- `paper`: Paper integration, platform `BaseConfig`, Bukkit/Paper API values,
  and Bukkit Gson adapters.
- `forge`: Forge integration, platform `BaseConfig`, Forge-specific values, and
  adapters.
- `samples/spigot`: sample Spigot plugin usage.
- `samples/paper`: sample Paper plugin usage.
- `samples/forge`: sample Forge mod usage.
- `spigot/test-plugin`: internal Spigot test plugin.

All source lives under `net.kunmc.lab.configlib`.

## Architecture Notes

Config fields are declared as `public final` `Value` subclass instances. The
base hierarchy is:

```text
Value<E, T>
|-- SingleValue<E, T>
|   `-- NumericValue<E, T>
|-- CollectionValue<T, E, U>
|   |-- ListValue<E, U>
|   `-- SetValue<E, U>
`-- MapValue<K, V, T>
```

`ConfigUtil` and `ReflectionUtil` discover `Value` fields by reflection.
`CommonBaseConfig` owns the core lifecycle: registration, asynchronous initial
load, file watching, modification detection, synchronized save/load, history,
and migrations.

Platform `BaseConfig` classes handle plugin or mod lifecycle integration,
config directory resolution, and platform-specific Gson adapters.

## Implementation Guidance

- Prefer the existing module boundaries. Keep platform-independent behavior in
  `common`; keep Bukkit or Forge API usage in the matching platform module.
- Follow the public API style already used in the surrounding code.
- Keep changes scoped to the behavior being fixed or added.
- Avoid broad formatting-only edits.
- Preserve binary/source compatibility unless the task explicitly calls for an
  API break.
- When adding a new value type, add the value class, serialization support when
  needed, and command modification support when the value should be editable by
  generated commands.
- When changing config lifecycle behavior, consider async initialization, file
  watching, modification detection, command modification, history, undo, and
  migration interactions.

## Public Usage Patterns

When writing examples or tests that represent downstream user code, prefer the
patterns documented in `.claude/skills/configlib/references/conventions.md`.
Those patterns are user-facing API guidance, not repository architecture rules.
