package com.axiomai.ai.preprocessing;

public class NLToMath {

    public static String convert(String text) {
        text = text.toLowerCase();

        // Remove punctuation
        text = text.replace("?", " ")
                .replace(",", " ")
                .replace("=", " ");

        // Remove filler phrases
        text = text.replace("what is the result", " ")
                .replace("what is", " ")
                .replace("whats", " ")
                .replace("calculate", " ")
                .replace("compute", " ")
                .replace("help me with dividing", " ")
                .replace("i want to divide", " ")
                .replace("can you help me with dividing", " ")
                .replace("if i divide", " ")
                .replace("result of", " ")
                .trim();

        // Division phrases
        text = text.replace("divided by", "/")
                .replace("divide by", "/")
                .replace("divide with", "/")
                .replace("divided with", "/")
                .replace("dividing", "/")
                .replace("divide", "/");

        // Multiplication
        text = text.replace("multiplied by", "*")
                .replace("multiplied with", "*")
                .replace("times", "*");

        // Addition
        text = text.replace("plus", "+")
                .replace("add", "+");

        // Subtraction
        text = text.replace("minus", "-")
                .replace("subtract", "-");

        // Remove leftover English words
        text = text.replaceAll("[^0-9+\\-*/. ]", " ");

        // Collapse spaces
        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }
}
