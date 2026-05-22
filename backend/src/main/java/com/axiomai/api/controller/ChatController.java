package com.axiomai.api.controller;

import com.axiomai.api.request.ChatRequest;
import com.axiomai.api.response.ChatResponse;
import com.axiomai.api.response.MathResponse;
import com.axiomai.service.HybridMathEngine;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final HybridMathEngine engine;

    public ChatController(
            HybridMathEngine engine
    ) {
        this.engine = engine;
    }

    @PostMapping
    public ChatResponse chat(
            @RequestBody ChatRequest request
    ) {

        MathResponse response =
                engine.solve(
                        request.getMessage()
                );

        return new ChatResponse(
                response.getResult(),
                response.getLatex(),
                response.getSteps(),
                response.getType(),
                response.getGraph()
        );
    }
}