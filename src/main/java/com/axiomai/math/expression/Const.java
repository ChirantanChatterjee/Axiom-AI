package com.axiomai.math.expression;

public class Const implements Expr {
    public final double value;

    public Const(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }
}
