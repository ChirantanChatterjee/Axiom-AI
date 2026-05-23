package com.axiomai.service;

import com.axiomai.api.response.MathResponse;
import com.axiomai.math.solver.MathSolver;
import com.axiomai.security.SensitiveLogSanitizer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;

@Service
public class AIService {

    private final SymPyService symPyService;

    public AIService(
            SymPyService symPyService
    ) {
        this.symPyService = symPyService;
    }

    public MathResponse process(String input) {

        System.out.println("================================");
        System.out.println(
                "USER INPUT = "
                        + SensitiveLogSanitizer.redact(input)
        );

        try {

            // =========================================
            // FIRST TRY NATIVE AI ENGINE
            // =========================================

            String nativeAnswer =
                    MathSolver.solve(input);

            System.out.println(
                    "NATIVE ENGINE RESPONSE = "
                            + nativeAnswer
            );

            // =========================================
            // IF NATIVE ENGINE UNDERSTANDS
            // =========================================

            if (nativeAnswer != null &&
                    !nativeAnswer.isBlank() &&
                    !nativeAnswer.contains("I’m not sure") &&
                    !nativeAnswer.contains("learning")) {

                MathResponse response =
                        new MathResponse();

                response.setResult(nativeAnswer);

                response.setLatex("");

                response.setSteps(new ArrayList<>());

                response.setType("native");

                response.setGraph(new HashMap<>());

                return response;
            }

            // =========================================
            // OTHERWISE FALLBACK TO SYMPY
            // =========================================

            System.out.println(
                    "FALLING BACK TO SYMPY..."
            );

            return symPyService.solve(input);

        } catch (Exception e) {

            System.out.println(
                    "[AI SERVICE] Request failed: "
                            + SensitiveLogSanitizer.redact(
                            e.getMessage()
                    )
            );

            MathResponse error =
                    new MathResponse();

            error.setResult(
                    "Axiom-AI encountered an internal error."
            );

            error.setLatex("");

            error.setSteps(new ArrayList<>());

            error.setType("error");

            error.setGraph(new HashMap<>());

            return error;
        }
    }
}
