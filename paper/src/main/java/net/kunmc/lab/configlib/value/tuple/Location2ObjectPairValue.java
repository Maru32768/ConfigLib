package net.kunmc.lab.configlib.value.tuple;

import net.kunmc.lab.commandlib.argument.LocationArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;
import org.bukkit.Location;

public abstract class Location2ObjectPairValue<R, T extends Location2ObjectPairValue<R, T>> extends PairValue<Location, R, T> {
    public Location2ObjectPairValue(Location left, R right) {
        super(left, right);
    }

    protected ArgumentDefinition<Location> leftArgumentDefinition() {
        return new ArgumentDefinition<>(new LocationArgument("location"), (loc, ctx) -> loc);
    }

    @Override
    protected String leftToString(Location location) {
        String worldName = "null";
        if (location.getWorld() != null) {
            worldName = location.getWorld()
                                .getName();
        }

        return String.format("{world=%s,x=%.1f,y=%.1f,z=%.1f,pitch=%.1f,yaw=%.1f}",
                             worldName,
                             location.getX(),
                             location.getY(),
                             location.getZ(),
                             location.getPitch(),
                             location.getYaw());
    }
}
