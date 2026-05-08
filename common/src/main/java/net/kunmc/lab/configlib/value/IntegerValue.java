package net.kunmc.lab.configlib.value;

import net.kunmc.lab.commandlib.argument.CommonIntegerArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;
import net.kunmc.lab.configlib.NumericValue;

import java.util.List;

public class IntegerValue extends NumericValue<Integer, IntegerValue> {
    public IntegerValue(Integer value) {
        this(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public IntegerValue(Integer value, Integer min, Integer max) {
        super(value, min, max);
    }

    @Override
    public Integer plus(Number other) {
        return value + other.intValue();
    }

    @Override
    public Integer plus(NumericValue<?, ?> other) {
        return value + other.intValue();

    }

    @Override
    public Integer minus(Number other) {
        return value - other.intValue();
    }

    @Override
    public Integer minus(NumericValue<?, ?> other) {
        return value - other.intValue();
    }

    @Override
    public Integer multiply(Number other) {
        return value * other.intValue();
    }

    @Override
    public Integer multiply(NumericValue<?, ?> other) {
        return value * other.intValue();
    }

    @Override
    public Integer divide(Number other) {
        return value / other.intValue();
    }

    @Override
    public Integer divide(NumericValue<?, ?> other) {
        return value / other.intValue();
    }

    @Override
    public Integer mod(Number other) {
        return value % other.intValue();
    }

    @Override
    public Integer mod(NumericValue<?, ?> other) {
        return value % other.intValue();
    }


    @Override
    protected Integer copyAdd(Number amount) {
        return value + amount.intValue();
    }

    @Override
    protected Integer copySub(Number amount) {
        return value - amount.intValue();
    }

    @Override
    protected int compare(Number n) {
        return value.compareTo(n.intValue());
    }

    @Override
    protected List<ArgumentDefinition<Integer>> argumentDefinitions() {
        return List.of(new ArgumentDefinition<>(new CommonIntegerArgument("Integer", min, max), (n, ctx) -> {
            return n;
        }));
    }

    @Override
    protected String valueToString(Integer integer) {
        return integer.toString();
    }

}
