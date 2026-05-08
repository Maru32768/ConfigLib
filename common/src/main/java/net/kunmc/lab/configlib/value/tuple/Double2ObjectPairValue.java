package net.kunmc.lab.configlib.value.tuple;

import net.kunmc.lab.commandlib.argument.CommonDoubleArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;

public abstract class Double2ObjectPairValue<R, T extends Double2ObjectPairValue<R, T>> extends PairValue<Double, R, T> {
    private transient double min = -Double.MAX_VALUE;
    private transient double max = Double.MAX_VALUE;

    public Double2ObjectPairValue(Double left, R right) {
        super(left, right);
    }

    public T leftMin(double min) {
        this.min = min;
        return ((T) this);
    }

    public T leftMax(double max) {
        this.max = max;
        return ((T) this);
    }

    protected ArgumentDefinition<Double> leftArgumentDefinition() {
        return new ArgumentDefinition<>(new CommonDoubleArgument("double", min, max), (d, ctx) -> {
            return d;
        });
    }

    @Override
    protected String leftToString(Double left) {
        return left.toString();
    }
}
