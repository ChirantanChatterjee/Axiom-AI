package com.axiomai.math.expression;

public class Mul implements Expr {
    public final Expr left, right;

    public Mul(Expr left, Expr right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return "(" + left + " * " + right + ")";
    }
}
