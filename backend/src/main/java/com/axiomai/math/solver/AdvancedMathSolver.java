package com.axiomai.math.solver;

import com.axiomai.ai.preprocessing.ExpressionExtractor;
import com.axiomai.api.response.MathResponse;
import com.axiomai.math.calculus.CalculusSolver;
import com.axiomai.math.calculus.StepGenerator;
import com.axiomai.service.Memory;
import com.axiomai.service.SymPyService;

public class AdvancedMathSolver {

    private static final SymPyService symPy =
            new SymPyService();

    public static String solve(String text) {

        System.out.println(
                "DEBUG ADVANCED SOLVER INPUT = "
                        + text
        );

        if (
                text == null
                        ||
                        text.isBlank()
        ) {

            return """
                Give me a math problem.
                """;
        }

        String lower =
                text.toLowerCase();

        // =====================================================
        // BREAKDOWN
        // =====================================================

        if (
                lower.equals("explain")
                        ||
                        lower.equals("break it down")
                        ||
                        lower.equals("break this down")
                        ||
                        lower.equals("show steps")
        ) {

            if (Memory.lastBreakdown != null) {
                return Memory.lastBreakdown;
            }

            return """
                There is nothing recent
                to explain.
                """;
        }

        // =====================================================
        // SYMPY FIRST ROUTING
        // =====================================================

        if (

                lower.contains("differentiate")
                        ||

                        lower.contains("derivative")
                        ||

                        lower.contains("integrate")
                        ||

                        lower.contains("integral")
                        ||

                        lower.contains("simplify")
                        ||

                        lower.contains("limit")
                        ||

                        lower.contains("sin")
                        ||

                        lower.contains("cos")
                        ||

                        lower.contains("tan")
                        ||

                        lower.contains("log")
                        ||

                        lower.contains("sqrt")
                        ||

                        lower.contains("^")
        ) {

            try {

                System.out.println(
                        "ROUTING TO SYMPY FIRST"
                );

                MathResponse response =
                        symPy.solve(text);

                if (
                        response != null
                                &&
                                response.getResult() != null
                ) {

                    String answer =
                            response.getResult();

                    Memory.lastAnswer =
                            answer;

                    // =====================================
                    // STEP GENERATION
                    // =====================================

                    String expr =
                            ExpressionExtractor
                                    .extractExpression(text);

                    if (
                            lower.contains("derivative")
                                    ||
                                    lower.contains("differentiate")
                    ) {

                        Memory.lastBreakdown =
                                StepGenerator
                                        .derivativeSteps(
                                                expr,
                                                answer
                                        );
                    }

                    else if (
                            lower.contains("integral")
                                    ||
                                    lower.contains("integrate")
                    ) {

                        Memory.lastBreakdown =
                                StepGenerator
                                        .integralSteps(
                                                expr,
                                                answer
                                        );
                    }

                    else if (
                            lower.contains("simplify")
                    ) {

                        Memory.lastBreakdown =
                                StepGenerator
                                        .simplifySteps(
                                                expr,
                                                answer
                                        );
                    }

                    return answer;
                }

            } catch (Exception e) {

                System.out.println(
                        "SYMPY FAILED → FALLBACK"
                );
            }
        }

        // =====================================================
        // JAVA FALLBACK
        // =====================================================

        if (
                lower.contains("differentiate")
                        ||
                        lower.contains("derivative")
        ) {

            return CalculusSolver
                    .solveDerivative(text);
        }

        if (
                lower.contains("integrate")
                        ||
                        lower.contains("integral")
        ) {

            return CalculusSolver
                    .solveIntegral(text);
        }

        if (
                lower.contains("limit")
        ) {

            return CalculusSolver
                    .solveLimit(text);
        }

        // =====================================================
        // LINEAR ALGEBRA
        // =====================================================

        if (
                lower.contains("eigenvalue")
                        ||
                        lower.contains("eigenvector")
        ) {

            return LinearAlgebraSolver
                    .solveEigen(text);
        }

        if (
                lower.contains("determinant")
                        ||
                        lower.contains("det(")
        ) {

            return LinearAlgebraSolver
                    .solveDeterminant(text);
        }

        // =====================================================
        // VECTOR CALCULUS
        // =====================================================

        if (
                lower.contains("gradient")
                        ||
                        lower.contains("divergence")
                        ||
                        lower.contains("curl")
        ) {

            return VectorCalcSolver
                    .solve(text);
        }

        // =====================================================
        // THEOREMS
        // =====================================================

        if (
                lower.contains("theorem")
                        ||
                        lower.contains("prove")
        ) {

            return TheoremSolver
                    .solve(text);
        }

        return """
            I recognise this as
            advanced mathematics.

            Current supported areas:
            • derivatives
            • integrals
            • simplify
            • limits
            • graph plotting
            • vector calculus
            • linear algebra
            • theorems
            """;
    }

}
