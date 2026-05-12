package net.kunmc.lab.configlib.value;

import net.kunmc.lab.commandlib.argument.IntegerArgument;
import net.kunmc.lab.commandlib.argument.ItemStackArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;
import net.kunmc.lab.configlib.SingleValue;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ItemStackValue extends SingleValue<ItemStack, ItemStackValue> {
    public ItemStackValue() {
        this(null);
    }

    public ItemStackValue(ItemStack value) {
        super(value);
    }

    @Override
    protected ItemStack copyValue(ItemStack value) {
        return new ItemStack(value);
    }

    @Override
    protected List<ArgumentDefinition<ItemStack>> argumentDefinitions() {
        return List.of(new ArgumentDefinition<>(new ItemStackArgument("item"),
                                                new IntegerArgument("amount", 1, Integer.MAX_VALUE),
                                                (item, amount, ctx) -> {
                                                    item.setAmount(amount);
                                                    return item;
                                                }));
    }

    @Override
    protected String valueToString(ItemStack itemStack) {
        return itemStack.toString();
    }
}
