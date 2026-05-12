package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.argument.IntegerArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;

import java.util.List;
import java.util.UUID;

public class UUID2IntegerMapValue extends UUID2ObjectMapValue<Integer, UUID2IntegerMapValue> {
    @Override
    protected List<PutArgumentDefinition<UUID, Integer>> argumentDefinitionsForPut() {
        return List.of(new PutArgumentDefinition<>(keyArgumentDefinitionForPut(),
                                                   new ArgumentDefinition<>(new IntegerArgument("integer"),
                                                                            (n, ctx) -> n)));
    }

    @Override
    protected String valueToString(Integer integer) {
        return integer.toString();
    }
}
