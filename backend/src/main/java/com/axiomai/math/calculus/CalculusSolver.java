package com.axiomai.math.calculus;

import com.axiomai.ai.preprocessing.ExpressionExtractor;
import com.axiomai.ai.preprocessing.SymbolicParser;
import com.axiomai.math.expression.Expr;
import com.axiomai.math.expression.Simplifier;

public class CalculusSolver {

    // =====================================================
    // DERIVATIVES
    // =====================================================

    public static String solveDerivative(String text) {

        System.out.println("DEBUG CALCULUS INPUT = " + text);

        String exprText = ExpressionExtractor.extractExpression(text);

        System.out.println("DEBUG EXTRACTED EXPR = " + exprText);

        if (exprText == null) {
            return "I couldn't see which function you want to differentiate.";
        }

        try {

            Expr expr = SymbolicParser.parse(exprText);

            Expr derivative = Differentiator.d(expr);

            Expr simplified = Simplifier.simplify(derivative);

            return "The derivative of " +
                    exprText +
                    " is:\n\n" +
                    simplified;

        } catch (Exception e) {

            System.out.println("DERIVATIVE SOLVER ERROR: " + e.getMessage());

            e.printStackTrace();

            return "I couldn't parse the expression for differentiation.";
        }
    }

    // =====================================================
    // INTEGRALS
    // =====================================================

    public static String solveIntegral(String text) {

        System.out.println("DEBUG CALCULUS INPUT = " + text);

        String exprText = ExpressionExtractor.extractExpression(text);

        System.out.println("DEBUG EXTRACTED EXPR = " + exprText);

        if (exprText == null) {
            return "I couldn't see which function you want to integrate.";
        }

        try {

            Expr expr = SymbolicParser.parse(exprText);

            Expr integral = Integrator.integrate(expr);

            Expr simplified = Simplifier.simplify(integral);

            return "An antiderivative of " +
                    exprText +
                    " is:\n\n" +
                    simplified +
                    " + C";

        } catch (UnsupportedOperationException u) {

            return "I don't yet know how to integrate that expression symbolically.";

        } catch (Exception e) {

            System.out.println("INTEGRAL SOLVER ERROR: " + e.getMessage());

            e.printStackTrace();

            return "I couldn't parse the expression for integration.";
        }
    }

    // =====================================================
    // LIMITS
    // =====================================================

    public static String solveLimit(String text) {

        System.out.println("DEBUG CALCULUS INPUT = " + text);

        String exprText = ExpressionExtractor.extractExpression(text);

        String pointText = ExpressionExtractor.extractLimitPoint(text);

        System.out.println("DEBUG EXTRACTED EXPR = " + exprText);
        System.out.println("DEBUG EXTRACTED LIMIT POINT = " + pointText);

        if (exprText == null || pointText == null) {
            return "I couldn't fully parse the limit expression and point.";
        }

        try {

            String cleanedExpr =
                    exprText.replaceAll("\\s+", "").toLowerCase();

            String cleanedPoint =
                    pointText.replaceAll("\\s+", "").toLowerCase();

            // -------------------------------------------------
            // CLASSIC TRIG LIMITS
            // -------------------------------------------------

            if (cleanedExpr.contains("sin(x)/x") &&
                    cleanedPoint.equals("0")) {

                return """
                        lim(x→0) sin(x)/x = 1

                        This is a standard trigonometric limit.
                        """;
            }

            if (cleanedExpr.contains("tan(x)/x") &&
                    cleanedPoint.equals("0")) {

                return """
                        lim(x→0) tan(x)/x = 1

                        This is another classic trigonometric limit.
                        """;
            }

            if (cleanedExpr.contains("(1-cos(x))/x") &&
                    cleanedPoint.equals("0")) {

                return """
                        lim(x→0) (1-cos(x))/x = 0
                        """;
            }

            // -------------------------------------------------
            // EXPONENTIAL / LOG LIMITS
            // -------------------------------------------------

            if (cleanedExpr.contains("(e^x-1)/x") &&
                    cleanedPoint.equals("0")) {

                return """
                        lim(x→0) (e^x - 1)/x = 1
                        """;
            }

            if (cleanedExpr.contains("ln(1+x)/x") &&
                    cleanedPoint.equals("0")) {

                return """
                        lim(x→0) ln(1+x)/x = 1
                        """;
            }

            if (cleanedExpr.contains("(1+1/x)^x") &&
                    cleanedPoint.contains("infinity")) {

                return """
                        lim(x→∞) (1 + 1/x)^x = e
                        """;
            }

            // -------------------------------------------------
            // POLYNOMIAL LIMITS
            // -------------------------------------------------

            if (!cleanedExpr.contains("/") &&
                    cleanedPoint.matches("-?\\d+")) {

                return """
                        For polynomial functions,
                        limits are usually evaluated
                        using direct substitution.

                        Full symbolic substitution
                        engine is coming soon.
                        """;
            }

            // -------------------------------------------------
            // SYMBOLIC PARSE TEST
            // -------------------------------------------------

            Expr expr = SymbolicParser.parse(exprText);

            return """
                    I successfully parsed the limit expression:

                    """ + exprText + """

                    approaching:

                    """ + pointText + """

                    but advanced symbolic limit solving
                    is still under development.
                    """;

        } catch (Exception e) {

            System.out.println("LIMIT SOLVER ERROR: " + e.getMessage());

            e.printStackTrace();

            return "I couldn't solve the limit problem.";
        }
    }
}