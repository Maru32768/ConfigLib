package net.kunmc.lab.configlib.store;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kunmc.lab.configlib.ConfigKeys;
import net.kunmc.lab.configlib.schema.ConfigSchema;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Strategy for handling keys that are not part of the current config schema.
 */
public interface UnknownKeyPolicy {
    /**
     * Keeps unknown keys when reading and writing.
     */
    UnknownKeyPolicy PRESERVE = new UnknownKeyPolicy() {
        @Override
        public void apply(JsonObject object, ConfigSchema schema) {
        }

        @Override
        public void beforeWrite(JsonObject target, JsonObject stored, ConfigSchema schema) {
            copyUnknownKeys(target, stored, schema, path -> true);
        }
    };

    /**
     * Removes unknown keys from the next written config.
     */
    UnknownKeyPolicy REMOVE = new UnknownKeyPolicy() {
        @Override
        public void apply(JsonObject object, ConfigSchema schema) {
            filterUnknownKeys(object, schema, path -> false);
        }
    };

    /**
     * Rejects configs that contain unknown keys.
     */
    UnknownKeyPolicy FAIL = (object, schema) -> {
        filterUnknownKeys(object, schema, path -> {
            throw new UnknownConfigKeyException(path);
        });
    };

    /**
     * Creates a policy that keeps unknown keys matching the predicate and removes the rest.
     *
     * @param predicate receives a dot-separated config path
     * @return unknown key policy
     */
    static UnknownKeyPolicy filter(Predicate<String> predicate) {
        return new UnknownKeyPolicy() {
            @Override
            public void apply(JsonObject object, ConfigSchema schema) {
                filterUnknownKeys(object, schema, predicate);
            }

            @Override
            public void beforeWrite(JsonObject target, JsonObject stored, ConfigSchema schema) {
                copyUnknownKeys(target, stored, schema, predicate);
            }
        };
    }

    /**
     * Applies this policy to loaded config data.
     */
    void apply(JsonObject object, ConfigSchema schema);

    /**
     * Applies this policy while writing, allowing unknown keys from existing stored data to be copied.
     */
    default void beforeWrite(JsonObject target, JsonObject stored, ConfigSchema schema) {
    }

    private static void filterUnknownKeys(JsonObject object, ConfigSchema schema, Predicate<String> keep) {
        Set<String> leafPaths = leafPaths(schema);
        filterUnknownKeys(object, leafPaths, branchPaths(leafPaths), "", keep);
    }

    private static void filterUnknownKeys(JsonObject object,
                                          Set<String> leafPaths,
                                          Set<String> branchPaths,
                                          String prefix,
                                          Predicate<String> keep) {
        Iterator<Map.Entry<String, JsonElement>> iterator = object.entrySet()
                                                                  .iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonElement> entry = iterator.next();
            String key = entry.getKey();
            if (prefix.isEmpty() && ConfigKeys.VERSION.equals(key)) {
                continue;
            }

            String path = prefix.isEmpty() ? key : prefix + "." + key;
            if (!leafPaths.contains(path) && !branchPaths.contains(path)) {
                if (!keep.test(path)) {
                    iterator.remove();
                }
                continue;
            }

            if (!leafPaths.contains(path) && entry.getValue()
                                                  .isJsonObject()) {
                filterUnknownKeys(entry.getValue()
                                       .getAsJsonObject(), leafPaths, branchPaths, path, keep);
            }
        }
    }

    private static void copyUnknownKeys(JsonObject target,
                                        JsonObject stored,
                                        ConfigSchema schema,
                                        Predicate<String> keep) {
        Set<String> leafPaths = leafPaths(schema);
        copyUnknownKeys(target, stored, leafPaths, branchPaths(leafPaths), "", keep);
    }

    private static void copyUnknownKeys(JsonObject target,
                                        JsonObject stored,
                                        Set<String> leafPaths,
                                        Set<String> branchPaths,
                                        String prefix,
                                        Predicate<String> keep) {
        for (Map.Entry<String, JsonElement> entry : stored.entrySet()) {
            String key = entry.getKey();
            if (prefix.isEmpty() && ConfigKeys.VERSION.equals(key)) {
                continue;
            }

            String path = prefix.isEmpty() ? key : prefix + "." + key;
            if (!leafPaths.contains(path) && !branchPaths.contains(path)) {
                if (!target.has(key) && keep.test(path)) {
                    target.add(key,
                               entry.getValue()
                                    .deepCopy());
                }
                continue;
            }

            if (!leafPaths.contains(path) && entry.getValue()
                                                  .isJsonObject() && target.has(key) && target.get(key)
                                                                                              .isJsonObject()) {
                copyUnknownKeys(target.get(key)
                                      .getAsJsonObject(),
                                entry.getValue()
                                     .getAsJsonObject(),
                                leafPaths,
                                branchPaths,
                                path,
                                keep);
            }
        }
    }

    private static Set<String> leafPaths(ConfigSchema schema) {
        Set<String> result = new HashSet<>();
        schema.entries()
              .forEach(entry -> result.add(entry.path()
                                                .asString()));
        return result;
    }

    private static Set<String> branchPaths(Set<String> leafPaths) {
        Set<String> result = new HashSet<>();
        for (String path : leafPaths) {
            int index = path.indexOf('.');
            while (index > 0) {
                result.add(path.substring(0, index));
                index = path.indexOf('.', index + 1);
            }
        }
        return result;
    }
}
