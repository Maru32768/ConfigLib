package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.argument.CommonIntegerArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class String2IntegerMapValue extends String2ObjectMapValue<Integer, String2IntegerMapValue> {
    public String2IntegerMapValue() {
        this(new HashMap<>());
    }

    public String2IntegerMapValue(Map<String, Integer> value) {
        super(value);
    }

    @Override
    protected List<PutArgumentDefinition<String, Integer>> argumentDefinitionsForPut() {
        return List.of(new PutArgumentDefinition<>(keyArgumentDefinitionForPut(),
                                                   new ArgumentDefinition<>(new CommonIntegerArgument("integer"),
                                                                            (n, ctx) -> {
                                                                                return n;
                                                                            })));
    }

    @Override
    protected String valueToString(Integer integer) {
        return integer.toString();
    }
}
