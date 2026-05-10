package com.axiomai.ai.intent;

public class IntentDetector {

    // =====================================================
    // ARITHMETIC
    // =====================================================

    public static boolean isArithmetic(String text) {

        text = text.toLowerCase().trim();

        if (
                text.contains("x") ||
                        text.contains("y") ||
                        text.contains("^") ||
                        text.contains("sin") ||
                        text.contains("cos") ||
                        text.contains("tan") ||
                        text.contains("log") ||
                        text.contains("ln") ||
                        text.contains("sqrt") ||
                        text.contains("integral") ||
                        text.contains("integrate") ||
                        text.contains("derivative") ||
                        text.contains("differentiate") ||
                        text.contains("limit")
        ) {
            return false;
        }

        return

                text.matches(".*\\d+\\s*[+\\-*/]\\s*\\d+.*")

                        ||

                        text.contains("plus") ||
                        text.contains("minus") ||
                        text.contains("times") ||
                        text.contains("multiplied") ||
                        text.contains("divide");
    }

    // =====================================================
    // GRAPH
    // =====================================================

    public static boolean isGraph(String text) {

        text = text.toLowerCase();

        return text.contains("plot") ||
                text.contains("graph") ||
                text.contains("draw") ||
                text.contains("visualize");
    }

    // =====================================================
    // ADVANCED MATH
    // =====================================================

    public static boolean isAdvancedMath(String text) {

        text = text.toLowerCase();

        // =====================================================
        // NLP EQUATION DETECTION
        // =====================================================

        boolean looksLikeEquation =

                (
                        text.contains("equals")
                                ||
                                text.contains("=")
                )

                        &&

                        (
                                text.contains("x")
                                        ||
                                        text.contains("y")
                                        ||
                                        text.contains("z")
                        );

        // =====================================================
        // SYMBOLIC DETECTION
        // =====================================================

        return

                looksLikeEquation

                        ||

                        text.contains("simplify") ||

                        text.contains("integral") ||
                        text.contains("integrate") ||

                        text.contains("differentiate") ||
                        text.contains("derivative") ||

                        text.contains("limit") ||

                        text.contains("sin") ||
                        text.contains("cos") ||
                        text.contains("tan") ||

                        text.contains("log") ||
                        text.contains("ln") ||
                        text.contains("sqrt") ||

                        text.contains("^") ||

                        text.contains("solve") ||

                        text.contains("matrix") ||
                        text.contains("vector") ||

                        text.contains("theorem") ||
                        text.contains("prove");
    }
    // =====================================================
    // INVESTMENT
    // =====================================================

    public static boolean isInvestment(String text) {

        text = text.toLowerCase();

        return text.contains("invest") ||
                text.contains("investment") ||
                text.contains("return") ||
                text.contains("interest") ||
                text.contains("growth") ||
                text.contains("fund");
    }

    // =====================================================
    // REQUIRED PRINCIPAL
    // =====================================================

    public static boolean isRequiredPrincipal(String text) {

        text = text.toLowerCase();

        return

                text.contains("how much should i invest")

                        ||

                        text.contains("how much do i need to invest")

                        ||

                        text.contains("how much money should i invest")

                        ||

                        text.contains("need today")

                        ||

                        text.contains("required principal")

                        ||

                        text.contains("reach")

                        ||

                        text.contains("target")

                        ||

                        text.contains("goal")

                        ||

                        text.contains("million")

                        ||

                        text.contains("get 100000")

                        ||

                        text.contains("to get");
    }

    // =====================================================
    // YEARS
    // =====================================================

    public static boolean isYearsQuestion(String text) {

        text = text.toLowerCase();

        return text.contains("how long") ||
                text.contains("how many years");
    }

    // =====================================================
    // PATTERN INVESTMENT
    // =====================================================

    public static boolean isPatternInvestment(String text) {

        text = text.toLowerCase();

        return

                text.contains("every month")

                        ||

                        text.contains("monthly")

                        ||

                        text.contains("sip")

                        ||

                        text.contains("recurring")

                        ||

                        text.contains("each month")

                        ||

                        text.contains("every year")

                        ||

                        text.contains("every week")

                        ||

                        text.contains("repeat") ||

                        text.contains("again and again");
    }

    // =====================================================
    // BREAKDOWN
    // =====================================================

    public static boolean isBreakdownRequest(String text) {

        text = text.toLowerCase();

        return

                text.contains("break this down") ||

                        text.contains("break it down") ||

                        text.contains("show steps") ||

                        text.contains("how did you get that") ||

                        text.contains("explain") ||

                        text.contains("why");
    }
}