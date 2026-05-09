package net.kunmc.lab.configlib.schema;

import net.kunmc.lab.configlib.CommonBaseConfig;
import net.kunmc.lab.configlib.Value;
import net.kunmc.lab.configlib.ValueConfigSchemaEntry;
import net.kunmc.lab.configlib.store.ConfigStore;
import net.kunmc.lab.configlib.util.ConfigUtil;
import net.kunmc.lab.configlib.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Normalized description of a ConfigLib config class.
 * <p>
 * A schema is the shared read model used by formats, validation, generated commands,
 * history, diff, undo, and audit features. It intentionally hides whether an entry
 * came from the Value API or the POJO API.
 * </p>
 * <p>
 * Public consumers may inspect entries, look up entries by dotted path, and read
 * metadata. Constructing schemas from arbitrary objects is an internal concern of
 * ConfigLib and currently happens through {@link #fromConfig(CommonBaseConfig)}.
 * </p>
 */
public final class ConfigSchema {
    private final List<ConfigSchemaEntry<?>> entries;
    private final Map<String, String> descriptionsByPath;

    private ConfigSchema(List<ConfigSchemaEntry<?>> entries, Map<String, String> descriptionsByPath) {
        this.entries = List.copyOf(entries);
        this.descriptionsByPath = Map.copyOf(descriptionsByPath);
    }

    public static ConfigSchema fromConfig(CommonBaseConfig config) {
        List<ConfigSchemaEntry<?>> entries = new ArrayList<>();
        Map<String, String> descriptionsByPath = new LinkedHashMap<>();
        for (Field field : ReflectionUtil.getFieldsIncludingSuperclasses(config.getClass())) {
            if (!ConfigUtil.isObservableField(config, field)) {
                continue;
            }
            if (ConfigStore.class.isAssignableFrom(field.getType())) {
                continue;
            }
            field.setAccessible(true);
            try {
                if (Value.class.isAssignableFrom(field.getType())) {
                    Value<?, ?> value = (Value<?, ?>) field.get(config);
                    entries.add(ValueConfigSchemaEntry.from(field, value));
                    addDescription(descriptionsByPath, field.getName(), value.description());
                } else if (isNestedPojoType(field.getType())) {
                    addDescription(descriptionsByPath, field.getName(), PojoAnnotations.descriptionOf(field));
                    collectNestedEntries(config, new Field[]{field}, field.getType(), entries, descriptionsByPath);
                } else {
                    entries.add(PojoConfigSchemaEntry.from(config, new Field[0], field));
                    addDescription(descriptionsByPath, field.getName(), PojoAnnotations.descriptionOf(field));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return new ConfigSchema(entries, descriptionsByPath);
    }

    private static void collectNestedEntries(Object root,
                                             Field[] parentChain,
                                             Class<?> pojoClass,
                                             List<ConfigSchemaEntry<?>> result,
                                             Map<String, String> descriptionsByPath) {
        for (Field field : ReflectionUtil.getFieldsIncludingSuperclasses(pojoClass)) {
            if (field.getDeclaringClass() == Object.class) {
                continue;
            }
            if (field.isSynthetic()) {
                continue;
            }
            if (!ConfigUtil.isConfigFieldModifier(field)) {
                continue;
            }
            field.setAccessible(true);
            addDescription(descriptionsByPath, buildPath(parentChain, field), PojoAnnotations.descriptionOf(field));
            if (isNestedPojoType(field.getType())) {
                Field[] newChain = appendToChain(parentChain, field);
                collectNestedEntries(root, newChain, field.getType(), result, descriptionsByPath);
            } else {
                result.add(PojoConfigSchemaEntry.from(root, parentChain, field));
            }
        }
    }

    private static String buildPath(Field[] parentChain, Field leafField) {
        StringBuilder sb = new StringBuilder();
        for (Field field : parentChain) {
            sb.append(field.getName())
              .append('.');
        }
        sb.append(leafField.getName());
        return sb.toString();
    }

    private static void addDescription(Map<String, String> descriptionsByPath, String path, String description) {
        if (description != null) {
            descriptionsByPath.put(path, description);
        }
    }

    private static Field[] appendToChain(Field[] chain, Field field) {
        Field[] newChain = new Field[chain.length + 1];
        System.arraycopy(chain, 0, newChain, 0, chain.length);
        newChain[chain.length] = field;
        return newChain;
    }

    // Only expand classes explicitly designed as config POJOs (i.e. nested/inner classes).
    // Top-level custom/library types are treated as opaque leaf values. This keeps the
    // v1.0 POJO API bounded: structured POJO sections are nested classes/records, while
    // custom scalar parsing and Minecraft-specific command behavior belong in Value types.
    private static boolean isNestedPojoType(Class<?> type) {
        if (type.getEnclosingClass() == null) {
            return false;
        }
        if (type.isEnum()) {
            return false;
        }
        if (type.isAnnotation()) {
            return false;
        }
        if (Value.class.isAssignableFrom(type)) {
            return false;
        }
        return true;
    }

    public List<ConfigSchemaEntry<?>> entries() {
        return List.copyOf(entries);
    }

    public Map<String, String> descriptionsByPath() {
        return Map.copyOf(descriptionsByPath);
    }

    public Optional<ConfigSchemaEntry<?>> findEntry(String path) {
        return entries.stream()
                      .filter(e -> e.path()
                                    .asString()
                                    .equals(path))
                      .findFirst();
    }
}
