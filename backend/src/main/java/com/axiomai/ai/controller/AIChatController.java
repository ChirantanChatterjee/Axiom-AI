package com.axiomai.ai.controller;

import com.axiomai.ai.dto.AIResponse;
import com.axiomai.ai.dto.ChatRequest;
import com.axiomai.ai.service.AIOrchestratorService;
import com.axiomai.workspace.WorkspaceAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")

@RequiredArgsConstructor
public class AIChatController {

    private final AIOrchestratorService
            aiOrchestratorService;

    private final WorkspaceAccessService
            workspaceAccessService;

    @PostMapping("/chat")

    public AIResponse chat(
            @RequestBody ChatRequest request,
            @RequestHeader("X-AIF-Session") String token
    ) {

        String sessionId =
                workspaceAccessService.bindToCurrentUser(
                        token,
                        request.getSessionId()
                );

        return aiOrchestratorService
                .processMessage(
                        request.getMessage(),
                        sessionId,
                        request.getWebsiteUrl(),
                        request.getDomainName(),
                        request.getFrameworkLocked()
                );
    }

}
