package com.axiomai.math.solver;

import com.axiomai.ai.preprocessing.ExpressionExtractor;

public class SimplifySolver {


    public static String solve(String text) {

        String expr =
                ExpressionExtractor.extractExpression(text);

        if (expr == null) {
            return "I couldn't detect the expression to simplify.";
        }

        return """
            Simplified expression:

            """ + expr;
    }

}
