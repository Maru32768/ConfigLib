package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.argument.CommonBooleanArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Enum2BooleanMapValue<T extends Enum<T>> extends Enum2ObjectMapValue<T, Boolean, Enum2BooleanMapValue<T>> {
    public Enum2BooleanMapValue(Class<T> kClass) {
        this(kClass, new HashMap<>());
    }

    public Enum2BooleanMapValue(Class<T> kClass, Map<T, Boolean> value) {
        super(kClass, value);
    }

    @Override
    protected List<PutArgumentDefinition<T, Boolean>> argumentDefinitionsForPut() {
        return List.of(new PutArgumentDefinition<>(keyArgumentDefinitionForPut(),
                                                   new ArgumentDefinition<>(new CommonBooleanArgument("bool"),
                                                                            (b, ctx) -> {
                                                                                return b;
                                                                            })));
    }

    @Override
    protected String valueToString(Boolean bool) {
        return bool.toString();
    }
}
