package com.axiomai.math.calculus;

public class StepGenerator {

// =====================================================
// DERIVATIVE STEPS
// =====================================================

    public static String derivativeSteps(
            String expr,
            String result
    ) {

        StringBuilder sb =
                new StringBuilder();

        sb.append("""
            Step-by-step derivative explanation:

            Original expression:

            """);

        sb.append(expr).append("\n\n");

        sb.append("Rules used:\n");

        if (expr.contains("*")) {

            sb.append("• product rule\n");
        }

        if (expr.contains("^")) {

            sb.append("• power rule\n");
        }

        if (
                expr.contains("sin")
                        ||
                        expr.contains("cos")
                        ||
                        expr.contains("tan")
        ) {

            sb.append(
                    "• trigonometric derivative rules\n"
            );
        }

        if (
                expr.contains("e^")
                        ||
                        expr.contains("exp")
        ) {

            sb.append(
                    "• exponential derivative rules\n"
            );
        }

        sb.append("\n");

        sb.append("""
            Final derivative:

            """);

        sb.append(result);

        return sb.toString();
    }

// =====================================================
// INTEGRAL STEPS
// =====================================================

    public static String integralSteps(
            String expr,
            String result
    ) {

        StringBuilder sb =
                new StringBuilder();

        sb.append("""
            Step-by-step integration explanation:

            Original expression:

            """);

        sb.append(expr).append("\n\n");

        sb.append("Rules used:\n");

        if (expr.contains("^")) {

            sb.append(
                    "• power rule integration\n"
            );
        }

        if (
                expr.contains("sin")
                        ||
                        expr.contains("cos")
        ) {

            sb.append(
                    "• trigonometric integration rules\n"
            );
        }

        if (
                expr.contains("e^")
                        ||
                        expr.contains("exp")
        ) {

            sb.append(
                    "• exponential integration rules\n"
            );
        }

        sb.append("\n");

        sb.append("""
            Final integral:

            """);

        sb.append(result).append(" + C");

        return sb.toString();
    }

// =====================================================
// SIMPLIFY STEPS
// =====================================================

    public static String simplifySteps(
            String original,
            String simplified
    ) {

        return """
            Simplification breakdown:

            Original expression:

            """ + original + """

            Simplified result:

            """ + simplified;
    }

}
