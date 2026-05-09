package com.axiomai.math.calculus;

import com.axiomai.math.expression.*;

public class Differentiator {

    public static Expr d(Expr e) {
        if (e instanceof Const c) {
            return new Const(0);
        }

        if (e instanceof Var v) {
            return new Const(1);
        }

        if (e instanceof Add a) {
            return new Add(d(a.left), d(a.right));
        }

        if (e instanceof Sub s) {
            return new Sub(d(s.left), d(s.right));
        }

        if (e instanceof Mul m) {
            // Product rule: (fg)' = f'g + fg'
            return new Add(
                    new Mul(d(m.left), m.right),
                    new Mul(m.left, d(m.right))
            );
        }

        if (e instanceof Div q) {
            // Quotient rule: (f/g)' = (f'g - fg') / g^2
            return new Div(
                    new Sub(
                            new Mul(d(q.left), q.right),
                            new Mul(q.left, d(q.right))
                    ),
                    new Pow(q.right, new Const(2))
            );
        }

        if (e instanceof Pow p) {
            // Handle x^n
            if (p.base instanceof Var && p.exp instanceof Const c) {
                double n = c.value;
                return new Mul(
                        new Const(n),
                        new Pow(p.base, new Const(n - 1))
                );
            }

            // General case: f(x)^g(x)
            // d/dx f^g = f^g * (g' ln f + g f'/f)
            return new Mul(
                    p,
                    new Add(
                            new Mul(d(p.exp), new Div(new Const(1), new Const(1))), // placeholder
                            new Mul(p.exp, new Div(d(p.base), p.base))
                    )
            );
        }

        if (e instanceof Sin s) {
            return new Mul(new Cos(s.arg), d(s.arg));
        }

        if (e instanceof Cos c) {
            return new Mul(new Const(-1), new Mul(new Sin(c.arg), d(c.arg)));
        }

        if (e instanceof Exp ex) {
            return new Mul(new Exp(ex.arg), d(ex.arg));
        }

        throw new RuntimeException("Don't know how to differentiate: " + e);
    }
}
