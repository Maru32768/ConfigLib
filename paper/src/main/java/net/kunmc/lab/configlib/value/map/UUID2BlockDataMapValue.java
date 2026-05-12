package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.argument.BlockDataArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;
import org.bukkit.block.data.BlockData;

import java.util.List;
import java.util.UUID;

public class UUID2BlockDataMapValue extends UUID2ObjectMapValue<BlockData, UUID2BlockDataMapValue> {
    private transient boolean listOnlyBlockName = false;

    public UUID2BlockDataMapValue listOnlyBlockName(boolean listOnlyBlockName) {
        this.listOnlyBlockName = listOnlyBlockName;
        return this;
    }

    @Override
    protected List<PutArgumentDefinition<UUID, BlockData>> argumentDefinitionsForPut() {
        return List.of(new PutArgumentDefinition<>(keyArgumentDefinitionForPut(),
                                                   new ArgumentDefinition<>(new BlockDataArgument("data"),
                                                                            (data, ctx) -> data)));
    }

    @Override
    protected String valueToString(BlockData blockData) {
        if (listOnlyBlockName) {
            return blockData.getMaterial()
                            .name();
        } else {
            return blockData.getAsString();
        }
    }
}
