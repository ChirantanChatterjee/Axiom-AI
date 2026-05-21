package com.axiomai.ai.routing;

public class RuleRouter {

    public static String detectIntent(String text) {

        if (text == null || text.isBlank()) {
            return "UNKNOWN";
        }

        text = text.toLowerCase();

        // =====================================================
        // GRAPH
        // =====================================================

        if (
                text.contains("graph")
                        ||
                        text.contains("plot")
        ) {

            return "GRAPH";
        }

        // =====================================================
        // ADVANCED MATH
        // =====================================================

        if (

                text.contains("integrate")

                        ||

                        text.contains("derivative")

                        ||

                        text.contains("limit")

                        ||

                        text.contains("solve equation")

                        ||

                        text.contains("matrix")

        ) {

            return "ADVANCED_MATH";
        }

        // =====================================================
        // INVESTMENT
        // =====================================================

        if (
                text.contains("invest")
                        ||
                        text.contains("sip")
                        ||
                        text.contains("compound")
        ) {

            return "INVEST_SIMPLE";
        }

        // =====================================================
        // QUIZ
        // =====================================================

        if (
                text.contains("quiz")
                        ||
                        text.contains("question")
        ) {

            return "ASK_QUESTION";
        }

        // =====================================================
        // GREETING
        // =====================================================

        if (

                text.equals("hi")

                        ||

                        text.equals("hello")

                        ||

                        text.equals("hey")

        ) {

            return "GREETING";
        }

        // =====================================================
        // ARITHMETIC
        // =====================================================

        if (
                text.matches(".*\\d+.*")
        ) {

            return "ARITHMETIC";
        }

        return "UNKNOWN";
    }

}