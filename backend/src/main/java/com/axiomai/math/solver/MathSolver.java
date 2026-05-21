package com.axiomai.math.solver;

import com.axiomai.ai.preprocessing.NLToMath;
import com.axiomai.ai.routing.GreetingResponder;
import com.axiomai.ai.routing.RuleRouter;
import com.axiomai.math.finance.InvestmentSolver;
import com.axiomai.service.DynamicQuestionGenerator;
import com.axiomai.service.Memory;
import com.axiomai.service.QuizQuestion;

public class MathSolver {

    public static String solve(String text) {

        if (text == null || text.isBlank()) {

            return "Say something and I’ll try to help.";
        }

        text = text.trim().toLowerCase();

        Memory.lastQuestion = text;

        String intent =
                RuleRouter.detectIntent(text);

        Memory.lastIntent = intent;

        switch (intent) {

            case "GREETING":

                return GreetingResponder.randomGreeting();

            case "ASK_QUESTION": {

                QuizQuestion q =
                        DynamicQuestionGenerator.generate();

                Memory.quizActive = true;

                Memory.quizCorrectAnswer = q.answer;

                return q.question;
            }

            case "ADVANCED_MATH":

                return AdvancedMathSolver.solve(text);

            case "INVEST_SIMPLE":

                return InvestmentSolver.solveForward(text);

            case "GRAPH":

                return "GRAPH";

            case "ARITHMETIC":

                return solveArithmetic(text);

            default:

                return """
                I’m not sure what you mean yet.
                """;
        }
    }

    public static String solveArithmetic(String text) {

        String expr =
                NLToMath.convert(text);

        Double result =
                ExpressionSolver
                        .solveExpression(expr);

        if (result == null) {

            return """
            I couldn't understand
            the math expression.
            """;
        }

        String formatted =
                String.format("%,.10f", result)
                        .replaceAll("\\.?0+$", "");

        return "The result is " + formatted;
    }

}