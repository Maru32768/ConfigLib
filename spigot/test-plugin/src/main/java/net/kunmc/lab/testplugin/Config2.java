package net.kunmc.lab.testplugin;

import net.kunmc.lab.configlib.exception.InvalidValueException;
import net.kunmc.lab.configlib.value.IntegerValue;
import net.kunmc.lab.configlib.value.StringValue;
import net.kunmc.lab.configlib.value.collection.ItemStackListValue;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class Config2 extends AbstractConfig {
    public final IntegerValue n = new IntegerValue(12).onInitialize(x -> TestPlugin.broadcast("n initialize"))
                                                      .onModify(x -> TestPlugin.broadcast(x.toString()));
    public final ItemStackListValue itemStacks = new ItemStackListValue();
    public final StringValue str = new StringValue("hoge").addValidator(x -> {
        if (!x.equals("hoge")) {
            throw new InvalidValueException("only hoge");
        }
    });
    public final IntegerValue conflict = new IntegerValue(1);

    public Config2(@NotNull Plugin plugin) {
        super(plugin);
        ItemStack item = new ItemStack(Material.JUNGLE_LOG);
        item.editMeta(meta -> meta.displayName(Component.text("aiueo")));
        itemStacks.add(item);
        initialize();
    }
}
