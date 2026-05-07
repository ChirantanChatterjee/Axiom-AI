package com.axiomai.ai.intent;

public class IntentDetector {

    public static boolean isRepeat(String text) {
        text = text.toLowerCase();
        return text.contains("again") ||
                text.contains("repeat") ||
                text.contains("same again") ||
                text.contains("do it again");
    }

//    public static boolean isArithmetic(String text) {
//        text = text.toLowerCase();
//
//        // 1. Direct math operators with or without spaces
//        if (text.matches(".*\\d+\\s*[+\\-*/]\\s*\\d+.*")) return true;
//        if (text.matches(".*\\d+[+\\-*/]\\d+.*")) return true;
//
//        // 2. All natural-language division patterns
//        if (text.contains("divide") ||
//                text.contains("divided") ||
//                text.contains("dividing") ||
//                text.contains("divide by") ||
//                text.contains("divide with") ||
//                text.contains("help me with dividing") ||
//                text.contains("i want to divide") ||
//                text.contains("can you help me with dividing") ||
//                text.contains("if i divide") ||
//                text.contains("what is the result") ||
//                text.contains("result of")) {
//            return true;
//        }
//
//        // 3. Other arithmetic keywords
//        if (text.contains("plus") ||
//                text.contains("add") ||
//                text.contains("sum") ||
//                text.contains("minus") ||
//                text.contains("subtract") ||
//                text.contains("take away") ||
//                text.contains("times") ||
//                text.contains("multiply") ||
//                text.contains("multiplied") ||
//                text.contains("x") ||
//                text.contains("over") ||
//                text.contains("what is") ||
//                text.contains("whats") ||
//                text.contains("calculate") ||
//                text.contains("compute")) {
//            return true;
//        }
//
//        return false;
//    }

    public static boolean isArithmetic(String text) {

        text = text.toLowerCase();

        // ONLY simple arithmetic expressions

        if (text.matches("^\\s*\\d+\\s*[+\\-*/]\\s*\\d+\\s*$")) {
            return true;
        }

        // Simple arithmetic keywords only

        return text.contains("plus") ||
                text.contains("minus") ||
                text.contains("times") ||
                text.contains("multiplied by") ||
                text.contains("divided by") ||
                text.contains("addition") ||
                text.contains("subtraction") ||
                text.contains("multiplication") ||
                text.contains("division");
    }

    public static boolean isAdvancedMath(String text) {

        text = text.toLowerCase();

        return text.contains("^") ||
                text.contains("integer solutions") ||
                text.contains("theorem") ||
                text.contains("prove") ||
                text.contains("mod") ||
                text.contains("polynomial") ||
                text.contains("matrix") ||
                text.contains("vector") ||
                text.contains("integral") ||
                text.contains("derivative") ||
                text.contains("limit") ||
                text.contains("equation");
    }

    public static boolean isInvestment(String text) {
        text = text.toLowerCase();
        return text.contains("invest") ||
                text.contains("investment") ||
                text.contains("fund") ||
                text.contains("return") ||
                text.contains("interest") ||
                text.contains("growth");
    }

    public static boolean isPatternInvestment(String text) {
        text = text.toLowerCase();

        // Strong hints of multi-stage / pattern questions
        if (text.contains("pattern") ||
                text.contains("similar pattern") ||
                text.contains("for the last") ||
                text.contains("then") ||
                text.contains("after that") ||
                text.contains("again and again")) {
            return true;
        }

        // Mix of multiple percents and years often implies pattern
        int percentCount = text.split("%", -1).length - 1;
        int yearsCount = text.toLowerCase().split("years", -1).length - 1;
        return percentCount > 1 && yearsCount >= 1;
    }


    public static boolean isRequiredPrincipal(String text) {
        text = text.toLowerCase();
        return text.contains("how much") &&
                text.contains("invest") &&
                text.contains("to");
    }

    public static boolean isYearsQuestion(String text) {
        text = text.toLowerCase();
        return text.contains("how long") ||
                text.contains("how many years");
    }

    public static boolean isBreakdownRequest(String text) {
        text = text.toLowerCase();
        return text.contains("break it down") ||
                text.contains("show steps") ||
                text.contains("explain the steps") ||
                text.contains("how did you get that") ||
                text.contains("explain it");
    }

}
