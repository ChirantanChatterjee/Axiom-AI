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

        System.out.println(
                "DEBUG: Received text = [" + text + "]"
        );

        // =====================================================
        // EMPTY INPUT
        // =====================================================

        if (text == null || text.isBlank()) {

            return "Say something and I’ll try to help.";
        }

        text = text.trim().toLowerCase();

        Memory.lastQuestion = text;

        // =====================================================
        // QUIZ ANSWER HANDLING
        // =====================================================

        if (Memory.quizActive) {

            System.out.println("QUIZ INTERCEPTED");

            // -------------------------------------------------
            // NUMERIC ANSWERS
            // -------------------------------------------------

            if (text.matches("-?\\d+(\\.\\d+)?")) {

                try {

                    double userAnswer =
                            Double.parseDouble(text);

                    double correctAnswer =
                            Double.parseDouble(
                                    String.valueOf(
                                            Memory.quizCorrectAnswer
                                    )
                            );

                    // =========================================
                    // CORRECT
                    // =========================================

                    if (Math.abs(
                            userAnswer - correctAnswer
                    ) < 0.0001) {

                        Memory.quizActive = false;

                        Memory.awaitingQuizConfirmation = true;

                        Memory.lastBreakdown =
                                "The quiz answer matched the expected numerical result.";

                        return """
                        Correct! 🎉

                        Type:
                        • another
                        • one more
                        • yes

                        for another question.
                        """;
                    }

                    // =========================================
                    // WRONG
                    // =========================================

                    return "Not quite. Try again.";

                } catch (Exception e) {

                    e.printStackTrace();

                    return "Something went wrong.";
                }
            }

            // -------------------------------------------------
            // STOP QUIZ
            // -------------------------------------------------

            if (text.equals("stop") ||
                    text.equals("quit") ||
                    text.equals("exit")) {

                clearQuizContext();

                return """
                Quiz stopped.

                What would you like to do next?
                """;
            }

            // -------------------------------------------------
            // BLOCK RANDOM TEXT
            // -------------------------------------------------

            return """
            Please answer the quiz question first.

            Or type:
            • stop
            """;
        }

        // =====================================================
        // INTENT DETECTION
        // =====================================================

        String intent =
                IntentClassifier.predict(text);

        Memory.lastIntent = intent;

        System.out.println(
                "FINAL INTENT = " + intent
        );

        // =====================================================
        // ROUTING
        // =====================================================

        switch (intent) {

            // =================================================
            // GREETING
            // =================================================

            case "GREETING": {

                String greeting =
                        GreetingResponder.randomGreeting();

                Memory.awaitingQuizConfirmation =
                        GreetingResponder
                                .lastGreetingWasInteractive;

                Memory.lastAnswer = greeting;

                return greeting;
            }

            // =================================================
            // CHITCHAT
            // =================================================

            case "CHITCHAT":

                clearQuizContext();

                Memory.lastBreakdown =
                        "General conversational interaction.";

                return "Got it. What would you like to do next?";

            // =================================================
            // ASK QUESTION
            // =================================================

            case "ASK_QUESTION": {

                QuizQuestion q =
                        DynamicQuestionGenerator.generate();

                Memory.quizActive = true;

                Memory.awaitingQuizConfirmation = false;

                Memory.quizCorrectAnswer = q.answer;

                Memory.lastBreakdown =
                        "Quiz mode activated.";

                return q.question;
            }

            // =================================================
            // QUIZ CONTINUE
            // =================================================

            case "QUIZ_CONTINUE": {

                QuizQuestion q2 =
                        DynamicQuestionGenerator.generate();

                Memory.quizActive = true;

                Memory.awaitingQuizConfirmation = false;

                Memory.quizCorrectAnswer = q2.answer;

                Memory.lastBreakdown =
                        "Generated another quiz question.";

                return q2.question;
            }

            // =================================================
            // QUIZ STOP
            // =================================================

            case "QUIZ_STOP":

                clearQuizContext();

                return """
                Alright 👍

                Let me know if you'd like:
                • math solving
                • graph plotting
                • symbolic calculus
                • another quiz
                """;

            // =================================================
            // BREAKDOWN
            // =================================================

            case "BREAKDOWN":

                clearQuizContext();

                // =============================================
                // SYMBOLIC STEPS
                // =============================================

                if (

                        Memory.lastSteps != null

                                &&

                                !Memory.lastSteps.isEmpty()

                ) {

                    StringBuilder sb =
                            new StringBuilder();

                    sb.append(
                            "Here is the breakdown:\n\n"
                    );

                    for (String step : Memory.lastSteps) {

                        sb.append(step)
                                .append("\n");
                    }

                    return sb.toString();
                }

                // =============================================
                // TEXT BREAKDOWN
                // =============================================

                if (Memory.lastBreakdown != null) {

                    return Memory.lastBreakdown;
                }

                return "There is nothing recent to explain.";

            // =================================================
            // ADVANCED MATH
            // =================================================

            case "ADVANCED_MATH": {

                clearQuizContext();

                String advancedAnswer =
                        AdvancedMathSolver.solve(text);

                Memory.lastAnswer =
                        advancedAnswer;

                Memory.lastBreakdown =
                        "Advanced symbolic math operation performed.";

                return advancedAnswer;
            }

            // =================================================
            // ARITHMETIC
            // =================================================

            case "ARITHMETIC":

                clearQuizContext();

                return solveArithmetic(text);

            // =================================================
            // INVEST SIMPLE
            // =================================================

            case "INVEST_SIMPLE": {

                clearQuizContext();

                String response =
                        InvestmentSolver.solveForward(text);

                Memory.lastAnswer = response;

                Memory.lastBreakdown =
                        "Calculated compound investment growth using principal, rate, and time.";

                return response;
            }

            // =================================================
            // INVEST REQUIRED PRINCIPAL
            // =================================================

            case "INVEST_REQUIRED_PRINCIPAL": {

                clearQuizContext();

                String response =
                        InvestmentSolver
                                .solveRequiredPrincipal(text);

                Memory.lastAnswer = response;

                Memory.lastBreakdown =
                        "Calculated required principal needed to reach a target investment value.";

                return response;
            }

            // =================================================
            // INVEST YEARS
            // =================================================

            case "INVEST_YEARS": {

                clearQuizContext();

                String response =
                        InvestmentSolver.solveYears(text);

                Memory.lastAnswer = response;

                Memory.lastBreakdown =
                        "Calculated required investment duration using compound growth.";

                return response;
            }

            // =================================================
            // INVEST PATTERN
            // =================================================

            case "INVEST_PATTERN": {

                clearQuizContext();

                String response =
                        PatternInvestmentSolver
                                .solvePattern(text);

                Memory.lastAnswer = response;

                Memory.lastBreakdown =
                        "Calculated recurring investment growth pattern.";

                return response;
            }

            // =================================================
            // GRAPH
            // =================================================

            case "GRAPH":

                clearQuizContext();

                Memory.lastBreakdown =
                        "Generated mathematical graph visualization.";

                return "GRAPH";

            // =================================================
            // DEFAULT
            // =================================================

            default:

                clearQuizContext();

                return """
                I’m not sure what you mean,
                but I’m learning.
                """;
        }
    }

    // =====================================================
    // CLEAR QUIZ CONTEXT
    // =====================================================

    private static void clearQuizContext() {

        Memory.quizActive = false;

        Memory.awaitingQuizConfirmation = false;
    }

    // =====================================================
    // ARITHMETIC
    // =====================================================

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

        String answer =
                "The result is " + formatted;

        Memory.lastAnswer = answer;

        Memory.lastBreakdown =
                "Solved arithmetic expression using native expression parser.";

        return answer;
    }
}