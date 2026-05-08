package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.argument.CommonStringArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Enum2StringMapValue<T extends Enum<T>> extends Enum2ObjectMapValue<T, String, Enum2StringMapValue<T>> {
    public Enum2StringMapValue(Class<T> kClass) {
        this(kClass, new HashMap<>());
    }

    public Enum2StringMapValue(Class<T> kClass, Map<T, String> value) {
        super(kClass, value);
    }

    @Override
    protected List<PutArgumentDefinition<T, String>> argumentDefinitionsForPut() {
        return List.of(new PutArgumentDefinition<>(keyArgumentDefinitionForPut(),
                                                   new ArgumentDefinition<>(new CommonStringArgument("string",
                                                                                                     CommonStringArgument.Type.PHRASE),
                                                                            (s, ctx) -> {
                                                                                return s;
                                                                            })));
    }

    @Override
    protected String valueToString(String s) {
        return s;
    }
}
