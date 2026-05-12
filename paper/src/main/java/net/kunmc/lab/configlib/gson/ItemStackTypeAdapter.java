package net.kunmc.lab.configlib.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ItemStackTypeAdapter extends TypeAdapter<ItemStack> {
    private static final Gson GSON = new Gson();

    @Override
    public void write(JsonWriter out, ItemStack itemStack) throws IOException {
        Map<String, Object> map = itemStack.serialize();
        map.computeIfPresent("meta", (k, v) -> {
            Map<String, Object> meta = new HashMap<>(((ItemMeta) v).serialize());
            meta.put("==", "ItemMeta");
            return meta;
        });

        out.value(GSON.toJson(map));
    }

    @Override
    public ItemStack read(JsonReader in) throws IOException {
        Map<String, Object> map = GSON.fromJson(in.nextString(), new TypeToken<Map<String, Object>>() {
        }.getType());
        map.computeIfPresent("meta", (k, v) -> ConfigurationSerialization.deserializeObject(((Map<String, Object>) v)));

        return ItemStack.deserialize(map);
    }
}
