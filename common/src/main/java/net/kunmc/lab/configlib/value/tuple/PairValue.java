package net.kunmc.lab.configlib.value.tuple;

import net.kunmc.lab.commandlib.ArgumentBuilder;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentValidationException;
import net.kunmc.lab.configlib.ArgumentDefinition;
import net.kunmc.lab.configlib.SingleValue;
import net.kunmc.lab.configlib.util.function.ArgumentApplier;
import net.kunmc.lab.configlib.util.function.ArgumentMapper;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class PairValue<L, R, T extends PairValue<L, R, T>> extends SingleValue<ConfigPair<L, R>, T> {
    public PairValue(L left, R right) {
        this(ConfigPair.of(left, right));
    }

    private PairValue(ConfigPair<L, R> value) {
        super(value);
    }

    public L getLeft() {
        return value.getLeft();
    }

    public void setLeft(L left) {
        value.setLeft(left);
    }

    public R getRight() {
        return value.getRight();
    }

    public void setRight(R right) {
        value.setRight(right);
    }

    @Override
    protected ConfigPair<L, R> copyValue(ConfigPair<L, R> value) {
        if (value == null) {
            return null;
        }
        return ConfigPair.of(value.getLeft(), value.getRight());
    }

    @Override
    protected String valueToString(ConfigPair<L, R> pair) {
        String leftName = "null";
        if (pair.getLeft() != null) {
            leftName = leftToString(pair.getLeft());
        }

        String rightName = "null";
        if (pair.getRight() != null) {
            rightName = rightToString(pair.getRight());
        }

        return String.format("(%s, %s)", leftName, rightName);
    }

    protected abstract String leftToString(L left);

    protected abstract String rightToString(R right);

    /**
     * Defines how arguments are applied to a builder and mapped to a value.
     * The mapper may throw {@link net.kunmc.lab.commandlib.exception.ArgumentValidationException}
     * to send an error message to the command executor.
     */
    public static class PairArgumentDefinition<L, R> implements ArgumentApplier, ArgumentMapper<ConfigPair<L, R>> {
        private final ArgumentDefinition<L> left;
        private final ArgumentDefinition<R> right;
        private Consumer<ConfigPair<L, R>> validator = (p) -> {
        };

        public PairArgumentDefinition(ArgumentDefinition<L> left, ArgumentDefinition<R> right) {
            this.left = left;
            this.right = right;
        }

        public PairArgumentDefinition<L, R> validator(Consumer<ConfigPair<L, R>> validator) {
            this.validator = Objects.requireNonNull(validator);
            return this;
        }

        public PairArgumentDefinition<L, R> validator(BiConsumer<L, R> validator) {
            Objects.requireNonNull(validator);
            this.validator = p -> validator.accept(p.getLeft(), p.getRight());
            return this;
        }

        @Override
        public void applyArgument(ArgumentBuilder builder) {
            left.applyArgument(builder);
            right.applyArgument(builder);
        }

        @Override
        public ConfigPair<L, R> mapArgument(CommandContext ctx) throws ArgumentValidationException {
            ConfigPair<L, R> value = ConfigPair.of(left.mapArgument(ctx), right.mapArgument(ctx));
            validator.accept(value);
            return value;
        }
    }
}
