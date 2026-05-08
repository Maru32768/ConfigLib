package net.kunmc.lab.configlib.value;

import net.kunmc.lab.commandlib.argument.CommonFloatArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;
import net.kunmc.lab.configlib.NumericValue;

import java.util.List;

public class FloatValue extends NumericValue<Float, FloatValue> {
    public FloatValue(Float value) {
        this(value, -Float.MAX_VALUE, Float.MAX_VALUE);
    }

    public FloatValue(Float value, Float min, Float max) {
        super(value, min, max);
    }

    @Override
    public Float plus(Number other) {
        return value + other.floatValue();
    }

    @Override
    public Float plus(NumericValue<?, ?> other) {
        return value + other.floatValue();

    }

    @Override
    public Float minus(Number other) {
        return value - other.floatValue();
    }

    @Override
    public Float minus(NumericValue<?, ?> other) {
        return value - other.floatValue();
    }

    @Override
    public Float multiply(Number other) {
        return value * other.floatValue();
    }

    @Override
    public Float multiply(NumericValue<?, ?> other) {
        return value * other.floatValue();
    }

    @Override
    public Float divide(Number other) {
        return value / other.floatValue();
    }

    @Override
    public Float divide(NumericValue<?, ?> other) {
        return value / other.floatValue();
    }

    @Override
    public Float mod(Number other) {
        return value % other.floatValue();
    }

    @Override
    public Float mod(NumericValue<?, ?> other) {
        return value % other.floatValue();
    }

    @Override
    protected Float copyAdd(Number amount) {
        return value + amount.floatValue();
    }

    @Override
    protected Float copySub(Number amount) {
        return value - amount.floatValue();
    }

    @Override
    protected int compare(Number n) {
        return value.compareTo(n.floatValue());
    }

    @Override
    protected List<ArgumentDefinition<Float>> argumentDefinitions() {
        return List.of(new ArgumentDefinition<>(new CommonFloatArgument("float", min, max), (n, ctx) -> {
            return n;
        }));
    }

    @Override
    protected String valueToString(Float f) {
        return f.toString();
    }

}
