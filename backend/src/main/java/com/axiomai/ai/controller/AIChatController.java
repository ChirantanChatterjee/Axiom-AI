package com.axiomai.ai.controller;

import com.axiomai.ai.dto.AIResponse;
import com.axiomai.ai.dto.ChatRequest;
import com.axiomai.ai.service.AIOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")

@RequiredArgsConstructor

@CrossOrigin("*")

public class AIChatController {

    private final AIOrchestratorService
            aiOrchestratorService;

    @PostMapping("/chat")

    public AIResponse chat(
            @RequestBody ChatRequest request
    ) {

        return aiOrchestratorService
                .processMessage(
                        request.getMessage(),
                        request.getSessionId(),
                        request.getWebsiteUrl(),
                        request.getDomainName(),
                        request.getFrameworkLocked()
                );
    }

}
