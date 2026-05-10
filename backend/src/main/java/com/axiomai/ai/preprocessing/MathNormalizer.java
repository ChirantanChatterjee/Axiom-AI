package com.axiomai.ai.preprocessing;

public class MathNormalizer {

    public static String normalize(String text) {

        if (text == null) {
            return "";
        }

        text = text.toLowerCase();

        text = text.replace("∫", " integral ");
        text = text.replace("√", " sqrt ");
        text = text.replace("π", " pi ");
        text = text.replace("∞", " infinity ");

        text = text.replaceAll("x2", "x^2");
        text = text.replaceAll("x3", "x^3");

        text = text.replaceAll("\\s+", " ");

        return text.trim();
    }
}