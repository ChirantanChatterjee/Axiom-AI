package com.axiomai.math.expression;

public class Div implements Expr {
    public final Expr left, right;

    public Div(Expr left, Expr right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return "(" + left + " / " + right + ")";
    }
}
