package net.kunmc.lab.configlib.value.tuple;

import net.kunmc.lab.commandlib.argument.CommonIntegerArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;

import java.util.List;

public class Integer2IntegerPairValue extends Integer2ObjectPairValue<Integer, Integer2IntegerPairValue> {
    private transient int min = Integer.MIN_VALUE;
    private transient int max = Integer.MAX_VALUE;

    public Integer2IntegerPairValue(Integer left, Integer right) {
        super(left, right);
    }

    public Integer2IntegerPairValue rightMin(int min) {
        this.min = min;
        return this;
    }

    public Integer2IntegerPairValue rightMax(int max) {
        this.max = max;
        return this;
    }

    @Override
    protected List<PairArgumentDefinition<Integer, Integer>> argumentDefinitions() {
        return List.of(new PairArgumentDefinition<>(leftArgumentDefinition(),
                                                    new ArgumentDefinition<>(new CommonIntegerArgument("integer2",
                                                                                                       min,
                                                                                                       max),
                                                                             (n, ctx) -> {
                                                                                 return n;
                                                                             })));
    }

    @Override
    protected String rightToString(Integer right) {
        return right.toString();
    }
}
