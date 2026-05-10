package com.axiomai.math.calculus;

import com.axiomai.ai.preprocessing.ExpressionExtractor;
import com.axiomai.ai.preprocessing.SymbolicParser;
import com.axiomai.math.expression.Expr;
import com.axiomai.math.expression.Simplifier;
import com.axiomai.service.Memory;

public class CalculusSolver {


// =====================================================
// DERIVATIVE
// =====================================================

    public static String solveDerivative(String text) {

        System.out.println(
                "DEBUG CALCULUS INPUT = " + text
        );

        String exprText =
                ExpressionExtractor.extractExpression(text);

        System.out.println(
                "DEBUG EXTRACTED EXPR = " + exprText
        );

        if (
                exprText == null
                        ||
                        exprText.isBlank()
        ) {

            return """
                I couldn't detect
                the expression
                to differentiate.
                """;
        }

        try {

            Expr expr =
                    SymbolicParser.parse(exprText);

            Expr derivative =
                    Differentiator.d(expr);

            Expr simplified =
                    Simplifier.simplify(derivative);

            String result =
                    simplified.toString();

            String answer =
                    """
                    The derivative of
                    """ + exprText + """

                is:

                """ + result;

            Memory.lastAnswer =
                    answer;

            Memory.lastBreakdown =
                    StepGenerator.derivativeSteps(
                            exprText,
                            result
                    );

            return answer;

        } catch (Exception e) {

            System.out.println(
                    "DERIVATIVE ERROR: "
                            + e.getMessage()
            );

            return """
                Native derivative solver failed.
                """;
        }
    }

// =====================================================
// INTEGRAL
// =====================================================

    public static String solveIntegral(String text) {

        System.out.println(
                "DEBUG CALCULUS INPUT = " + text
        );

        String exprText =
                ExpressionExtractor.extractExpression(text);

        System.out.println(
                "DEBUG EXTRACTED EXPR = " + exprText
        );

        if (
                exprText == null
                        ||
                        exprText.isBlank()
        ) {

            return """
                I couldn't detect
                the expression
                to integrate.
                """;
        }

        try {

            Expr expr =
                    SymbolicParser.parse(exprText);

            Expr integral =
                    Integrator.integrate(expr);

            Expr simplified =
                    Simplifier.simplify(integral);

            String result =
                    simplified.toString();

            String answer =
                    """
                    The integral of
                    """ + exprText + """

                is:

                """ + result + " + C";

            Memory.lastAnswer =
                    answer;

            Memory.lastBreakdown =
                    StepGenerator.integralSteps(
                            exprText,
                            result
                    );

            return answer;

        } catch (Exception e) {

            System.out.println(
                    "INTEGRAL ERROR: "
                            + e.getMessage()
            );

            return """
                Native integral solver failed.
                """;
        }
    }

// =====================================================
// LIMIT
// =====================================================

    public static String solveLimit(String text) {

        String expr =
                ExpressionExtractor.extractExpression(text);

        String point =
                ExpressionExtractor
                        .extractLimitPoint(text);

        if (
                expr == null
                        ||
                        point == null
        ) {

            return """
                I couldn't parse
                the limit expression.
                """;
        }

        String answer =
                """
                Parsed limit expression:
    
                """ + expr + """

            approaching:

            """ + point;

        Memory.lastAnswer =
                answer;

        Memory.lastBreakdown =
                """
                Limit breakdown:
    
                Expression:
                """ + expr + """

            Point:
            """ + point;

        return answer;
    }


}
