package net.kunmc.lab.configlib.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.bukkit.Location;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LocationTypeAdapter extends TypeAdapter<Location> {
    @Override
    public void write(JsonWriter out, Location value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.beginObject();
            out.name("world")
               .value(value.getWorld()
                           .getName());
            out.name("x")
               .value(value.getX());
            out.name("y")
               .value(value.getY());
            out.name("z")
               .value(value.getZ());
            out.name("pitch")
               .value(value.getPitch());
            out.name("yaw")
               .value(value.getYaw());
            out.endObject();
        }
    }

    @Override
    public Location read(JsonReader in) throws IOException {
        in.beginObject();

        Map<String, Object> map = new HashMap<>();
        map.put(in.nextName(), in.nextString());
        map.put(in.nextName(), in.nextDouble());
        map.put(in.nextName(), in.nextDouble());
        map.put(in.nextName(), in.nextDouble());
        map.put(in.nextName(), in.nextDouble());
        map.put(in.nextName(), in.nextDouble());

        in.endObject();

        return Location.deserialize(map);
    }
}
