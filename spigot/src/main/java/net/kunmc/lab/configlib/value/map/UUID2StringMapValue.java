package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.argument.StringArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;

import java.util.List;
import java.util.UUID;

public class UUID2StringMapValue extends UUID2ObjectMapValue<String, UUID2StringMapValue> {
    @Override
    protected List<PutArgumentDefinition<UUID, String>> argumentDefinitionsForPut() {
        return List.of(new PutArgumentDefinition<>(keyArgumentDefinitionForPut(),
                                                   new ArgumentDefinition<>(new StringArgument("string",
                                                                                               StringArgument.Type.PHRASE),
                                                                            (s, ctx) -> s)));
    }

    @Override
    protected String valueToString(String s) {
        return s;
    }
}
