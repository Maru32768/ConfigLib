package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.argument.TeamArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;
import net.kunmc.lab.configlib.MapValue;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Team2ObjectMapValue<V, T extends Team2ObjectMapValue<V, T>> extends MapValue<Team, V, T> {
    public Team2ObjectMapValue() {
        this(new HashMap<>());
    }

    public Team2ObjectMapValue(Map<Team, V> map) {
        super(map);
    }

    protected ArgumentDefinition<Team> keyArgumentDefinitionForPut() {
        return new ArgumentDefinition<>(new TeamArgument("team"), (team, ctx) -> team);
    }

    @Override
    protected List<ArgumentDefinition<Team>> argumentDefinitionsForRemove() {
        return List.of(new ArgumentDefinition<>(new TeamArgument("team"), (team, ctx) -> team));
    }

    @Override
    protected String keyToString(Team team) {
        return team.getName();
    }
}
