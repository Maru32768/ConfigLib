package net.kunmc.lab.configlib.value.tuple;

import java.util.Objects;

public class ConfigPair<L, R> {
    private L left;
    private R right;

    public ConfigPair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public static <L, R> ConfigPair<L, R> of(L left, R right) {
        return new ConfigPair<>(left, right);
    }

    public L getLeft() {
        return left;
    }

    public void setLeft(L left) {
        this.left = left;
    }

    public R getRight() {
        return right;
    }

    public void setRight(R right) {
        this.right = right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConfigPair)) {
            return false;
        }
        ConfigPair<?, ?> that = (ConfigPair<?, ?>) o;
        return Objects.equals(left, that.left) && Objects.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    @Override
    public String toString() {
        return "(" + left + ", " + right + ")";
    }
}
