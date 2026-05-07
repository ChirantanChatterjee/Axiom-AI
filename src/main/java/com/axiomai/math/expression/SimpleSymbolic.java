package com.axiomai.math.expression;

public class SimpleSymbolic {

    // This is intentionally very limited; you can later replace with a real CAS.

    public static String derivative(String expr) {
        String e = expr.replaceAll("\\s+", "");

        if (e.equals("x")) return "1";
        if (e.equals("x^2")) return "2x";
        if (e.equals("x^3")) return "3x^2";
        if (e.equals("sin(x)")) return "cos(x)";
        if (e.equals("cos(x)")) return "-sin(x)";
        if (e.equals("e^x") || e.equals("exp(x)")) return "e^x";

        return "d/dx(" + expr + ") (symbolic engine stub)";
    }

    public static String integral(String expr) {
        String e = expr.replaceAll("\\s+", "");

        if (e.equals("x")) return "x^2/2";
        if (e.equals("x^2")) return "x^3/3";
        if (e.equals("1/x")) return "ln|x|";
        if (e.equals("sin(x)")) return "-cos(x)";
        if (e.equals("cos(x)")) return "sin(x)";
        if (e.equals("e^x") || e.equals("exp(x)")) return "e^x";

        return "∫(" + expr + ") dx (symbolic engine stub)";
    }

    public static String limit(String expr, String point) {
        String e = expr.replaceAll("\\s+", "");
        if (e.equals("sin(x)/x") && (point.equals("0") || point.equals("0.0"))) {
            return "1";
        }
        return "lim_{x→" + point + "} " + expr + " (limit engine stub)";
    }
}
