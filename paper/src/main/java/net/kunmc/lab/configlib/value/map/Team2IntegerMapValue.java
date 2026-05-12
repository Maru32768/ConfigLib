package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.argument.IntegerArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;
import org.bukkit.scoreboard.Team;

import java.util.List;

public class Team2IntegerMapValue extends Team2ObjectMapValue<Integer, Team2IntegerMapValue> {
    @Override
    protected List<PutArgumentDefinition<Team, Integer>> argumentDefinitionsForPut() {
        return List.of(new PutArgumentDefinition<>(keyArgumentDefinitionForPut(),
                                                   new ArgumentDefinition<>(new IntegerArgument("integer"),
                                                                            (n, ctx) -> n)));
    }

    @Override
    protected String valueToString(Integer integer) {
        return integer.toString();
    }
}
