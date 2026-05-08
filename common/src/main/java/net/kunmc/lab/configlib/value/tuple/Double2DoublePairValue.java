package net.kunmc.lab.configlib.value.tuple;

import net.kunmc.lab.commandlib.argument.CommonDoubleArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;

import java.util.List;

public class Double2DoublePairValue extends Double2ObjectPairValue<Double, Double2DoublePairValue> {
    private transient double min = -Double.MAX_VALUE;
    private transient double max = Double.MAX_VALUE;

    public Double2DoublePairValue(Double left, Double right) {
        super(left, right);
    }

    public Double2DoublePairValue rightMin(double min) {
        this.min = min;
        return this;
    }

    public Double2DoublePairValue rightMax(double max) {
        this.max = max;
        return this;
    }

    @Override
    protected List<PairArgumentDefinition<Double, Double>> argumentDefinitions() {
        return List.of(new PairArgumentDefinition<>(leftArgumentDefinition(),
                                                    new ArgumentDefinition<>(new CommonDoubleArgument("double2",
                                                                                                      min,
                                                                                                      max),
                                                                             (d, ctx) -> {
                                                                                 return d;
                                                                             })));
    }

    @Override
    protected String rightToString(Double right) {
        return right.toString();
    }
}
