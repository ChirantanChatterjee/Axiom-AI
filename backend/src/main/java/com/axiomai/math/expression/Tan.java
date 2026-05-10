package com.axiomai.math.expression;

public class Tan implements Expr {


    public final Expr arg;

    public Tan(Expr arg) {
        this.arg = arg;
    }

    @Override
    public String toString() {
        return "tan(" + arg + ")";
    }


}
