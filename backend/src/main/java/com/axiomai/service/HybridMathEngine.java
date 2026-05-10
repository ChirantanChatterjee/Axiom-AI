package com.axiomai.service;

import com.axiomai.api.response.MathResponse;
import com.axiomai.ai.intent.IntentClassifier;
import com.axiomai.math.solver.MathSolver;
import org.springframework.stereotype.Service;

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

        System.out.println("FINAL INTENT = " + intent);

        // =====================================================
        // STEP 2 — GRAPH ROUTES DIRECTLY TO SYMPY
        // =====================================================

        if (intent.equals("GRAPH")) {

            System.out.println(
                    "GRAPH DETECTED → ROUTING TO SYMPY"
            );

            return symPyService.solve(input);
        }

        // =====================================================
        // STEP 3 — TRY NATIVE ENGINE FIRST
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
        // STEP 5 — FALLBACK TO SYMPY
        // =====================================================

        if (nativeFailed) {

            System.out.println(
                    "NATIVE FAILED → FALLBACK TO SYMPY"
            );

            MathResponse sympyResponse =
                    symPyService.solve(input);

            if (sympyResponse != null &&
                    sympyResponse.getResult() != null) {

                return sympyResponse;
            }
        }

        // =====================================================
        // STEP 6 — RETURN NATIVE RESPONSE
        // =====================================================

        MathResponse response =
                new MathResponse();

        response.setResult(nativeAnswer);

        response.setLatex("");

        response.setSteps(
                java.util.Collections.emptyList()
        );

        response.setType("native");

        response.setGraph(null);

        return response;
    }
}