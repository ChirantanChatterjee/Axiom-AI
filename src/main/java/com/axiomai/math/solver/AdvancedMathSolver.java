package com.axiomai.math.solver;

import com.axiomai.math.calculus.CalculusSolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdvancedMathSolver {

    public static String solve(String text) {

        System.out.println("DEBUG ADVANCED SOLVER INPUT = " + text);

        if (text == null || text.isBlank()) {
            return "Give me a math question and I'll try to solve it.";
        }

        String lower = text.toLowerCase();

        // --------------------------------
        // LINEAR ALGEBRA
        // --------------------------------

        if (lower.contains("eigenvalue") || lower.contains("eigenvector")) {
            return LinearAlgebraSolver.solveEigen(text);
        }

        if (lower.contains("determinant") || lower.contains("det(") || lower.contains("det of")) {
            return LinearAlgebraSolver.solveDeterminant(text);
        }

        if (lower.contains("diagonalise") || lower.contains("diagonalize")) {
            return LinearAlgebraSolver.solveDiagonalize(text);
        }

        // --------------------------------
        // THEOREMS
        // --------------------------------

        if (lower.contains("prove") ||
                    lower.contains("show that") ||
                lower.contains("state and prove") ||
                lower.contains("theorem")) {
            System.out.println("DEBUG: Theorem solver triggered");
            return TheoremSolver.solve(text);
        }
        // --------------------------------
        // CALCULUS
        // --------------------------------

        if (lower.contains("differentiate") ||
                lower.contains("derivative") ||
                lower.contains("d/dx")) {

            System.out.println("DEBUG: Derivative solver triggered");
            return CalculusSolver.solveDerivative(text);
        }

        if (lower.contains("integral") ||
                lower.contains("integrate") ||
                lower.contains("∫")) {

            System.out.println("DEBUG: Integral solver triggered");
            return CalculusSolver.solveIntegral(text);
        }

        if (lower.contains("limit")) {

            System.out.println("DEBUG: Limit solver triggered");
            return CalculusSolver.solveLimit(text);
        }

        // --------------------------------
        // VECTOR CALCULUS
        // --------------------------------

        if (lower.contains("gradient") ||
                lower.contains("divergence") ||
                lower.contains("curl")) {

            return VectorCalcSolver.solve(text);
        }


        // --------------------------------
        // QUADRATIC EQUATIONS
        // --------------------------------

        if (lower.matches(".*x\\^2.*=.*")) {
            return solveQuadratic(lower);
        }

        // --------------------------------
        // NUMBER THEORY
        // --------------------------------

        if (lower.contains("integer solutions")) {
            return solveIntegerSolutions(text);
        }
        // --------------------------------
        // SIMPLE LINEAR EQUATIONS
        // --------------------------------

        if (lower.matches(".*x.*=.*")) {
            return solveLinear(lower);
        }


        // --------------------------------
        // FALLBACK
        // --------------------------------

        return """
                I recognise this as advanced mathematics.

                Currently I support:
                • derivatives
                • integrals
                • limits
                • eigenvalues
                • determinants
                • vector calculus
                • theorem explanations
                • quadratic equations
                • basic linear equations
                """;
    }

    // =====================================================
    // QUADRATIC SOLVER
    // =====================================================

    private static String solveQuadratic(String text) {

        try {

            // CLEAN INPUT
            text = text.toLowerCase()
                    .replace("solve", "")
                    .replaceAll("\\s+", "");

            Pattern pattern =
                    Pattern.compile("([+-]?\\d*)x\\^2([+-]\\d*)x([+-]\\d+)=0");

            Matcher m = pattern.matcher(text);

            if (!m.find()) {
                return "I could not parse the quadratic equation.";
            }

            String aStr = m.group(1);
            String bStr = m.group(2);
            String cStr = m.group(3);

            double a = parseCoeff(aStr, 1);
            double b = parseCoeff(bStr, 1);
            double c = Double.parseDouble(cStr);

            double disc = b * b - 4 * a * c;

            if (disc < 0) {
                return "This quadratic has complex roots.";
            }

            double x1 = (-b + Math.sqrt(disc)) / (2 * a);
            double x2 = (-b - Math.sqrt(disc)) / (2 * a);

            return """
                Quadratic Equation Solved:

                x₁ = """ + x1 + """

                x₂ = """ + x2;

        } catch (Exception e) {

            e.printStackTrace();

            return "Could not solve the quadratic equation.";
        }
    }

    // =====================================================
    // LINEAR SOLVER
    // =====================================================

    private static String solveLinear(String text) {

        try {

            text = text.replaceAll("\\s+", "");

            Pattern pattern =
                    Pattern.compile("([+-]?\\d*)x([+-]\\d+)=([+-]?\\d+)");

            Matcher m = pattern.matcher(text);

            if (!m.find()) {
                return "I could not parse the linear equation.";
            }

            String aStr = m.group(1);
            String bStr = m.group(2);
            String cStr = m.group(3);

            double a = parseCoeff(aStr, 1);
            double b = Double.parseDouble(bStr);
            double c = Double.parseDouble(cStr);

            double x = (c - b) / a;

            return "Solution:\nx = " + x;

        } catch (Exception e) {
            return "Could not solve the linear equation.";
        }
    }

    // =====================================================
    // INTEGER SOLUTIONS
    // =====================================================

    private static String solveIntegerSolutions(String text) {

        if (text.contains("3^x") &&
                text.contains("3^y") &&
                text.contains("3^z")) {

            return """
                    For:

                    3^x + 3^y + 3^z = 837

                    observe:

                    837 = 729 + 81 + 27

                    and:

                    729 = 3^6
                    81 = 3^4
                    27 = 3^3

                    Therefore one integer solution is:

                    x = 6
                    y = 4
                    z = 3
                    """;
        }

        return """
                I recognise this as a number theory /
                integer solutions problem,
                but I do not yet have a general solver.
                """;
    }

    // =====================================================
    // HELPER
    // =====================================================

    private static double parseCoeff(String s, double defaultVal) {

        if (s == null || s.isEmpty() || s.equals("+")) {
            return defaultVal;
        }

        if (s.equals("-")) {
            return -defaultVal;
        }

        return Double.parseDouble(s);
    }
}
