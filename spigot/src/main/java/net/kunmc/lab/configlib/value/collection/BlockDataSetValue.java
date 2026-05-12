package net.kunmc.lab.configlib.value.collection;

import net.kunmc.lab.commandlib.argument.BlockDataArgument;
import net.kunmc.lab.commandlib.argument.StringArgument;
import net.kunmc.lab.commandlib.exception.ArgumentValidationException;
import net.kunmc.lab.configlib.ArgumentDefinition;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockDataSetValue extends SetValue<BlockData, BlockDataSetValue> {
    private transient boolean listOnlyBlockName = false;

    public BlockDataSetValue() {
        this(new HashSet<>());
    }

    public BlockDataSetValue(@NotNull Set<BlockData> value) {
        super(value);
    }

    public BlockDataSetValue listOnlyBlockName(boolean listOnlyBlockName) {
        this.listOnlyBlockName = listOnlyBlockName;
        return this;
    }

    @Override
    protected List<ArgumentDefinition<Set<BlockData>>> argumentDefinitionsForAdd() {
        return List.of(new ArgumentDefinition<>(new BlockDataArgument("data"), (blockData, ctx) -> Set.of(blockData)));
    }

    @Override
    protected List<ArgumentDefinition<Set<BlockData>>> argumentDefinitionsForRemove() {
        return List.of(new ArgumentDefinition<>(new StringArgument("name", StringArgument.Type.PHRASE).suggestionAction(
                sb -> {
                    value().stream()
                           .map(BlockData::getAsString)
                           .forEach(sb::suggest);
                }), (data, ctx) -> {
            try {
                return Set.of(Bukkit.createBlockData(data));
            } catch (IllegalArgumentException e) {
                throw new ArgumentValidationException(data + "はブロック化出来ない値です.");
            }
        }));
    }

    @Override
    protected String elementToString(BlockData blockData) {
        if (listOnlyBlockName) {
            return blockData.getMaterial()
                            .name();
        } else {
            return blockData.getAsString();
        }
    }
}
