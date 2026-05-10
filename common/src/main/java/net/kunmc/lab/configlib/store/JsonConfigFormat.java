package net.kunmc.lab.configlib.store;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kunmc.lab.configlib.schema.ConfigSchema;
import org.jetbrains.annotations.Nullable;

public final class JsonConfigFormat implements ConfigFormat {
    private final Gson gson;

    public JsonConfigFormat(Gson gson) {
        this.gson = gson;
    }

    @Override
    public String extension() {
        return "json";
    }

    @Override
    public JsonElement parse(String content) {
        if (content == null || content.trim()
                                      .isEmpty()) {
            return new JsonObject();
        }
        return JsonParser.parseString(content);
    }

    @Override
    public String write(JsonElement element, @Nullable ConfigSchema schema) {
        return gson.toJson(element);
    }
}
