package com.axiomai.service;

import com.axiomai.api.response.MathResponse;
import com.axiomai.ai.intent.IntentClassifier;
import com.axiomai.math.solver.MathSolver;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class HybridMathEngine {

    private final SymPyService symPyService;

    public HybridMathEngine(
            SymPyService symPyService
    ) {
        this.symPyService = symPyService;
    }

    public MathResponse solve(String input) {

        System.out.println("================================");
        System.out.println("USER INPUT = " + input);

        // =====================================================
        // STEP 1 — DETECT INTENT
        // =====================================================

        String intent =
                IntentClassifier.predict(input);

        System.out.println(
                "FINAL INTENT = " + intent
        );

        // STORE LAST INTENT
        Memory.lastIntent = intent;

        // STORE LAST QUESTION
        Memory.lastQuestion = input;

        // =====================================================
        // STEP 2 — SYMBOLIC ROUTING
        // =====================================================

        if (

                intent.equals("GRAPH")

                        ||

                        intent.equals("ADVANCED_MATH")

        ) {

            System.out.println(
                    "SYMBOLIC INTENT DETECTED → ROUTING TO SYMPY"
            );

            MathResponse sympyResponse =
                    symPyService.solve(input);

            // =====================================================
            // SAFETY CHECK
            // =====================================================

            if (
                    sympyResponse != null
                            &&
                            (
                                    sympyResponse.getLatex() != null
                                            ||
                                            sympyResponse.getResult() != null
                            )
            ) {

                // =================================================
                // STORE MEMORY
                // =================================================

                Memory.lastAnswer =
                        sympyResponse.getResult();

                Memory.lastLatex =
                        sympyResponse.getLatex();

                Memory.lastSteps =
                        sympyResponse.getSteps();

                Memory.lastType =
                        sympyResponse.getType();

                Memory.lastGraph =
                        sympyResponse.getGraph();

                // =================================================
                // BREAKDOWN MEMORY
                // =================================================

                if (

                        sympyResponse.getSteps() != null

                                &&

                                !sympyResponse.getSteps().isEmpty()

                ) {

                    StringBuilder breakdown =
                            new StringBuilder();

                    breakdown.append(
                            "Symbolic solution steps:\n\n"
                    );

                    for (String step :
                            sympyResponse.getSteps()) {

                        breakdown.append(step)
                                .append("\n");
                    }

                    Memory.lastBreakdown =
                            breakdown.toString();

                } else {

                    Memory.lastBreakdown =
                            "Symbolic operation completed.";
                }

                return sympyResponse;
            }

            System.out.println(
                    "SYMPY FAILED → RETURNING ERROR RESPONSE"
            );

            MathResponse error =
                    new MathResponse();

            error.setType("error");

            error.setResult(
                    "Symbolic engine failed to solve the problem."
            );

            error.setLatex("");

            error.setSteps(
                    Collections.emptyList()
            );

            error.setGraph(null);

            return error;
        }

        // =====================================================
        // STEP 3 — NATIVE ENGINE
        // =====================================================

        String nativeAnswer =
                MathSolver.solve(input);

        System.out.println(
                "NATIVE ENGINE RESPONSE = "
                        + nativeAnswer
        );

        // =====================================================
        // STEP 4 — FAILURE DETECTION
        // =====================================================

        boolean nativeFailed =

                nativeAnswer == null ||

                        nativeAnswer.isBlank() ||

                        nativeAnswer.contains("I couldn't") ||

                        nativeAnswer.contains("not sure") ||

                        nativeAnswer.contains("under development");

        // =====================================================
        // STEP 5 — GENERIC FALLBACK
        // =====================================================

        if (nativeFailed) {

            System.out.println(
                    "NATIVE FAILED → TRYING SYMPY FALLBACK"
            );

            MathResponse sympyResponse =
                    symPyService.solve(input);

            if (
                    sympyResponse != null
                            &&
                            (
                                    sympyResponse.getLatex() != null
                                            ||
                                            sympyResponse.getResult() != null
                            )
            ) {

                // =================================================
                // STORE MEMORY
                // =================================================

                Memory.lastAnswer =
                        sympyResponse.getResult();

                Memory.lastLatex =
                        sympyResponse.getLatex();

                Memory.lastSteps =
                        sympyResponse.getSteps();

                Memory.lastType =
                        sympyResponse.getType();

                Memory.lastGraph =
                        sympyResponse.getGraph();

                // =================================================
                // BREAKDOWN MEMORY
                // =================================================

                if (

                        sympyResponse.getSteps() != null

                                &&

                                !sympyResponse.getSteps().isEmpty()

                ) {

                    StringBuilder breakdown =
                            new StringBuilder();

                    breakdown.append(
                            "Fallback symbolic solution:\n\n"
                    );

                    for (String step :
                            sympyResponse.getSteps()) {

                        breakdown.append(step)
                                .append("\n");
                    }

                    Memory.lastBreakdown =
                            breakdown.toString();

                } else {

                    Memory.lastBreakdown =
                            "Fallback symbolic operation completed.";
                }

                return sympyResponse;
            }

            MathResponse error =
                    new MathResponse();

            error.setType("error");

            error.setResult(
                    "Axiom-AI could not solve this problem."
            );

            error.setLatex("");

            error.setSteps(
                    Collections.emptyList()
            );

            error.setGraph(null);

            return error;
        }

        // =====================================================
        // STEP 6 — RETURN NATIVE RESPONSE
        // =====================================================

        MathResponse response =
                new MathResponse();

        response.setResult(nativeAnswer);

        response.setLatex("");

        response.setSteps(
                Collections.emptyList()
        );

        response.setType("native");

        response.setGraph(null);

        // =====================================================
        // STORE MEMORY
        // =====================================================

        Memory.lastAnswer =
                nativeAnswer;

        Memory.lastLatex =
                "";

        Memory.lastSteps =
                Collections.emptyList();

        Memory.lastType =
                "native";

        Memory.lastGraph =
                null;

        return response;
    }
}