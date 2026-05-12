package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.argument.UUIDArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;

import java.util.List;
import java.util.UUID;

public class UUID2UUIDMapValue extends UUID2ObjectMapValue<UUID, UUID2UUIDMapValue> {
    @Override
    protected List<PutArgumentDefinition<UUID, UUID>> argumentDefinitionsForPut() {
        return List.of(new PutArgumentDefinition<>(keyArgumentDefinitionForPut(),
                                                   new ArgumentDefinition<>(new UUIDArgument("value"),
                                                                            (value, ctx) -> value)));
    }

    @Override
    protected String valueToString(UUID uuid) {
        return keyToString(uuid);
    }
}
