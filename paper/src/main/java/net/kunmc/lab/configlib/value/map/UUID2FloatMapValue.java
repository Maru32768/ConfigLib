package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.argument.FloatArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;

import java.util.List;
import java.util.UUID;

public class UUID2FloatMapValue extends UUID2ObjectMapValue<Float, UUID2FloatMapValue> {
    @Override
    protected List<PutArgumentDefinition<UUID, Float>> argumentDefinitionsForPut() {
        return List.of(new PutArgumentDefinition<>(keyArgumentDefinitionForPut(),
                                                   new ArgumentDefinition<>(new FloatArgument("float"),
                                                                            (f, ctx) -> f)));
    }

    @Override
    protected String valueToString(Float f) {
        return f.toString();
    }
}
