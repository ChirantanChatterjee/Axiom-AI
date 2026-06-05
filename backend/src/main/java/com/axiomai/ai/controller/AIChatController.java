package com.axiomai.ai.controller;

import com.axiomai.ai.dto.AIResponse;
import com.axiomai.ai.dto.ChatRequest;
import com.axiomai.ai.service.AIOrchestratorService;
import com.axiomai.workspace.WorkspaceAccessService;
import com.axiomai.workspace.WorkspaceChatSessionDto;
import com.axiomai.workspace.WorkspaceChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")

@RequiredArgsConstructor
@Slf4j
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
                        request.getFrameworkLocked(),
                        request.getIntent(),
                        request.getVariables()
                );

        try {

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

        } catch (Exception e) {

            log.warn(
                    "Unable to persist chat session {} after AI response.",
                    sessionId,
                    e
            );
        }

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

        Map<String, String> maskedVariables =
                maskedVariableSummary(
                        request.getVariables()
                );

        userMessage.put("sender", "user");
        userMessage.put(
                "text",
                maskedVariables.isEmpty()
                        ? request.getMessage()
                        : structuredVariableMessage(maskedVariables)
        );

        if (
                !maskedVariables.isEmpty()
        ) {

            userMessage.put("type", "variables");
            userMessage.put(
                    "data",
                    maskedVariables
            );
        }

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

    private Map<String, String> maskedVariableSummary(
            Map<String, String> variables
    ) {

        Map<String, String> summary =
                new LinkedHashMap<>();

        if (
                variables == null
                        ||
                        variables.isEmpty()
        ) {

            return summary;
        }

        variables.keySet()
                .stream()
                .filter(variable ->
                        variable != null
                                &&
                                !variable.isBlank()
                )
                .forEach(variable -> summary.put(
                        variable,
                        "Saved"
                ));

        return summary;
    }

    private String structuredVariableMessage(
            Map<String, String> maskedVariables
    ) {

        return "Submitted runtime values for "
                + String.join(
                        ", ",
                        maskedVariables.keySet()
                )
                + ".";
    }

}
