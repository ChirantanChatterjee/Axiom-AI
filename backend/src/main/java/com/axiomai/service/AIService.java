package com.axiomai.service;

import com.axiomai.math.solver.MathSolver;

public class AIService {

    public String process(String userInput) {

        try {

            return MathSolver.solve(userInput);

        } catch (Exception e) {

            e.printStackTrace();

            return "Something went wrong while processing.";

        }
    }
}