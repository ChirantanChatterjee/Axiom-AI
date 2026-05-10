package com.axiomai.math.solver;

import com.axiomai.ai.intent.GreetingResponder;
import com.axiomai.ai.intent.IntentClassifier;
import com.axiomai.ai.preprocessing.NLToMath;
import com.axiomai.math.finance.InvestmentSolver;
import com.axiomai.math.finance.PatternInvestmentSolver;
import com.axiomai.service.DynamicQuestionGenerator;
import com.axiomai.service.Memory;
import com.axiomai.service.QuizQuestion;

public class MathSolver {

    public static String solve(String text) {
        System.out.println("DEBUG: Received text = [" + text + "]");

        if (text == null || text.isBlank()) {
            return "Say something and I’ll try to help.";
        }

        text = text.trim().toLowerCase();

        String intent = IntentClassifier.predict(text);

        System.out.println("DEBUG: Predicted intent = " + intent);

        switch (intent) {

            case "GREETING":
                return GreetingResponder.randomGreeting();

            case "CHITCHAT":
                return "Got it. What would you like to do next?";

            case "ASK_QUESTION":
                QuizQuestion q = DynamicQuestionGenerator.generate();
                Memory.quizActive = true;
                Memory.quizCorrectAnswer = q.answer;
                return q.question;

            case "QUIZ_CONTINUE":
                QuizQuestion q2 = DynamicQuestionGenerator.generate();
                Memory.quizActive = true;
                Memory.quizCorrectAnswer = q2.answer;
                return q2.question;

            case "QUIZ_STOP":
                Memory.quizActive = false;
                return GreetingResponder.randomGreeting();

            case "BREAKDOWN":
                if (Memory.lastBreakdown != null) {
                    return Memory.lastBreakdown;
                }
                return "I don't have a breakdown for the last calculation.";

            // inside switch(intent) in MathSolver.solve(...)
            case "ADVANCED_MATH":

                String advancedAnswer =
                        AdvancedMathSolver.solve(text);

                Memory.lastAnswer = advancedAnswer;

                Memory.lastBreakdown = """
            Detailed explanation for:

            """ + text + """

            AI Response:

            """ + advancedAnswer + """

            Step-by-step symbolic breakdown
            is still being expanded.
            """;

                return advancedAnswer;

            case "ARITHMETIC":
                return solveArithmetic(text);

            case "INVEST_SIMPLE":
                return InvestmentSolver.solveForward(text);

            case "INVEST_REQUIRED_PRINCIPAL":
                return InvestmentSolver.solveRequiredPrincipal(text);

            case "INVEST_YEARS":
                return InvestmentSolver.solveYears(text);

            case "INVEST_PATTERN":
                return PatternInvestmentSolver.solvePattern(text);

            default:
                return "I’m not sure what you mean, but I’m learning.";
        }
    }

    public static String solveArithmetic(String text) {
        String expr = NLToMath.convert(text);
        Double result = ExpressionSolver.solveExpression(expr);

        if (result == null) {
            return "I couldn't understand the math expression.";
        }

        String formatted = String.format("%,.10f", result)
                .replaceAll("\\.?0+$", "");

        String answer = "The result is " + formatted;
        Memory.lastAnswer = answer;
        return answer;
    }
}

