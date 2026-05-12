package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.argument.LocationArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;
import org.bukkit.Location;
import org.bukkit.scoreboard.Team;

import java.util.List;

public class Team2LocationMapValue extends Team2ObjectMapValue<Location, Team2LocationMapValue> {
    @Override
    protected List<PutArgumentDefinition<Team, Location>> argumentDefinitionsForPut() {
        return List.of(new PutArgumentDefinition<>(keyArgumentDefinitionForPut(),
                                                   new ArgumentDefinition<>(new LocationArgument("location"),
                                                                            (loc, ctx) -> loc)));
    }

    @Override
    protected String valueToString(Location location) {
        return String.format("world=%s,x=%.1f,y=%.1f,z=%.1f,pitch=%.1f,yaw=%.1f",
                             location.getWorld()
                                     .getName(),
                             location.getX(),
                             location.getY(),
                             location.getZ(),
                             location.getPitch(),
                             location.getYaw());
    }
}
