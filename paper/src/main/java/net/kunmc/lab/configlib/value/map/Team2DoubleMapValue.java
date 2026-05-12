package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.argument.DoubleArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;
import org.bukkit.scoreboard.Team;

import java.util.List;

public class Team2DoubleMapValue extends Team2ObjectMapValue<Double, Team2DoubleMapValue> {
    @Override
    protected List<PutArgumentDefinition<Team, Double>> argumentDefinitionsForPut() {
        return List.of(new PutArgumentDefinition<>(keyArgumentDefinitionForPut(),
                                                   new ArgumentDefinition<>(new DoubleArgument("double"),
                                                                            (d, ctx) -> d)));
    }

    @Override
    protected String valueToString(Double d) {
        return d.toString();
    }
}
