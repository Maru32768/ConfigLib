package net.kunmc.lab.configlib.value;

import net.kunmc.lab.commandlib.argument.CommonDoubleArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;
import net.kunmc.lab.configlib.NumericValue;

import java.util.List;

public class DoubleValue extends NumericValue<Double, DoubleValue> {
    public DoubleValue(Double value) {
        this(value, -Double.MAX_VALUE, Double.MAX_VALUE);
    }

    public DoubleValue(Double value, Double min, Double max) {
        super(value, min, max);
    }

    @Override
    public Double plus(Number other) {
        return value + other.doubleValue();
    }

    @Override
    public Double plus(NumericValue<?, ?> other) {
        return value + other.doubleValue();

    }

    @Override
    public Double minus(Number other) {
        return value - other.doubleValue();
    }

    @Override
    public Double minus(NumericValue<?, ?> other) {
        return value - other.doubleValue();
    }

    @Override
    public Double multiply(Number other) {
        return value * other.doubleValue();
    }

    @Override
    public Double multiply(NumericValue<?, ?> other) {
        return value * other.doubleValue();
    }

    @Override
    public Double divide(Number other) {
        return value / other.doubleValue();
    }

    @Override
    public Double divide(NumericValue<?, ?> other) {
        return value / other.doubleValue();
    }

    @Override
    public Double mod(Number other) {
        return value % other.doubleValue();
    }

    @Override
    public Double mod(NumericValue<?, ?> other) {
        return value % other.doubleValue();
    }

    @Override
    protected Double copyAdd(Number amount) {
        return value + amount.doubleValue();
    }

    @Override
    protected Double copySub(Number amount) {
        return value - amount.doubleValue();
    }

    @Override
    protected int compare(Number n) {
        return value.compareTo(n.doubleValue());
    }

    @Override
    protected List<ArgumentDefinition<Double>> argumentDefinitions() {
        return List.of(new ArgumentDefinition<>(new CommonDoubleArgument("Double", min, max), (d, ctx) -> {
            return d;
        }));
    }

    @Override
    protected String valueToString(Double d) {
        return d.toString();
    }

}
