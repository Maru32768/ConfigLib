package net.kunmc.lab.configlib.gson;

import com.google.gson.*;
import net.kunmc.lab.configlib.value.tuple.ConfigPair;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class PairTypeAdapter<L, R> implements JsonSerializer<ConfigPair<L, R>>, JsonDeserializer<ConfigPair<L, R>> {
    @Override
    public ConfigPair<L, R> deserialize(JsonElement jsonElement,
                                        Type type,
                                        JsonDeserializationContext ctx) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        Type leftType = Object.class;
        Type rightType = Object.class;
        if (type instanceof ParameterizedType) {
            Type[] typeArguments = ((ParameterizedType) type).getActualTypeArguments();
            leftType = typeArguments[0];
            rightType = typeArguments[1];
        }
        L left = ctx.deserialize(jsonObject.get("left"), leftType);
        R right = ctx.deserialize(jsonObject.get("right"), rightType);

        return ConfigPair.of(left, right);
    }

    @Override
    public JsonElement serialize(ConfigPair<L, R> pair, Type type, JsonSerializationContext ctx) {
        JsonObject res = new JsonObject();
        res.add("left", ctx.serialize(pair.getLeft()));
        res.add("right", ctx.serialize(pair.getRight()));

        return res;
    }
}
