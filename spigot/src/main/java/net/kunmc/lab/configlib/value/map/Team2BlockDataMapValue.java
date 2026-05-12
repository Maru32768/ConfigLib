package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.argument.BlockDataArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;
import org.bukkit.block.data.BlockData;
import org.bukkit.scoreboard.Team;

import java.util.List;

public class Team2BlockDataMapValue extends Team2ObjectMapValue<BlockData, Team2BlockDataMapValue> {
    private transient boolean listOnlyBlockName = false;

    public Team2BlockDataMapValue listOnlyBlockName(boolean listOnlyBlockName) {
        this.listOnlyBlockName = listOnlyBlockName;
        return this;
    }

    @Override
    protected List<PutArgumentDefinition<Team, BlockData>> argumentDefinitionsForPut() {
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
