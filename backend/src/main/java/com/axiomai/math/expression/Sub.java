package com.axiomai.math.expression;

public class Sub implements Expr {
    public final Expr left, right;

    public Sub(Expr left, Expr right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return "(" + left + " - " + right + ")";
    }
}
