package net.kunmc.lab.configlib.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;

import java.io.IOException;

public class BlockDataTypeAdapter extends TypeAdapter<BlockData> {
    @Override
    public void write(JsonWriter out, BlockData value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.getAsString());
        }
    }

    @Override
    public BlockData read(JsonReader in) throws IOException {
        String s = in.nextString();

        if (s.equals("null")) {
            return null;
        }

        BlockData b;
        try {
            b = Bukkit.createBlockData(s);
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }

        return b;
    }
}
