package com.axiomai.math.expression;

public class Sin implements Expr {
    public final Expr arg;

    public Sin(Expr arg) {
        this.arg = arg;
    }

    @Override
    public String toString() {
        return "sin(" + arg + ")";
    }
}
