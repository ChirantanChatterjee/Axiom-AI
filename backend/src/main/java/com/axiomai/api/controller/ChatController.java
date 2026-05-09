package com.axiomai.api.controller;

import com.axiomai.api.request.ChatRequest;
import com.axiomai.api.response.MathResponse;
import com.axiomai.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@CrossOrigin("*")
public class ChatController {

    @Autowired
    private AIService aiService;

    @PostMapping
    public MathResponse chat(
            @RequestBody ChatRequest request
    ) {

        return aiService.process(
                request.getMessage()
        );
    }
}