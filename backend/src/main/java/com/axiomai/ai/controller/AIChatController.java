package com.axiomai.ai.controller;

import com.axiomai.ai.dto.AIResponse;
import com.axiomai.ai.dto.ChatRequest;
import com.axiomai.ai.service.AIOrchestratorService;
import com.axiomai.workspace.WorkspaceAccessService;
import com.axiomai.workspace.WorkspaceChatSessionDto;
import com.axiomai.workspace.WorkspaceChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")

@RequiredArgsConstructor
public class AIChatController {

    private final AIOrchestratorService
            aiOrchestratorService;

    private final WorkspaceAccessService
            workspaceAccessService;

    private final WorkspaceChatSessionService
            workspaceChatSessionService;

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

        AIResponse response =
                aiOrchestratorService
                .processMessage(
                        request.getMessage(),
                        sessionId,
                        request.getWebsiteUrl(),
                        request.getDomainName(),
                        request.getFrameworkLocked()
                );

        workspaceChatSessionService.appendMessagesForCurrentUser(
                token,
                sessionId,
                WorkspaceChatSessionDto.builder()
                        .websiteUrl(request.getWebsiteUrl())
                        .domainName(request.getDomainName())
                        .frameworkLocked(Boolean.TRUE.equals(
                                request.getFrameworkLocked()
                        ))
                        .build(),
                chatTurnMessages(
                        request,
                        response
                )
        );

        return response;
    }

    private List<Map<String, Object>> chatTurnMessages(
            ChatRequest request,
            AIResponse response
    ) {

        List<Map<String, Object>> messages =
                new ArrayList<>();

        Map<String, Object> userMessage =
                new LinkedHashMap<>();

        userMessage.put("sender", "user");
        userMessage.put("text", request.getMessage());
        messages.add(userMessage);

        Map<String, Object> aiMessage =
                new LinkedHashMap<>();

        aiMessage.put("sender", "ai");
        aiMessage.put("text", response.getMessage());
        aiMessage.put("type", response.getType());
        aiMessage.put("data", response.getData());
        aiMessage.put("downloadUrl", response.getDownloadUrl());
        aiMessage.put("reportUrl", response.getReportUrl());
        messages.add(aiMessage);

        return messages;
    }

}
