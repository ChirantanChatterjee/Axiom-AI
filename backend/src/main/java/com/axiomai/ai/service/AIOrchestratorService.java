package com.axiomai.ai.service;

import com.axiomai.audit.AuditLogService;
import com.axiomai.ai.dto.AICommand;
import com.axiomai.ai.dto.AIResponse;
import com.axiomai.ai.intent.IntentParser;
import com.axiomai.ai.orchestrator.AICommandOrchestrator;
import com.axiomai.core.memory.ExecutionMemoryService;
import com.axiomai.core.session.ExecutionSession;
import com.axiomai.security.SensitiveLogSanitizer;
import com.axiomai.workspace.AutomationSession;
import com.axiomai.workspace.AutomationWorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

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

    private AuditLogService
            auditLogService;

    @Autowired(required = false)
    public void setAuditLogService(
            AuditLogService auditLogService
    ) {

        this.auditLogService =
                auditLogService;
    }

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

        return processMessage(
                message,
                sessionId,
                websiteUrl,
                domainName,
                frameworkLocked,
                null,
                null
        );
    }

    public AIResponse processMessage(

            String message,

            String sessionId,

            String websiteUrl,

            String domainName,

            Boolean frameworkLocked,

            String explicitIntent,

            Map<String, String> explicitVariables

    ) {

        String safeMessage =
                message == null
                        ? ""
                        : message;

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
                intentParser.parse(safeMessage);

        command.setUserId(userId);

        applyStructuredRequestFields(
                command,
                explicitIntent,
                explicitVariables
        );

        // =================================================
        // NORMALIZATION
        // =================================================

        normalizeCommand(
                command,
                safeMessage
        );

        // =================================================
        // CONVERSATIONAL RECOVERY
        // =================================================

        recoverConversationalIntent(
                command,
                safeMessage,
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

        String auditAction =
                auditAction(command);

        auditAiRequested(
                userId,
                command,
                auditAction
        );

        try {

            AIResponse response =
                    orchestrator.execute(
                            command
                    );

            auditAiCompleted(
                    userId,
                    command,
                    response,
                    auditAction
            );

            return response;

        } catch (RuntimeException e) {

            log.warn(
                    "AIF command failed: {} ({})",
                    SensitiveLogSanitizer.redact(
                            e.getMessage()
                    ),
                    e.getClass()
                            .getSimpleName()
            );

            auditAiFailed(
                    userId,
                    command,
                    e,
                    auditAction
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

    private String auditAction(
            AICommand command
    ) {

        if (
                command == null
                        ||
                        command.getIntent() == null
        ) {

            return null;
        }

        return switch (
                command.getIntent()
                        .trim()
                        .toUpperCase()
        ) {
            case "GENERATE_FRAMEWORK" -> "ai.framework_generation";
            case "GENERATE_FEATURE" -> "ai.feature_generation";
            case "UPDATE_TEST_DATA" -> "ai.test_data_update";
            case "EXECUTE_GENERATED_TESTS" -> "ai.generated_test_execution";
            default -> null;
        };
    }

    private void auditAiRequested(
            String userId,
            AICommand command,
            String action
    ) {

        if (
                auditLogService == null
                        ||
                        action == null
        ) {

            return;
        }

        auditLogService.recordSuccess(
                userId,
                userId,
                action + ".requested",
                "AI_COMMAND",
                userId,
                auditDetails(
                        command,
                        null,
                        null
                )
        );
    }

    private void auditAiCompleted(
            String userId,
            AICommand command,
            AIResponse response,
            String action
    ) {

        if (
                auditLogService == null
                        ||
                        action == null
        ) {

            return;
        }

        Map<String, ?> details =
                auditDetails(
                        command,
                        response,
                        null
                );

        if (
                response != null
                        &&
                        response.isSuccess()
        ) {

            auditLogService.recordSuccess(
                    userId,
                    userId,
                    action + ".completed",
                    "AI_COMMAND",
                    userId,
                    details
            );

            return;
        }

        auditLogService.recordFailure(
                userId,
                userId,
                action + ".failed",
                "AI_COMMAND",
                userId,
                details
        );
    }

    private void auditAiFailed(
            String userId,
            AICommand command,
            RuntimeException exception,
            String action
    ) {

        if (
                auditLogService == null
                        ||
                        action == null
        ) {

            return;
        }

        auditLogService.recordFailure(
                userId,
                userId,
                action + ".failed",
                "AI_COMMAND",
                userId,
                auditDetails(
                        command,
                        null,
                        exception
                )
        );
    }

    private Map<String, Object> auditDetails(
            AICommand command,
            AIResponse response,
            RuntimeException exception
    ) {

        Map<String, Object> details =
                new LinkedHashMap<>();

        if (
                command != null
        ) {

            putIfPresent(
                    details,
                    "intent",
                    command.getIntent()
            );
            putIfPresent(
                    details,
                    "target",
                    command.getTarget()
            );
            putIfPresent(
                    details,
                    "featureName",
                    command.getFeatureName()
            );
            putIfPresent(
                    details,
                    "artifactName",
                    command.getArtifactName()
            );
            putIfPresent(
                    details,
                    "domain",
                    extractDomain(command.getUrl())
            );

            details.put(
                    "hasVariables",
                    command.getVariables() != null
                            &&
                            !command.getVariables()
                                    .isEmpty()
            );
        }

        if (
                response != null
        ) {

            putIfPresent(
                    details,
                    "responseType",
                    response.getType()
            );

            details.put(
                    "success",
                    response.isSuccess()
            );
        }

        if (
                exception != null
        ) {

            putIfPresent(
                    details,
                    "errorType",
                    exception.getClass()
                            .getSimpleName()
            );
        }

        return details;
    }

    private void putIfPresent(
            Map<String, Object> details,
            String key,
            String value
    ) {

        if (
                value != null
                        &&
                        !value.isBlank()
        ) {

            details.put(
                    key,
                    value
            );
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

    private void applyStructuredRequestFields(

            AICommand command,

            String explicitIntent,

            Map<String, String> explicitVariables

    ) {

        if (
                explicitIntent != null
                        &&
                        !explicitIntent.isBlank()
        ) {

            command.setIntent(
                    explicitIntent.trim()
            );
        }

        Map<String, String> structuredVariables =
                cleanStructuredVariables(
                        explicitVariables
                );

        if (
                !structuredVariables.isEmpty()
        ) {

            command.setVariables(
                    structuredVariables
            );
        }
    }

    private Map<String, String> cleanStructuredVariables(
            Map<String, String> variables
    ) {

        Map<String, String> cleanVariables =
                new LinkedHashMap<>();

        if (
                variables == null
                        ||
                        variables.isEmpty()
        ) {

            return cleanVariables;
        }

        variables.forEach((name, value) -> {
            if (
                    name == null
                            ||
                            name.isBlank()
                            ||
                            value == null
            ) {

                return;
            }

            cleanVariables.put(
                    name.trim(),
                    value
            );
        });

        return cleanVariables;
    }

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

                "EXECUTE_GENERATED_TESTS".equalsIgnoreCase(
                        command.getIntent()
                )

                        &&

                        shouldUseRememberedGeneratedTestTarget(
                                command,
                                message,
                                lower,
                                workspace
                        )

        ) {

            command.setTarget(
                    generatedTestRerunTarget(
                            workspace
                    )
            );

            return;
        }

        if (

                "EXECUTE_GENERATED_TESTS".equalsIgnoreCase(
                        command.getIntent()
                )

                        &&

                        shouldUseDefaultGeneratedTestTarget(
                                command,
                                lower,
                                workspace
                        )

        ) {

            command.setTarget(
                    generatedTestDefaultTarget(
                            lower,
                            workspace
                    )
            );

            return;
        }

        if (

                "EXECUTE_GENERATED_TESTS".equalsIgnoreCase(
                        command.getIntent()
                )

                        &&

                        (
                                command.getTarget() == null
                                        ||
                                        command.getTarget()
                                                .isBlank()
                        )

        ) {

            command.setTarget(
                    generatedTestDefaultTarget(
                            lower,
                            workspace
                    )
            );

            return;
        }

        if (

                isUnknownOrFlowExecution(
                        command.getIntent()
                )

                        &&

                        shouldRepairGeneratedTests(
                                lower,
                                workspace
                        )

        ) {

            command.setIntent(
                    "REPAIR_GENERATED_TESTS"
            );

            return;
        }

        if (

                isUnknownOrFlowExecution(
                        command.getIntent()
                )

                        &&

                        isRunRequest(lower)

                        &&

                        shouldRerunGeneratedTests(
                                lower,
                                workspace
                        )

        ) {

            command.setIntent(
                    "EXECUTE_GENERATED_TESTS"
            );

            command.setTarget(
                    generatedTestRerunTarget(
                            workspace
                    )
            );

            return;
        }

        if (

                "UNKNOWN".equalsIgnoreCase(
                        command.getIntent()
                )

                        &&

                        isRunRequest(lower)

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

    private boolean isRunRequest(
            String lower
    ) {

        return lower.contains("execute")
                ||
                lower.contains("run")
                ||
                lower.contains("start")
                ||
                lower.contains("rerun")
                ||
                lower.contains("re-run");
    }

    private boolean isUnknownOrFlowExecution(
            String intent
    ) {

        return "UNKNOWN".equalsIgnoreCase(intent)
                ||
                "EXECUTE_FLOW".equalsIgnoreCase(intent);
    }

    private boolean shouldRerunGeneratedTests(

            String lower,
            AutomationSession workspace

    ) {

        if (
                lower.contains("flow")
                        ||
                        lower.contains("feature")
        ) {

            return false;
        }

        boolean referencesTests =
                lower.contains("test")
                        ||
                        lower.contains("cucumber")
                        ||
                        lower.contains("scenario")
                        ||
                        lower.contains("tag")
                        ||
                        lower.contains("same")
                        ||
                        lower.contains("again")
                        ||
                        lower.contains("rerun")
                        ||
                        lower.contains("re-run");

        if (
                !referencesTests
        ) {

            return false;
        }

        return firstNonBlank(
                workspace.getPendingGeneratedTestTagExpression(),
                workspace.getLastGeneratedTestTagExpression()
        ) != null
                ||
                hasFrameworkArtifact(workspace);
    }

    private String generatedTestRerunTarget(
            AutomationSession workspace
    ) {

        String rememberedTarget =
                firstNonBlank(
                        workspace.getPendingGeneratedTestTagExpression(),
                        workspace.getLastGeneratedTestTagExpression()
                );

        return rememberedTarget == null
                ? "ALL"
                : rememberedTarget;
    }

    private String generatedTestDefaultTarget(
            String lower,
            AutomationSession workspace
    ) {

        if (
                shouldRerunGeneratedTests(
                        lower,
                        workspace
                )
        ) {

            return generatedTestRerunTarget(workspace);
        }

        return "ALL";
    }

    private boolean shouldRepairGeneratedTests(

            String lower,

            AutomationSession workspace

    ) {

        if (
                lower == null
                        ||
                        lower.isBlank()
        ) {

            return false;
        }

        if (
                lower.contains("test data")
                        ||
                        lower.contains("runtime data")
        ) {

            return false;
        }

        boolean repairVerb =
                java.util.regex.Pattern.compile(
                                "\\b(?:fix|repair|resolve|heal|rectify|correct|stabilize|stabilise|analy[sz]e|diagnose|inspect|investigate)\\b"
                        )
                        .matcher(lower)
                        .find()
                        ||
                        lower.contains("make it pass")
                        ||
                        lower.contains("make the test pass")
                        ||
                        lower.contains("make tests pass")
                        ||
                        lower.contains("use what you learned")
                        ||
                        lower.contains("apply learned");

        if (
                !repairVerb
        ) {

            return false;
        }

        boolean generatedTestContext =
                lower.contains("generated")
                        ||
                        lower.contains("cucumber")
                        ||
                        lower.contains("gherkin")
                        ||
                        lower.contains("test")
                        ||
                        lower.contains("scenario")
                        ||
                        lower.contains("last")
                        ||
                        lower.contains("failure")
                        ||
                        lower.contains("failed")
                        ||
                        lower.contains("error")
                        ||
                        containsWholeWord(lower, "it")
                        ||
                        containsWholeWord(lower, "this")
                        ||
                        containsWholeWord(lower, "that");

        if (
                !generatedTestContext
        ) {

            return false;
        }

        if (
                lower.contains("flow")
                        &&
                        !lower.contains("test")
                        &&
                        !lower.contains("generated")
                        &&
                        !lower.contains("cucumber")
        ) {

            return false;
        }

        return firstNonBlank(
                workspace.getPendingGeneratedTestTagExpression(),
                workspace.getLastGeneratedTestTagExpression()
        ) != null
                ||
                hasFrameworkArtifact(workspace);
    }

    private boolean shouldUseRememberedGeneratedTestTarget(

            AICommand command,

            String message,

            String lower,

            AutomationSession workspace

    ) {

        if (
                lower.contains("flow")
                        ||
                        lower.contains("feature")
                        ||
                        hasExplicitGeneratedTestTarget(lower)
        ) {

            return false;
        }

        if (
                !isConversationalGeneratedTestRerun(lower)
                        ||
                        !shouldRerunGeneratedTests(
                                lower,
                                workspace
                        )
        ) {

            return false;
        }

        String target =
                command.getTarget();

        if (
                target == null
                        ||
                        target.isBlank()
        ) {

            return true;
        }

        String normalizedTarget =
                target.trim();

        String normalizedMessage =
                message == null
                        ? ""
                        : message.trim();

        if (
                normalizedTarget.equalsIgnoreCase("ALL")
                        &&
                        !lower.contains("all")
        ) {

            return true;
        }

        return normalizedTarget.equalsIgnoreCase(normalizedMessage)
                ||
                normalizedTarget.equalsIgnoreCase(lower)
                ||
                looksLikeConversationalRunTarget(normalizedTarget);
    }

    private boolean shouldUseDefaultGeneratedTestTarget(

            AICommand command,

            String lower,

            AutomationSession workspace

    ) {

        if (
                lower.contains("flow")
                        ||
                        lower.contains("feature")
                        ||
                        hasExplicitGeneratedTestTarget(lower)
        ) {

            return false;
        }

        String target =
                command.getTarget();

        if (
                target == null
                        ||
                        target.isBlank()
        ) {

            return false;
        }

        if (
                target.trim()
                        .startsWith("@")
                        ||
                        "ALL".equalsIgnoreCase(
                                target.trim()
                        )
        ) {

            return false;
        }

        return looksLikeConversationalRunTarget(target)
                ||
                target.equalsIgnoreCase(lower);
    }

    private boolean isConversationalGeneratedTestRerun(
            String lower
    ) {

        if (
                !isRunRequest(lower)
        ) {

            return false;
        }

        return lower.contains("rerun")
                ||
                lower.contains("re-run")
                ||
                lower.contains("again")
                ||
                lower.contains("same")
                ||
                lower.contains("last")
                ||
                java.util.regex.Pattern.compile(
                                "\\b(?:run|rerun|re-run)\\s+(?:the\\s+)?(?:generated\\s+)?tests?\\s*\\??\\s*$"
                        )
                        .matcher(lower)
                        .find();
    }

    private boolean hasExplicitGeneratedTestTarget(
            String lower
    ) {

        if (
                lower == null
        ) {

            return false;
        }

        return lower.contains("@")
                ||
                lower.contains(" tag ")
                ||
                lower.contains(" tags ")
                ||
                lower.contains("with tag")
                ||
                lower.contains("matching ")
                ||
                java.util.regex.Pattern.compile(
                                "\\b(?:for|of|on)\\s+(?!me\\b|us\\b)(?:@?[a-z0-9][a-z0-9 _-]{1,80})"
                        )
                        .matcher(lower)
                        .find()
                ||
                lower.contains(" all ");
    }

    private boolean containsWholeWord(
            String lower,
            String word
    ) {

        if (
                lower == null
                        ||
                        word == null
                        ||
                        word.isBlank()
        ) {

            return false;
        }

        return java.util.regex.Pattern.compile(
                        "\\b" + java.util.regex.Pattern.quote(word) + "\\b"
                )
                .matcher(lower)
                .find();
    }

    private boolean looksLikeConversationalRunTarget(
            String target
    ) {

        if (
                target == null
        ) {

            return false;
        }

        String lower =
                target.toLowerCase();

        return !lower.contains("@")
                &&
                (
                        lower.contains("run")
                                ||
                                lower.contains("rerun")
                                ||
                                lower.contains("re-run")
                                ||
                                lower.contains("test")
                                ||
                                lower.contains("again")
                                ||
                                lower.contains("same")
                );
    }

    private boolean hasFrameworkArtifact(
            AutomationSession workspace
    ) {

        return workspace.getArtifacts() != null
                &&
                workspace.getArtifacts()
                        .stream()
                        .anyMatch(
                                artifact ->
                                        artifact != null
                                                &&
                                                "FRAMEWORK".equalsIgnoreCase(
                                                        artifact.getType()
                                                )
                        );
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
                "EXECUTE_GENERATED_TESTS".equalsIgnoreCase(
                        command.getIntent()
                )
        ) {

            if (
                    command.getTarget() == null
                            ||
                            command.getTarget()
                                    .isBlank()
            ) {

                command.setTarget(
                        generatedTestRerunTarget(workspace)
                );
            }

        } else
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
