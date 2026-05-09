package com.axiomai.math.expression;

public class Simplifier {

    public static Expr simplify(Expr e) {
        if (e instanceof Const c) {
            return c;
        }

        if (e instanceof Var v) {
            return v;
        }

        if (e instanceof Add a) {
            Expr left = simplify(a.left);
            Expr right = simplify(a.right);

            // 0 + x → x
            if (left instanceof Const lc && lc.value == 0) return right;
            if (right instanceof Const rc && rc.value == 0) return left;

            // both constants → fold
            if (left instanceof Const lc2 && right instanceof Const rc2) {
                return new Const(lc2.value + rc2.value);
            }

            return new Add(left, right);
        }

        if (e instanceof Sub s) {
            Expr left = simplify(s.left);
            Expr right = simplify(s.right);

            // x - 0 → x
            if (right instanceof Const rc && rc.value == 0) return left;

            // both constants → fold
            if (left instanceof Const lc && right instanceof Const rc2) {
                return new Const(lc.value - rc2.value);
            }

            return new Sub(left, right);
        }

        if (e instanceof Mul m) {
            Expr left = simplify(m.left);
            Expr right = simplify(m.right);

            // 0 * x → 0
            if (left instanceof Const lc && lc.value == 0) return new Const(0);
            if (right instanceof Const rc && rc.value == 0) return new Const(0);

            // 1 * x → x
            if (left instanceof Const lc2 && lc2.value == 1) return right;
            if (right instanceof Const rc2 && rc2.value == 1) return left;

            // both constants → fold
            if (left instanceof Const lc3 && right instanceof Const rc3) {
                return new Const(lc3.value * rc3.value);
            }

            return new Mul(left, right);
        }

        if (e instanceof Div d) {
            Expr left = simplify(d.left);
            Expr right = simplify(d.right);

            // x / 1 → x
            if (right instanceof Const rc && rc.value == 1) return left;

            // both constants → fold (avoid divide by zero)
            if (left instanceof Const lc && right instanceof Const rc2 && rc2.value != 0) {
                return new Const(lc.value / rc2.value);
            }

            return new Div(left, right);
        }

        if (e instanceof Pow p) {
            Expr base = simplify(p.base);
            Expr exp = simplify(p.exp);

            // x^1 → x
            if (exp instanceof Const ec && ec.value == 1) return base;

            // x^0 → 1 (x ≠ 0 assumed)
            if (exp instanceof Const ec2 && ec2.value == 0) return new Const(1);

            // constants → fold
            if (base instanceof Const bc && exp instanceof Const ec3) {
                return new Const(Math.pow(bc.value, ec3.value));
            }

            return new Pow(base, exp);
        }

        if (e instanceof Sin s) {
            Expr arg = simplify(s.arg);
            return new Sin(arg);
        }

        if (e instanceof Cos c) {
            Expr arg = simplify(c.arg);
            return new Cos(arg);
        }

        if (e instanceof Exp ex) {
            Expr arg = simplify(ex.arg);
            return new Exp(arg);
        }

        return e;
    }
}
