package com.axiomai.service;

import com.axiomai.ai.routing.RuleRouter;
import com.axiomai.api.response.MathResponse;
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

        String intent =
                RuleRouter.detectIntent(input);

        Memory.lastIntent = intent;

        Memory.lastQuestion = input;

        // =====================================================
        // SYMBOLIC ROUTING
        // =====================================================

        if (

                intent.equals("GRAPH")

                        ||

                        intent.equals("ADVANCED_MATH")

        ) {

            MathResponse response =
                    symPyService.solve(input);

            if (response != null) {

                return response;
            }
        }

        // =====================================================
        // NATIVE ENGINE
        // =====================================================

        String nativeAnswer =
                MathSolver.solve(input);

        MathResponse response =
                new MathResponse();

        response.setResult(nativeAnswer);

        response.setLatex("");

        response.setSteps(
                Collections.emptyList()
        );

        response.setType("native");

        response.setGraph(null);

        return response;
    }

}