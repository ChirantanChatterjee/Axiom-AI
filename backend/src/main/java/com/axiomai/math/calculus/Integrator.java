package com.axiomai.math.calculus;

import com.axiomai.math.expression.*;

public class Integrator {

    // ∫ e dx, assuming variable is x
    public static Expr integrate(Expr e) {
        if (e instanceof Const c) {
            // ∫ a dx = a x
            return new Mul(new Const(c.value), new Var("x"));
        }

        if (e instanceof Var v) {
            if (v.name.equals("x")) {
                // ∫ x dx = x^2 / 2
                return new Div(
                        new Pow(new Var("x"), new Const(2)),
                        new Const(2)
                );
            }
        }

        if (e instanceof Add a) {
            // ∫ (f + g) dx = ∫ f dx + ∫ g dx
            return new Add(integrate(a.left), integrate(a.right));
        }

        if (e instanceof Sub s) {
            // ∫ (f - g) dx = ∫ f dx - ∫ g dx
            return new Sub(integrate(s.left), integrate(s.right));
        }

        if (e instanceof Mul m) {
            // Very limited: handle a * x^n where a is constant, n != -1
            if (m.left instanceof Const c && m.right instanceof Pow p &&
                    p.base instanceof Var v && v.name.equals("x") &&
                    p.exp instanceof Const ec && ec.value != -1) {

                double n = ec.value;
                // ∫ a x^n dx = a x^(n+1) / (n+1)
                return new Div(
                        new Mul(
                                new Const(c.value),
                                new Pow(new Var("x"), new Const(n + 1))
                        ),
                        new Const(n + 1)
                );
            }

            // fallback: not implemented
            throw new UnsupportedOperationException("Don't know how to integrate product: " + e);
        }

        if (e instanceof Pow p) {
            // ∫ x^n dx, n != -1
            if (p.base instanceof Var v && v.name.equals("x") &&
                    p.exp instanceof Const c && c.value != -1) {

                double n = c.value;
                return new Div(
                        new Pow(new Var("x"), new Const(n + 1)),
                        new Const(n + 1)
                );
            }

            // ∫ x^-1 dx = ln|x|
            if (p.base instanceof Var v2 && v2.name.equals("x") &&
                    p.exp instanceof Const c2 && c2.value == -1) {
                // represent ln|x| as Var("ln|x|") *just as a placeholder*
                return new Var("ln|x|");
            }
        }

        if (e instanceof Sin s) {
            // ∫ sin(x) dx = -cos(x)
            if (s.arg instanceof Var v && v.name.equals("x")) {
                return new Mul(new Const(-1), new Cos(new Var("x")));
            }
        }

        if (e instanceof Cos c) {
            // ∫ cos(x) dx = sin(x)
            if (c.arg instanceof Var v && v.name.equals("x")) {
                return new Sin(new Var("x"));
            }
        }

        if (e instanceof Exp ex) {
            // ∫ e^{x} dx = e^{x}
            if (ex.arg instanceof Var v && v.name.equals("x")) {
                return new Exp(new Var("x"));
            }
        }

        throw new UnsupportedOperationException("Don't know how to integrate: " + e);
    }
}
