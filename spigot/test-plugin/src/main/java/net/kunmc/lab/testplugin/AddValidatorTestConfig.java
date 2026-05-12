package net.kunmc.lab.testplugin;

import net.kunmc.lab.configlib.BaseConfig;
import net.kunmc.lab.configlib.exception.InvalidValueException;
import net.kunmc.lab.configlib.value.EnumValue;
import net.kunmc.lab.configlib.value.collection.EnumSetValue;
import net.kunmc.lab.configlib.value.map.Enum2IntegerMapValue;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class AddValidatorTestConfig extends BaseConfig {
    public final EnumValue<Material> locationValue = new EnumValue<>(Material.DIAMOND).addValidator(x -> {
        if (x.isAir()) {
            throw new InvalidValueException("cannot set air");
        }
    });
    public final EnumSetValue<EntityType> entityTypes = new EnumSetValue<>(EntityType.class).addValidator(x -> {
        if (x.contains(EntityType.BOAT)) {
            throw new InvalidValueException();
        }
    });
    public final Enum2IntegerMapValue<EntityType> entityType2IntegerMapValue = new Enum2IntegerMapValue<>(EntityType.class).addValidator(
            x -> {
                if (x.containsKey(EntityType.BOAT)) {
                    throw new InvalidValueException();
                }
            });

    public AddValidatorTestConfig(@NotNull Plugin plugin) {
        super(plugin);
        initialize();
    }
}
