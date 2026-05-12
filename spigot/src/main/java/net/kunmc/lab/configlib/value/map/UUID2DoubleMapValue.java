package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.argument.DoubleArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;

import java.util.List;
import java.util.UUID;

public class UUID2DoubleMapValue extends UUID2ObjectMapValue<Double, UUID2DoubleMapValue> {
    @Override
    protected List<PutArgumentDefinition<UUID, Double>> argumentDefinitionsForPut() {
        return List.of(new PutArgumentDefinition<>(keyArgumentDefinitionForPut(),
                                                   new ArgumentDefinition<>(new DoubleArgument("double"),
                                                                            (d, ctx) -> d)));
    }

    @Override
    protected String valueToString(Double d) {
        return d.toString();
    }
}
