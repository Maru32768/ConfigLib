package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.argument.CommonDoubleArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Enum2DoubleMapValue<T extends Enum<T>> extends Enum2ObjectMapValue<T, Double, Enum2DoubleMapValue<T>> {
    public Enum2DoubleMapValue(Class<T> kClass) {
        this(kClass, new HashMap<>());
    }

    public Enum2DoubleMapValue(Class<T> kClass, Map<T, Double> value) {
        super(kClass, value);
    }

    @Override
    protected List<PutArgumentDefinition<T, Double>> argumentDefinitionsForPut() {
        return List.of(new PutArgumentDefinition<>(keyArgumentDefinitionForPut(),
                                                   new ArgumentDefinition<>(new CommonDoubleArgument("double"),
                                                                            (d, ctx) -> {
                                                                                return d;
                                                                            })));
    }

    @Override
    protected String valueToString(Double d) {
        return d.toString();
    }
}
