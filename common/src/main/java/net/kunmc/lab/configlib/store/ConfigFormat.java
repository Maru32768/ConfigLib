package net.kunmc.lab.configlib.store;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kunmc.lab.configlib.schema.ConfigSchema;
import org.jetbrains.annotations.Nullable;

public interface ConfigFormat {
    String extension();

    JsonElement parse(String content);

    default JsonObject parseObject(String content) {
        JsonElement element = parse(content);
        if (element == null || element.isJsonNull()) {
            return new JsonObject();
        }
        if (!element.isJsonObject()) {
            throw new InvalidConfigFormatException("Config root must be an object.");
        }
        return element.getAsJsonObject();
    }

    default boolean wrapsHistory() {
        return false;
    }

    String write(JsonElement element, @Nullable ConfigSchema schema);
}
