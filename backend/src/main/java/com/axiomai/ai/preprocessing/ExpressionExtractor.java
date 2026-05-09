package com.axiomai.ai.preprocessing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpressionExtractor {

    // =====================================================
    // EXPRESSION EXTRACTION
    // =====================================================

    public static String extractExpression(String text) {

        if (text == null) {
            return null;
        }

        String lower = text.toLowerCase();

        // -------------------------------------------------
        // DERIVATIVES / INTEGRALS
        // -------------------------------------------------

        String[] triggers = {
                "differentiate",
                "derivative of",
                "integrate",
                "integral of"
        };

        for (String t : triggers) {

            int idx = lower.indexOf(t);

            if (idx != -1) {

                String after =
                        text.substring(idx + t.length()).trim();

                after = after.replaceAll("with respect to x", "")
                        .replaceAll("dx", "")
                        .replaceAll("d x", "")
                        .trim();

                if (!after.isEmpty()) {
                    return after;
                }
            }
        }

        // -------------------------------------------------
        // LIMIT EXPRESSIONS
        // -------------------------------------------------

        // Example:
        // "limit as x approaches 0 of sin(x)/x"

        Pattern limitPattern =
                Pattern.compile("of\\s+(.+)",
                        Pattern.CASE_INSENSITIVE);

        Matcher m = limitPattern.matcher(text);

        if (m.find()) {

            String expr = m.group(1).trim();

            if (!expr.isEmpty()) {
                return expr;
            }
        }

        return null;
    }

    // =====================================================
    // LIMIT POINT EXTRACTION
    // =====================================================

    public static String extractLimitPoint(String text) {

        if (text == null) {
            return null;
        }

        String lower = text.toLowerCase();

        // -------------------------------------------------
        // x -> 0
        // x → 0
        // x approaches 0
        // x tends to 0
        // -------------------------------------------------

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