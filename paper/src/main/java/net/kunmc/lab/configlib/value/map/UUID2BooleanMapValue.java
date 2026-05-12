package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.argument.BooleanArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;

import java.util.List;
import java.util.UUID;

public class UUID2BooleanMapValue extends UUID2ObjectMapValue<Boolean, UUID2BooleanMapValue> {
    @Override
    protected List<PutArgumentDefinition<UUID, Boolean>> argumentDefinitionsForPut() {
        return List.of(new PutArgumentDefinition<>(keyArgumentDefinitionForPut(),
                                                   new ArgumentDefinition<>(new BooleanArgument("bool"),
                                                                            (b, ctx) -> b)));
    }

    @Override
    protected String valueToString(Boolean bool) {
        return bool.toString();
    }
}
