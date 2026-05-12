package net.kunmc.lab.configlib.value.tuple;

import net.kunmc.lab.commandlib.argument.LocationArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;
import org.bukkit.Location;

import java.util.List;

public class Location2LocationPairValue extends Location2ObjectPairValue<Location, Location2LocationPairValue> {
    public Location2LocationPairValue(Location left, Location right) {
        super(left, right);
    }

    @Override
    protected List<PairArgumentDefinition<Location, Location>> argumentDefinitions() {
        return List.of(new PairArgumentDefinition<>(leftArgumentDefinition(),
                                                    new ArgumentDefinition<>(new LocationArgument("location2"),
                                                                             (loc, ctx) -> loc)));
    }

    @Override
    protected String rightToString(Location location) {
        return leftToString(location);
    }
}
