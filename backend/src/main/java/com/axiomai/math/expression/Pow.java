package com.axiomai.math.expression;

public class Pow implements Expr {
    public final Expr base, exp;

    public Pow(Expr base, Expr exp) {
        this.base = base;
        this.exp = exp;
    }

    @Override
    public String toString() {
        return "(" + base + "^" + exp + ")";
    }
}
