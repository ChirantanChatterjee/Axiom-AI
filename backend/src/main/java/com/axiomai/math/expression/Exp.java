package com.axiomai.math.expression;

public class Exp implements Expr {
    public final Expr arg;

    public Exp(Expr arg) {
        this.arg = arg;
    }

    @Override
    public String toString() {
        return "exp(" + arg + ")";
    }
}
