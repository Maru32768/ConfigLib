package net.kunmc.lab.configlib.value.tuple;

import net.kunmc.lab.commandlib.argument.CommonIntegerArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;

public abstract class Integer2ObjectPairValue<R, T extends Integer2ObjectPairValue<R, T>> extends PairValue<Integer, R, T> {
    private transient int min = Integer.MIN_VALUE;
    private transient int max = Integer.MAX_VALUE;

    public Integer2ObjectPairValue(Integer left, R right) {
        super(left, right);
    }

    public T leftMin(int min) {
        this.min = min;
        return ((T) this);
    }

    public T leftMax(int max) {
        this.max = max;
        return ((T) this);
    }

    protected ArgumentDefinition<Integer> leftArgumentDefinition() {
        return new ArgumentDefinition<>(new CommonIntegerArgument("integer", min, max), (n, ctx) -> {
            return n;
        });
    }

    @Override
    protected String leftToString(Integer left) {
        return left.toString();
    }
}
