package com.axiomai.api.controller;

import com.axiomai.api.request.ChatRequest;
import com.axiomai.api.response.ChatResponse;
import com.axiomai.math.solver.MathSolver;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@CrossOrigin("*")
public class ChatController {

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {

        String result =
                MathSolver.solve(request.getMessage());

        return new ChatResponse(result);
    }
}
