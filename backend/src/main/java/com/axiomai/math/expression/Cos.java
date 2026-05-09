package com.axiomai.math.expression;

public class Cos implements Expr {
    public final Expr arg;

    public Cos(Expr arg) {
        this.arg = arg;
    }

    @Override
    public String toString() {
        return "cos(" + arg + ")";
    }
}
