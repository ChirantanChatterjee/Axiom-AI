package com.axiomai.math.solver;

public class VectorCalcSolver {

    public static String solve(String text) {
        String lower = text.toLowerCase();

        if (lower.contains("gradient")) {
            return "The gradient ∇f is the vector of partial derivatives. I can explain conceptually but don't yet compute arbitrary gradients.";
        }

        if (lower.contains("divergence")) {
            return "The divergence of F = (P,Q,R) is ∂P/∂x + ∂Q/∂y + ∂R/∂z.";
        }

        if (lower.contains("curl")) {
            return "The curl of F = (P,Q,R) is ∇×F = (∂R/∂y−∂Q/∂z, ∂P/∂z−∂R/∂x, ∂Q/∂x−∂P/∂y).";
        }

        return "I recognise this as vector calculus, but I only give basic formulas for now.";
    }
}
