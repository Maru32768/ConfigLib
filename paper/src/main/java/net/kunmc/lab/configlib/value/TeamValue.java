package net.kunmc.lab.configlib.value;

import net.kunmc.lab.commandlib.argument.TeamArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;
import net.kunmc.lab.configlib.SingleValue;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TeamValue extends SingleValue<Team, TeamValue> {
    private final transient Scoreboard scoreboard;

    public TeamValue() {
        this(null);
    }

    public TeamValue(Team value) {
        super(value);
        scoreboard = Bukkit.getScoreboardManager()
                           .getMainScoreboard();
    }

    public @NotNull Scoreboard scoreboard() {
        return scoreboard;
    }

    @Override
    protected List<ArgumentDefinition<Team>> argumentDefinitions() {
        return List.of(new ArgumentDefinition<>(new TeamArgument("team"), (team, ctx) -> team));
    }

    @Override
    protected String valueToString(Team team) {
        return team.getName();
    }
}
