package com.axiomai.ai.service;

import com.axiomai.ai.dto.AICommand;
import com.axiomai.ai.dto.AIResponse;
import com.axiomai.ai.intent.IntentParser;
import com.axiomai.ai.orchestrator.AICommandOrchestrator;
import com.axiomai.core.memory.ExecutionMemoryService;
import com.axiomai.core.session.ExecutionSession;
import com.axiomai.workspace.AutomationSession;
import com.axiomai.workspace.AutomationWorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j

public class AIOrchestratorService {

    private final IntentParser
            intentParser;

    private final AICommandOrchestrator
            orchestrator;

    private final ExecutionMemoryService
            executionMemoryService;

    private final AutomationWorkspaceService
            automationWorkspaceService;

    // =====================================================
    // PROCESS MESSAGE
    // =====================================================

    public AIResponse processMessage(
            String message
    ) {

        return processMessage(
                message,
                null,
                null,
                null,
                null
        );
    }

    public AIResponse processMessage(

            String message,

            String sessionId

    ) {

        return processMessage(
                message,
                sessionId,
                null,
                null,
                null
        );
    }

    public AIResponse processMessage(

            String message,

            String sessionId,

            String websiteUrl,

            String domainName,

            Boolean frameworkLocked

    ) {

        String userId =
                resolveUserId(sessionId);

        // =================================================
        // SESSION
        // =================================================

        ExecutionSession session =

                executionMemoryService
                        .getOrCreateSession(
                                userId
                        );

        AutomationSession workspace =

                automationWorkspaceService
                        .getOrCreateSession(
                                userId
                        );

        restoreClientSessionContext(
                userId,
                workspace,
                websiteUrl,
                domainName,
                frameworkLocked
        );

        // =================================================
        // PARSE
        // =================================================

        AICommand command =
                intentParser.parse(message);

        command.setUserId(userId);

        // =================================================
        // NORMALIZATION
        // =================================================

        normalizeCommand(
                command,
                message
        );

        // =================================================
        // CONVERSATIONAL RECOVERY
        // =================================================

        recoverConversationalIntent(
                command,
                message,
                session,
                workspace
        );

        // =================================================
        // SESSION ENRICHMENT
        // =================================================

        enrichCommandFromSession(
                command,
                session,
                workspace
        );

        // =================================================
        // UPDATE MEMORY
        // =================================================

        executionMemoryService
                .updateIntent(

                        userId,

                        command.getIntent()
                );

        // =================================================
        // EXECUTE
        // =================================================

        try {

            return orchestrator.execute(
                    command
            );

        } catch (RuntimeException e) {

            log.warn(
                    "AIF command failed: {}",
                    e.getMessage(),
                    e
            );

            return AIResponse.builder()
                    .success(false)
                    .type("error")
                    .message(
                            userFriendlyMessage(e)
                    )
                    .build();
        }
    }

    private String userFriendlyMessage(
            RuntimeException e
    ) {

        String message =
                e.getMessage();

        if (
                message == null
                        ||
                        message.isBlank()
        ) {

            return "AIF could not complete that request. Please try again or regenerate the framework for this chat.";
        }

        return message;
    }

    private String resolveUserId(
            String sessionId
    ) {

        if (
                sessionId == null
                        ||
                        sessionId.isBlank()
        ) {

            return "default-user";
        }

        String normalized =
                sessionId.trim()
                        .replaceAll(
                                "[^A-Za-z0-9._-]",
                                "-"
                        );

        return normalized.isBlank()
                ? "default-user"
                : normalized;
    }

    private void restoreClientSessionContext(

            String userId,

            AutomationSession workspace,

            String websiteUrl,

            String domainName,

            Boolean frameworkLocked

    ) {

        if (
                !Boolean.TRUE.equals(frameworkLocked)
                        ||
                        websiteUrl == null
                        ||
                        websiteUrl.isBlank()
                        ||
                        workspace.getWebsiteUrl() != null
        ) {

            return;
        }

        automationWorkspaceService
                .setWebsite(
                        userId,
                        websiteUrl
                );

        automationWorkspaceService
                .setDomain(
                        userId,
                        domainName == null
                                ||
                                domainName.isBlank()
                                ? extractDomain(websiteUrl)
                                : domainName
                );
    }

    // =====================================================
    // NORMALIZATION
    // =====================================================

    private void normalizeCommand(

            AICommand command,

            String message

    ) {

        // =================================================
        // URL FIX
        // =================================================

        if (

                isGenerationIntent(command)

                        &&

                        (
                                command.getUrl() == null
                                        ||
                                        command.getUrl().isBlank()
                        )

                        &&

                        command.getFlowName() != null
                        &&
                        command.getFlowName()
                                .startsWith("http")

        ) {

            command.setUrl(
                    command.getFlowName()
            );
        }

        if (

                isGenerationIntent(command)

                        &&

                        (
                                command.getUrl() == null
                                        ||
                                        command.getUrl().isBlank()
                        )

        ) {

            String extractedUrl =
                    extractUrl(message);

            if (
                    extractedUrl != null
            ) {

                command.setUrl(extractedUrl);
            }
        }

        // =================================================
        // TARGET FIX
        // =================================================

        if (

                (
                        command.getTarget() == null
                                ||
                                command.getTarget().isBlank()
                )

                        &&

                        command.getFlowName() != null
                        &&
                        !command.getFlowName().isBlank()

        ) {

            command.setTarget(
                    command.getFlowName()
            );
        }

        if (
                command.getFeatureName() == null
                        ||
                        command.getFeatureName().isBlank()
        ) {

            command.setFeatureName(
                    extractFeatureName(message)
            );
        }

        command.setMessage(message);
    }

    // =====================================================
    // CONVERSATIONAL RECOVERY
    // =====================================================

    private void recoverConversationalIntent(

            AICommand command,

            String message,

            ExecutionSession session,

            AutomationSession workspace

    ) {

        String lower =
                message.toLowerCase();

        // =================================================
        // UPDATE TEST DATA
        // =================================================

        if (

                "UNKNOWN".equalsIgnoreCase(
                        command.getIntent()
                )

                        &&

                        command.getVariables() != null
                        &&
                        !command.getVariables()
                                .isEmpty()

        ) {

            command.setIntent(
                    "UPDATE_TEST_DATA"
            );

            return;
        }

        // =================================================
        // DOWNLOAD FRAMEWORK
        // =================================================

        if (

                "UNKNOWN".equalsIgnoreCase(
                        command.getIntent()
                )

                        &&

                        (
                                lower.contains("download")
                                        ||
                                        lower.contains("zip")
                        )

        ) {

            command.setIntent(
                    "DOWNLOAD_FRAMEWORK"
            );

            command.setArtifactName(
                    "framework"
            );

            return;
        }

        // =================================================
        // EXECUTE IT / EXECUTE FEATURE
        // =================================================

        if (

                "UNKNOWN".equalsIgnoreCase(
                        command.getIntent()
                )

                        &&

                        (
                                lower.contains("execute")
                                        ||
                                        lower.contains("run")
                                        ||
                                        lower.contains("start")
                        )

        ) {

            if (
                    lower.contains("feature")
                            ||
                            workspace.getActiveFeature() != null
            ) {

                command.setIntent(
                        "EXECUTE_FEATURE"
                );

                command.setFeatureName(
                        workspace.getActiveFeature()
                );

            } else {

                command.setIntent(
                        "EXECUTE_FLOW"
                );

                command.setTarget(
                        session.getActiveFlowName()
                );
            }

            return;
        }

        // =================================================
        // SHOW REPORT
        // =================================================

        if (

                "UNKNOWN".equalsIgnoreCase(
                        command.getIntent()
                )

                        &&

                        lower.contains("report")

        ) {

            command.setIntent(
                    "SHOW_REPORT"
            );

            return;
        }

        // =================================================
        // GENERATE FRAMEWORK
        // =================================================

        if (

                "UNKNOWN".equalsIgnoreCase(
                        command.getIntent()
                )

                        &&

                        lower.contains("generate")
                        &&
                        lower.contains("framework")

        ) {

            command.setIntent(
                    "GENERATE_FRAMEWORK"
            );
        }

        // =================================================
        // GENERATE FEATURE
        // =================================================

        if (

                "UNKNOWN".equalsIgnoreCase(
                        command.getIntent()
                )

                        &&

                        lower.contains("generate")
                        &&
                        (
                                lower.contains("feature")
                                        ||
                                        lower.contains("scenario")
                                        ||
                                        lower.contains("test")
                        )

        ) {

            command.setIntent(
                    "GENERATE_FEATURE"
            );

            command.setFeatureName(
                    extractFeatureName(message)
            );
        }
    }

    // =====================================================
    // SESSION ENRICHMENT
    // =====================================================

    private void enrichCommandFromSession(

            AICommand command,

            ExecutionSession session,

            AutomationSession workspace

    ) {

        // =================================================
        // TARGET
        // =================================================

        if (

                (
                        command.getTarget() == null
                                ||
                                command.getTarget().isBlank()
                )

                        &&

                        firstNonBlank(
                                workspace.getActiveFlowName(),
                                session.getActiveFlowName()
                        ) != null

        ) {

            command.setTarget(
                    firstNonBlank(
                            workspace.getActiveFlowName(),
                            session.getActiveFlowName()
                    )
            );
        }

        // =================================================
        // URL
        // =================================================

        if (

                (
                        command.getUrl() == null
                                ||
                                command.getUrl().isBlank()
                )

                        &&

                        firstNonBlank(
                                workspace.getWebsiteUrl(),
                                session.getActiveUrl()
                        ) != null

        ) {

            command.setUrl(
                    firstNonBlank(
                            workspace.getWebsiteUrl(),
                            session.getActiveUrl()
                    )
            );
        }

        // =================================================
        // FEATURE
        // =================================================

        if (

                (
                        command.getFeatureName() == null
                                ||
                                command.getFeatureName().isBlank()
                )

                        &&

                        workspace.getActiveFeature()
                                != null

        ) {

            command.setFeatureName(
                    workspace.getActiveFeature()
            );
        }
    }

    private boolean isGenerationIntent(
            AICommand command
    ) {

        return "GENERATE_FRAMEWORK"
                .equalsIgnoreCase(
                        command.getIntent()
                )
                ||
                "GENERATE_FEATURE"
                        .equalsIgnoreCase(
                                command.getIntent()
                        );
    }

    private String firstNonBlank(

            String first,

            String second

    ) {

        if (
                first != null
                        &&
                        !first.isBlank()
        ) {

            return first;
        }

        return second;
    }

    private String extractUrl(
            String message
    ) {

        String[] tokens =
                message.split("\\s+");

        for (String token : tokens) {

            String cleaned =
                    token.replaceAll("[\\)\\].,;]+$", "");

            if (
                    cleaned.startsWith("http://")
                            ||
                            cleaned.startsWith("https://")
            ) {

                return cleaned;
            }

            if (
                    cleaned.matches("(?:www\\.)?[A-Za-z0-9-]+\\.[A-Za-z]{2,}(?:/.*)?")
            ) {

                return "https://"
                        + cleaned;
            }
        }

        return null;
    }

    private String extractDomain(
            String url
    ) {

        if (
                url == null
        ) {

            return null;
        }

        return url.replace("https://", "")
                .replace("http://", "")
                .replace("www.", "")
                .split("/")[0];
    }

    private String extractFeatureName(
            String message
    ) {

        String lower =
                message.toLowerCase();

        if (lower.contains("login")) {
            return "login";
        }

        if (lower.contains("search")) {
            return "search";
        }

        if (
                lower.contains("register")
                        ||
                        lower.contains("signup")
                        ||
                        lower.contains("sign up")
        ) {
            return "registration";
        }

        if (lower.contains("checkout")) {
            return "checkout";
        }

        if (lower.contains("form")) {
            return "form";
        }

        return null;
    }
}
