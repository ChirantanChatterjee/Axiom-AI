package com.axiomai.service;

import com.axiomai.api.response.MathResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AIService {

    @Autowired
    private SymPyService symPyService;

    public MathResponse process(String userInput) {

        try {

            return symPyService.solve(userInput);

        } catch (Exception e) {

            e.printStackTrace();

            return new MathResponse(
                    "error",
                    "Something went wrong while processing.",
                    "",
                    null,
                    null
            );
        }
    }
}