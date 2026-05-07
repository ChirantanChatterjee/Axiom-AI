package com.axiomai.math.expression;


public class Var implements Expr {
    public final String name;

    public Var(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
