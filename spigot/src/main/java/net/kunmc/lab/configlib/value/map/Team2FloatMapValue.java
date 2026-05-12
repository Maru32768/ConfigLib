package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.argument.FloatArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;
import org.bukkit.scoreboard.Team;

import java.util.List;

public class Team2FloatMapValue extends Team2ObjectMapValue<Float, Team2FloatMapValue> {
    @Override
    protected List<PutArgumentDefinition<Team, Float>> argumentDefinitionsForPut() {
        return List.of(new PutArgumentDefinition<>(keyArgumentDefinitionForPut(),
                                                   new ArgumentDefinition<>(new FloatArgument("float"),
                                                                            (f, ctx) -> f)));
    }

    @Override
    protected String valueToString(Float f) {
        return f.toString();
    }
}
