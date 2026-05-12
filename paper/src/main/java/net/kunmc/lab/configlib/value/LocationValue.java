package net.kunmc.lab.configlib.value;

import net.kunmc.lab.commandlib.argument.LocationArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;
import net.kunmc.lab.configlib.SingleValue;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.List;

public class LocationValue extends SingleValue<Location, LocationValue> {
    public LocationValue() {
        this(null);
    }

    public LocationValue(Location value) {
        super(value);
    }

    @Override
    protected Location copyValue(Location value) {
        return value.clone();
    }

    public Block getBlock() {
        if (value == null) {
            return null;
        }

        return value.getBlock();
    }

    public Vector toVector() {
        if (value == null) {
            return null;
        }

        return value.toVector();
    }

    @Override
    protected List<ArgumentDefinition<Location>> argumentDefinitions() {
        return List.of(new ArgumentDefinition<>(new LocationArgument("location"), (loc, ctx) -> loc));
    }

    @Override
    protected String valueToString(Location location) {
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
