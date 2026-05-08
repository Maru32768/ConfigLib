package net.kunmc.lab.configlib.value;

import net.kunmc.lab.commandlib.argument.CommonBooleanArgument;
import net.kunmc.lab.configlib.ArgumentDefinition;
import net.kunmc.lab.configlib.SingleValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BooleanValue extends SingleValue<Boolean, BooleanValue> {
    public BooleanValue(@NotNull Boolean value) {
        super(value);
    }

    public boolean isTrue() {
        return value;
    }

    public boolean isFalse() {
        return !value;
    }

    public void ifTrue(Runnable runnable) {
        if (value) {
            runnable.run();
        }
    }

    public void ifFalse(Runnable runnable) {
        if (!value) {
            runnable.run();
        }
    }

    public void ifTrueOrElse(Runnable trueRunnable, Runnable falseRunnable) {
        if (value) {
            trueRunnable.run();
        } else {
            falseRunnable.run();
        }
    }

    public boolean and(boolean b) {
        return value && b;
    }

    public boolean and(BooleanValue b) {
        return value && b.value;
    }

    public boolean or(boolean b) {
        return value || b;
    }

    public boolean or(BooleanValue b) {
        return value || b.value;
    }

    public boolean xor(boolean b) {
        return value ^ b;
    }

    public boolean xor(BooleanValue b) {
        return value ^ b.value;
    }

    @Override
    protected List<ArgumentDefinition<Boolean>> argumentDefinitions() {
        return List.of(new ArgumentDefinition<>(new CommonBooleanArgument("bool"), (b, ctx) -> {
            return b;
        }));
    }

    @Override
    protected String valueToString(Boolean bool) {
        return bool.toString();
    }

}
