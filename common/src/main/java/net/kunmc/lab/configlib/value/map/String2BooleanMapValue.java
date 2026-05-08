package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.argument.CommonBooleanArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class String2BooleanMapValue extends String2ObjectMapValue<Boolean, String2BooleanMapValue> {
    public String2BooleanMapValue() {
        this(new HashMap<>());
    }

    public String2BooleanMapValue(Map<String, Boolean> value) {
        super(value);
    }

    @Override
    protected List<PutArgumentDefinition<String, Boolean>> argumentDefinitionsForPut() {
        return List.of(new PutArgumentDefinition<>(keyArgumentDefinitionForPut(),
                                                   new ArgumentDefinition<>(new CommonBooleanArgument("bool"),
                                                                            (b, ctx) -> {
                                                                                return b;
                                                                            })));
    }

    @Override
    protected String valueToString(Boolean aBoolean) {
        return aBoolean.toString();
    }
}
