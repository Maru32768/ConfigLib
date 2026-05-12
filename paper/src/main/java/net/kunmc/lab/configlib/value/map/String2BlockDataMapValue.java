package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.argument.BlockDataArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class String2BlockDataMapValue extends String2ObjectMapValue<BlockData, String2BlockDataMapValue> {
    private transient boolean listOnlyBlockName = false;

    public String2BlockDataMapValue() {
        this(new HashMap<>());
    }

    public String2BlockDataMapValue(Map<String, BlockData> value) {
        super(value);
    }

    public String2BlockDataMapValue listOnlyBlockName(boolean listOnlyBlockName) {
        this.listOnlyBlockName = listOnlyBlockName;
        return this;
    }

    @Override
    protected List<PutArgumentDefinition<String, BlockData>> argumentDefinitionsForPut() {
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
