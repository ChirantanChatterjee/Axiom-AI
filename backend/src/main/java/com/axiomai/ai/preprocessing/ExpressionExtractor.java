package com.axiomai.ai.preprocessing;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpressionExtractor {


// =====================================================
// GENERIC CLEANER
// =====================================================

    private static final String[] PHRASES = {

            // derivatives
            "find the derivative of",
            "find derivative of",
            "derivative of",
            "differentiate",

            // integrals
            "find the integral of",
            "find integral of",
            "integral of",
            "integrate",

            // simplify
            "simplify the expression",
            "simplify",

            // graph
            "plot the graph of",
            "show me the graph of",
            "what is the graph of",
            "graph of",
            "plot",

            // generic
            "what is",
            "what's",
            "show me",
            "can you",
            "please"
    };

// =====================================================
// MAIN EXTRACTION
// =====================================================

    public static String extractExpression(String text) {

        if (text == null || text.isBlank()) {
            return null;
        }

        text = text.toLowerCase().trim();

        // longest phrases first
        Arrays.sort(
                PHRASES,
                (a, b) -> Integer.compare(
                        b.length(),
                        a.length()
                )
        );

        for (String p : PHRASES) {

            text = text.replace(p, " ");
        }

        // cleanup
        text = text.replaceAll("y\\s*=", "");
        text = text.replaceAll("with respect to x", "");
        text = text.replaceAll("\\bdx\\b", "");

        text = text.replace("^", "^");

        text = text.replaceAll("\\s+", " ");

        text = text.trim();

        if (text.isBlank()) {
            return null;
        }

        return text;
    }

// =====================================================
// LIMIT POINT EXTRACTION
// =====================================================

    public static String extractLimitPoint(String text) {

        if (text == null) {
            return null;
        }

        String lower = text.toLowerCase();

        Pattern p = Pattern.compile(
                "(?:->|→|approaches|tends to)\\s*([a-zA-Z0-9+\\-\\.∞infinity]+)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher m = p.matcher(lower);

        if (m.find()) {

            String point = m.group(1).trim();

            if (!point.isEmpty()) {
                return point;
            }
        }

        return null;
    }
}
