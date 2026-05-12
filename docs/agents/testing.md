# Testing Notes

Use focused Gradle tasks for the modules affected by the change.

Common checks:

```bash
./gradlew :common:test
./gradlew :spigot:test
./gradlew :paper:test
./gradlew :forge:test
```

Compile checks:

```bash
./gradlew :common:compileJava
./gradlew :spigot:compileJava
./gradlew :paper:compileJava
./gradlew :forge:compileJava
```

For cross-module behavior, run the relevant combined checks:

```bash
./gradlew test
./gradlew build
```

Gradle daemon is disabled in `gradle.properties`. The project targets Java 11.

When changing generated command behavior, add or update focused tests around
command building, get/list/modify/reload/reset/history/undo/diff behavior,
validation, permissions, and multi-config command trees.

When changing serialization or value behavior, cover load, save, validation,
formatting, command modification, file reload, and programmatic `value(...)`
updates where relevant.
