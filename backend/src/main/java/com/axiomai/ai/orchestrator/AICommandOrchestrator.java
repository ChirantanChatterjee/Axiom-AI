package com.axiomai.ai.orchestrator;

import com.axiomai.ai.dto.AICommand;
import com.axiomai.ai.dto.AIResponse;
import com.axiomai.ai.runtime.AIExecutionRuntimeExecutor;
import com.axiomai.config.PublicBaseUrlResolver;
import com.axiomai.core.adapter.DetectedFlowAdapter;
import com.axiomai.core.adapter.ScenarioPlanAdapter;
import com.axiomai.core.execution.ExecutionResult;
import com.axiomai.core.execution.GraphExecutionBridge;
import com.axiomai.core.graph.ActionNode;
import com.axiomai.core.graph.FlowGraph;
import com.axiomai.core.memory.ExecutionMemoryService;
import com.axiomai.execution.entity.StepExecutionEntity;
import com.axiomai.execution.repository.StepExecutionRepository;
import com.axiomai.flow.entity.FlowEntity;
import com.axiomai.flow.repository.FlowRepository;
import com.axiomai.flowstep.entity.FlowStepEntity;
import com.axiomai.flowstep.repository.FlowStepRepository;
import com.axiomai.qa.flow.DetectedFlow;
import com.axiomai.qa.flow.FlowDetectionEngine;
import com.axiomai.qa.execution.entity.GeneratedTestExecutionJobEntity;
import com.axiomai.qa.execution.service.GeneratedTestExecutionJobDto;
import com.axiomai.qa.execution.service.GeneratedTestExecutionQueueService;
import com.axiomai.qa.models.FlowStep;
import com.axiomai.qa.models.GeneratedFramework;
import com.axiomai.qa.models.PageNode;
import com.axiomai.qa.models.SiteMapResult;
import com.axiomai.qa.service.FlowPersistenceService;
import com.axiomai.qa.service.FrameworkGeneratorService;
import com.axiomai.qa.service.GeneratedFrameworkPersistenceService;
import com.axiomai.qa.service.GeneratedTestExecutionService;
import com.axiomai.qa.service.GeneratedProjectWriterService;
import com.axiomai.qa.service.RequirementTestCaseGeneratorService;
import com.axiomai.qa.service.WebsiteCrawlerService;
import com.axiomai.ml.AIFModelTrainingService;
import com.axiomai.ml.MLModelTrainingResult;
import com.axiomai.security.SensitiveLogSanitizer;
import com.axiomai.workspace.AutomationSession;
import com.axiomai.workspace.AutomationWorkspaceService;
import com.axiomai.workspace.GeneratedArtifact;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AICommandOrchestrator {

    private final FlowRepository
            flowRepository;

    private final FlowStepRepository
            flowStepRepository;

    private final StepExecutionRepository
            stepExecutionRepository;

    private final WebsiteCrawlerService
            websiteCrawlerService;

    private final FrameworkGeneratorService
            frameworkGeneratorService;

    private final GeneratedProjectWriterService
            generatedProjectWriterService;

    private final GeneratedFrameworkPersistenceService
            generatedFrameworkPersistenceService;

    private final GeneratedTestExecutionService
            generatedTestExecutionService;

    private final GeneratedTestExecutionQueueService
            generatedTestExecutionQueueService;

    private final RequirementTestCaseGeneratorService
            requirementTestCaseGeneratorService;

    private final FlowPersistenceService
            flowPersistenceService;

    private final AIExecutionRuntimeExecutor
            aiExecutionRuntimeExecutor;

    private final ExecutionMemoryService
            executionMemoryService;

    private final AutomationWorkspaceService
            automationWorkspaceService;

    private final DetectedFlowAdapter
            detectedFlowAdapter;

    @SuppressWarnings("unused")
    private final ScenarioPlanAdapter
            scenarioPlanAdapter;

    private final GraphExecutionBridge
            graphExecutionBridge;

    private final PublicBaseUrlResolver
            publicBaseUrlResolver;

    private AIFModelTrainingService
            aifModelTrainingService;

    @Autowired(required = false)
    public void setAifModelTrainingService(
            AIFModelTrainingService aifModelTrainingService
    ) {

        this.aifModelTrainingService =
                aifModelTrainingService;
    }

    @Value("${aif.generated-tests.execution-mode:${AIF_GENERATED_TEST_EXECUTION_MODE:worker}}")
    private String generatedTestExecutionMode;

    // =====================================================
    // MAIN EXECUTION
    // =====================================================

    public AIResponse execute(
            AICommand command
    ) {

        try {

            String userId =
                    userId(command);

            if (
                    (
                            command.getIntent() == null
                                    ||
                                    "UNKNOWN".equalsIgnoreCase(
                                            command.getIntent()
                                    )
                    )
                            &&
                            command.getVariables() != null
                            &&
                            !command.getVariables()
                                    .isEmpty()
                            &&
                            !looksLikeGeneratedTestCommand(
                                    command.getMessage()
                            )
            ) {

                command.setIntent("UPDATE_TEST_DATA");
            }

            System.out.println(
                    "ORCHESTRATOR INTENT = "
                            + command.getIntent()
            );

            storeCommandVariables(
                    command,
                    userId
            );

            return switch (command.getIntent()) {

                case "GENERATE_FRAMEWORK" ->
                        generateFramework(
                                command,
                                userId
                        );

                case "COMPOUND_COMMAND" ->
                        executeCompoundCommand(
                                command,
                                userId
                        );

                case "GENERATE_FEATURE" ->
                        generateFeature(
                                command,
                                userId
                        );

                case "UPDATE_TEST_DATA" ->
                        updateTestData(
                                command,
                                userId
                        );

                case "EXECUTE_FEATURE" ->
                        executeFeature(userId);

                case "AI_EXECUTION" ->
                        executeAIPlan(
                                command,
                                userId
                        );

                case "EXECUTE_FLOW" ->
                        executeFlow(
                                command,
                                userId
                        );

                case "DOWNLOAD_FRAMEWORK" ->
                        downloadFramework(userId);

                case "SHOW_GENERATED_TEST_TAGS" ->
                        showGeneratedTestTags(userId);

                case "SHOW_GENERATED_TESTS" ->
                        showGeneratedTests(userId);

                case "EXECUTE_GENERATED_TESTS" ->
                        executeGeneratedTests(
                                command,
                                userId
                        );

                case "REPAIR_GENERATED_TESTS" ->
                        repairGeneratedTests(
                                command,
                                userId
                        );

                case "SHOW_REPORT" ->
                        showReport(userId);

                case "SHOW_DB" ->
                        showDatabase();

                case "RETRAIN_ML_MODELS" ->
                        retrainMlModels(command);

                default -> AIResponse.builder()

                        .success(false)

                        .message(
                                "AIF could not understand the request."
                        )

                        .type("error")

                        .build();
            };

        } catch (Exception e) {

            AIResponse missingRuntimeDataResponse =
                    missingRuntimeDataResponse(e);

            if (
                    missingRuntimeDataResponse != null
            ) {

                return missingRuntimeDataResponse;
            }

            System.out.println(
                    "ORCHESTRATOR FAILED -> "
                            + SensitiveLogSanitizer.redact(
                            e.getMessage()
                    )
            );

            return AIResponse.builder()

                    .success(false)

                    .message(
                            SensitiveLogSanitizer.redact(
                                    e.getMessage()
                            )
                    )

                    .type("error")

                    .build();
        }
    }

    private AIResponse executeCompoundCommand(
            AICommand command,
            String userId
    ) {

        List<AICommand> commands =
                command.getCommands() == null
                        ? List.of()
                        : command.getCommands();

        if (
                commands.isEmpty()
        ) {

            return AIResponse.builder()
                    .success(false)
                    .message("AIF could not find executable steps in that compound request.")
                    .type("error")
                    .build();
        }

        List<Map<String, Object>> stepResults =
                new ArrayList<>();

        AIResponse latestResponse =
                null;

        AutomationSession workspace =
                automationWorkspaceService
                        .getSession(userId);

        String workspaceUrl =
                workspace == null
                        ? null
                        : workspace.getWebsiteUrl();

        for (
                int i = 0;
                i < commands.size();
                i++
        ) {

            AICommand subCommand =
                    commands.get(i);

            subCommand.setUserId(userId);

            if (
                    subCommand.getVariables() == null
                            ||
                            subCommand.getVariables()
                                    .isEmpty()
            ) {

                subCommand.setVariables(
                        command.getVariables()
                );
            }

            if (
                    isBlank(subCommand.getUrl())
                            &&
                            !isBlank(workspaceUrl)
            ) {

                subCommand.setUrl(workspaceUrl);
            }

            latestResponse =
                    execute(subCommand);

            stepResults.add(
                    compoundStepResult(
                            i + 1,
                            subCommand,
                            latestResponse
                    )
            );

            if (
                    latestResponse == null
                            ||
                            !latestResponse.isSuccess()
            ) {

                return compoundResponse(
                        false,
                        "AIF completed "
                                + i
                                + " of "
                                + commands.size()
                                + " requested actions. The next action failed: "
                                + (
                                latestResponse == null
                                        ? "No response was returned."
                                        : latestResponse.getMessage()
                        ),
                        stepResults,
                        latestResponse
                );
            }
        }

        return compoundResponse(
                true,
                "AIF completed "
                        + commands.size()
                        + " requested actions in order.",
                stepResults,
                latestResponse
        );
    }

    private AIResponse retrainMlModels(
            AICommand command
    ) {

        if (
                aifModelTrainingService == null
        ) {

            return AIResponse.builder()
                    .success(false)
                    .message("AIF custom ML training is not available in this runtime.")
                    .type("error")
                    .build();
        }

        String target =
                command == null
                        ? null
                        : command.getTarget();

        if (
                target != null
                        &&
                        !target.isBlank()
        ) {

            MLModelTrainingResult result =
                    aifModelTrainingService.retrainModel(
                            target
                    );

            return AIResponse.builder()
                    .success(
                            result.isTrained()
                    )
                    .message(
                            trainingResultMessage(result)
                    )
                    .type("ml_training")
                    .data(result)
                    .build();
        }

        Map<String, MLModelTrainingResult> results =
                aifModelTrainingService.retrainAllModels();

        boolean trainedAny =
                results.values()
                        .stream()
                        .anyMatch(
                                MLModelTrainingResult::isTrained
                        );

        StringBuilder message =
                new StringBuilder(
                        "AIF custom ML retraining completed."
                );

        for (
                MLModelTrainingResult result
                : results.values()
        ) {

            message.append("\n- ")
                    .append(
                            trainingResultMessage(result)
                    );
        }

        return AIResponse.builder()
                .success(trainedAny)
                .message(
                        message.toString()
                )
                .type("ml_training")
                .data(results)
                .build();
    }

    private String trainingResultMessage(
            MLModelTrainingResult result
    ) {

        if (
                result == null
        ) {

            return "AIF model training did not return a result.";
        }

        if (
                result.isTrained()
        ) {

            return "AIF model retrained: "
                    + result.getModelName()
                    + " "
                    + result.getVersion()
                    + " trained on "
                    + result.getTrainingExampleCount()
                    + " examples.";
        }

        return "AIF model training skipped: "
                + result.getModelName()
                + " - "
                + result.getMessage();
    }

    private Map<String, Object> compoundStepResult(
            int index,
            AICommand command,
            AIResponse response
    ) {

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put("index", index);
        result.put("intent", command.getIntent());
        result.put("featureName", command.getFeatureName());
        result.put("target", command.getTarget());
        result.put("success", response != null && response.isSuccess());
        result.put(
                "message",
                response == null
                        ? null
                        : response.getMessage()
        );
        result.put(
                "type",
                response == null
                        ? null
                        : response.getType()
        );

        return result;
    }

    private AIResponse compoundResponse(
            boolean success,
            String message,
            List<Map<String, Object>> stepResults,
            AIResponse latestResponse
    ) {

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put("steps", stepResults);

        return AIResponse.builder()
                .success(success)
                .message(message)
                .type("compound")
                .data(data)
                .downloadUrl(
                        latestResponse == null
                                ? null
                                : latestResponse.getDownloadUrl()
                )
                .reportUrl(
                        latestResponse == null
                                ? null
                                : latestResponse.getReportUrl()
                )
                .build();
    }

    private AIResponse missingRuntimeDataResponse(
            Exception exception
    ) {

        String message =
                exception.getMessage();

        if (
                message == null
                        ||
                        !message.startsWith(
                                "Missing runtime data for generated tests:"
                        )
        ) {

            return null;
        }

        String rawVariables =
                message.substring(
                        "Missing runtime data for generated tests:"
                                .length()
                );

        int guidanceStart =
                rawVariables.indexOf(".");

        if (
                guidanceStart >= 0
        ) {

            rawVariables =
                    rawVariables.substring(
                            0,
                            guidanceStart
                    );
        }

        List<String> missingVariables =
                new ArrayList<>();

        for (
                String variable
                : rawVariables.split(",")
        ) {

            String cleaned =
                    variable.trim();

            if (
                    !cleaned.isBlank()
            ) {

                missingVariables.add(cleaned);
            }
        }

        return missingRuntimeDataResponse(missingVariables);
    }

    private AIResponse missingRuntimeDataResponse(
            List<String> missingVariables
    ) {

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "missingVariables",
                missingVariables
        );

        data.put(
                "example",
                exampleVariableReply(missingVariables)
        );

        return AIResponse.builder()
                .success(false)
                .type("missing-variables")
                .message(
                        "I need a few runtime values before I can execute those generated tests. "
                                + "Please provide: "
                                + String.join(
                                ", ",
                                missingVariables
                        )
                                + "."
                )
                .data(data)
                .build();
    }

    private AIResponse missingRuntimeDataResponseWithContexts(
            List<GeneratedTestExecutionService.RuntimeVariableContext> contexts
    ) {

        List<String> missingVariables =
                new ArrayList<>();

        List<Map<String, Object>> variableDetails =
                new ArrayList<>();

        if (
                contexts != null
        ) {

            for (
                    GeneratedTestExecutionService.RuntimeVariableContext context
                    : contexts
            ) {

                if (
                        context == null
                                ||
                                context.getVariable() == null
                                ||
                                context.getVariable().isBlank()
                ) {

                    continue;
                }

                String variable =
                        context.getVariable();

                if (
                        missingVariables.stream()
                                .noneMatch(existing ->
                                        existing.equalsIgnoreCase(variable)
                                )
                ) {

                    missingVariables.add(variable);
                }

                Map<String, Object> detail =
                        new LinkedHashMap<>();

                detail.put(
                        "variable",
                        variable
                );

                detail.put(
                        "feature",
                        context.getFeature()
                );

                detail.put(
                        "scenario",
                        context.getScenario()
                );

                detail.put(
                        "step",
                        context.getStep()
                );

                detail.put(
                        "hint",
                        context.getHint()
                );

                variableDetails.add(detail);
            }
        }

        if (
                missingVariables.isEmpty()
        ) {

            return missingRuntimeDataResponse(
                    missingVariables
            );
        }

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "missingVariables",
                missingVariables
        );

        data.put(
                "variableDetails",
                variableDetails
        );

        data.put(
                "example",
                exampleVariableReply(missingVariables)
        );

        String contextSummary =
                runtimeVariableContextSummary(variableDetails);

        return AIResponse.builder()
                .success(false)
                .type("missing-variables")
                .message(
                        "I need a few runtime values before I can execute those generated tests. "
                                + "Please provide: "
                                + String.join(
                                ", ",
                                missingVariables
                        )
                                + "."
                                + (
                                contextSummary.isBlank()
                                        ? ""
                                        : " " + contextSummary
                        )
                )
                .data(data)
                .build();
    }

    private String runtimeVariableContextSummary(
            List<Map<String, Object>> variableDetails
    ) {

        if (
                variableDetails == null
                        ||
                        variableDetails.isEmpty()
        ) {

            return "";
        }

        List<String> summaries =
                new ArrayList<>();

        for (
                Map<String, Object> detail
                : variableDetails
        ) {

            if (
                    summaries.size() >= 3
            ) {

                break;
            }

            Object variable =
                    detail.get("variable");

            Object scenario =
                    detail.get("scenario");

            Object step =
                    detail.get("step");

            if (
                    variable == null
                            ||
                            step == null
            ) {

                continue;
            }

            summaries.add(
                    variable
                            + " is used"
                            + (
                            scenario == null
                                    ? ""
                                    : " in scenario \""
                                    + scenario
                                    + "\""
                    )
                            + " at step \""
                            + step
                            + "\""
            );
        }

        return summaries.isEmpty()
                ? ""
                : "Context: "
                + String.join(
                "; ",
                summaries
        )
                + ".";
    }

    private String exampleVariableReply(
            List<String> missingVariables
    ) {

        if (
                missingVariables == null
                        ||
                        missingVariables.isEmpty()
        ) {

            return "";
        }

        StringBuilder example =
                new StringBuilder();

        for (
                String variable
                : missingVariables
        ) {

            if (
                    !example.isEmpty()
            ) {

                example.append(", ");
            }

            example.append(variable)
                    .append(" is ")
                    .append(exampleValueFor(variable));
        }

        return example.toString();
    }

    private String exampleValueFor(
            String variable
    ) {

        String lower =
                variable == null
                        ? ""
                        : variable.toLowerCase();

        if (
                lower.contains("first")
        ) {

            return "John";
        }

        if (
                lower.contains("last")
        ) {

            return "Smith";
        }

        if (
                lower.contains("postal")
                        ||
                        lower.contains("zip")
        ) {

            return "12345";
        }

        return "value";
    }

    private String userId(
            AICommand command
    ) {

        if (
                command == null
                        ||
                        command.getUserId() == null
                        ||
                        command.getUserId().isBlank()
        ) {

            return "default-user";
        }

        return command.getUserId();
    }

    // =====================================================
    // GENERATE FRAMEWORK
    // =====================================================

    private AIResponse generateFramework(
            AICommand command,
            String userId
    ) {

        if (
                command.getUrl() == null
                        ||
                        command.getUrl().isBlank()
        ) {

            return AIResponse.builder()

                    .success(false)

                    .message(
                            "No website URL provided."
                    )

                    .type("error")

                            .build();
        }

        AIResponse sessionGuard =
                guardFrameworkSession(
                        command,
                        userId
                );

        if (
                sessionGuard != null
                        &&
                        !isPendingFrameworkResume(
                                userId,
                                command.getUrl()
                        )
        ) {

            return sessionGuard;
        }

        List<DetectedFlow> allFlows =
                crawlAndStoreFlows(
                        command.getUrl(),
                        userId
                );

        allFlows =
                prioritizeFlowsForCommand(
                        command,
                        allFlows,
                        userId
                );

        if (
                requiresCrawlerCredentials(allFlows)
                        &&
                        missingCrawlerCredentials(userId)
        ) {

            automationWorkspaceService
                    .setPendingFrameworkGeneration(
                            userId,
                            command.getUrl()
                    );

            return missingCrawlerDataResponse(
                    command.getUrl()
            );
        }

        if (
                allFlows.isEmpty()
        ) {

            return AIResponse.builder()

                    .success(false)

                    .message(
                            "No executable flows detected."
                    )

                    .type("error")

                    .build();
        }

        GeneratedFramework framework =
                frameworkGeneratorService
                        .generate(allFlows);

        AutomationSession workspace =
                automationWorkspaceService
                        .getSession(userId);

        String frameworkPath =
                generatedProjectWriterService
                        .writeFramework(
                                framework,
                                workspace.getSessionId(),
                                "generated"
                        );

        GeneratedArtifact artifact =
                zipAndStoreFrameworkArtifact(
                        workspace.getSessionId(),
                        userId
                );

        automationWorkspaceService
                .clearPendingFrameworkGeneration(userId);

        DetectedFlow primaryFlow =
                allFlows.get(0);

        FlowGraph graph =
                detectedFlowAdapter
                        .convert(primaryFlow);

        applyWorkspaceVariables(
                graph,
                userId
        );

        executionMemoryService
                .storeFlowGraph(
                        userId,
                        graph
                );

        automationWorkspaceService
                .setActiveFlow(
                        userId,
                        graph.getName()
                );

        executionMemoryService
                .getSession(userId)
                .setActiveUrl(
                        command.getUrl()
                );

        Map<String, Object> data =
                new HashMap<>();

        data.put("framework", framework);
        data.put("frameworkPath", frameworkPath);
        data.put("sessionId", workspace.getSessionId());
        data.put("artifact", artifact);
        data.put("downloadUrl", artifact.getDownloadUrl());
        data.put("flowsDetected", allFlows.size());
        data.put("flows", flowSummary(allFlows));
        data.put("testCases", framework.getTestCases());
        data.put(
                "testCaseCount",
                framework.getTestCases() == null
                        ? 0
                        : framework.getTestCases()
                        .size()
        );
        data.put("websiteUrl", command.getUrl());
        data.put("domainName", extractDomain(command.getUrl()));

        return AIResponse.builder()

                .success(true)

                .message(
                        "Framework generated and stored in the workspace."
                )

                .type("framework")

                .downloadUrl(
                        artifact.getDownloadUrl()
                )

                .data(data)

                .build();
    }

    private AIResponse guardFrameworkSession(

            AICommand command,

            String userId

    ) {

        AutomationSession workspace =
                automationWorkspaceService
                        .getSession(userId);

        if (
                !hasFrameworkContext(workspace)
        ) {

            return null;
        }

        String currentWebsite =
                workspace.getWebsiteUrl();

        String requestedWebsite =
                command.getUrl();

        if (
                isBlank(requestedWebsite)
        ) {

            return null;
        }

        if (
                isBlank(currentWebsite)
        ) {

            Map<String, Object> data =
                    new LinkedHashMap<>();

            data.put(
                    "requestedWebsite",
                    requestedWebsite
            );

            data.put(
                    "requestedDomain",
                    extractDomain(requestedWebsite)
            );

            return AIResponse.builder()
                    .success(false)
                    .message(
                            "This chat already has an active framework context. Create a new chat from the left sidebar when you need another framework."
                    )
                    .type("session_guard")
                    .data(data)
                    .build();
        }

        String currentDomain =
                extractDomain(currentWebsite);

        String requestedDomain =
                extractDomain(requestedWebsite);

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "currentWebsite",
                currentWebsite
        );

        data.put(
                "requestedWebsite",
                requestedWebsite
        );

        data.put(
                "currentDomain",
                currentDomain
        );

        data.put(
                "requestedDomain",
                requestedDomain
        );

        String message;

        if (
                sameText(
                        currentDomain,
                        requestedDomain
                )
        ) {

            message =
                    "This chat already has an active framework for "
                            + currentDomain
                            + ". Continue here to add tests, list tags, run tests, or download that framework. Create a new chat from the left sidebar when you need another framework.";

        } else {

            message =
                    "This chat is already attached to "
                            + currentDomain
                            + ". To generate a framework for "
                            + requestedDomain
                            + ", create a new chat from the left sidebar so each chat keeps one website and one framework context.";
        }

        return AIResponse.builder()

                .success(false)

                .message(message)

                .type("session_guard")

                .data(data)

                .build();
    }

    private boolean hasFrameworkContext(
            AutomationSession workspace
    ) {

        if (
                workspace == null
        ) {

            return false;
        }

        return !isBlank(
                workspace.getWebsiteUrl()
        )
                ||
                hasFrameworkArtifact(workspace)
                ||
                generatedFrameworkExists(workspace)
                ||
                (
                        workspace.getDetectedFlows() != null
                                &&
                                !workspace.getDetectedFlows()
                                        .isEmpty()
                )
                ||
                (
                        workspace.getGeneratedFeatures() != null
                                &&
                                !workspace.getGeneratedFeatures()
                                        .isEmpty()
                );
    }

    private boolean isPendingFrameworkResume(
            String userId,
            String requestedUrl
    ) {

        String pendingUrl =
                automationWorkspaceService
                        .getPendingFrameworkGeneration(userId);

        return !isBlank(pendingUrl)
                &&
                sameText(
                        extractDomain(pendingUrl),
                        extractDomain(requestedUrl)
                );
    }

    private boolean requiresCrawlerCredentials(
            List<DetectedFlow> flows
    ) {

        if (
                flows == null
        ) {

            return false;
        }

        return flows.stream()
                .anyMatch(flow ->
                        flow != null
                                &&
                                "LOGIN".equalsIgnoreCase(
                                        flow.getFlowType()
                                )
                );
    }

    private boolean missingCrawlerCredentials(
            String userId
    ) {

        Map<String, String> variables =
                automationWorkspaceService
                        .getVariableValues(userId);

        return isBlank(variables.get("username"))
                ||
                isBlank(variables.get("password"));
    }

    private AIResponse missingCrawlerDataResponse(
            String url
    ) {

        List<GeneratedTestExecutionService.RuntimeVariableContext> contexts =
                List.of(
                        GeneratedTestExecutionService.RuntimeVariableContext
                                .builder()
                                .variable("username")
                                .feature("Crawler")
                                .scenario("Continue crawling authenticated pages")
                                .step("Enter username into the login form for " + url)
                                .hint("Username required to move past this login gate.")
                                .build(),
                        GeneratedTestExecutionService.RuntimeVariableContext
                                .builder()
                                .variable("password")
                                .feature("Crawler")
                                .scenario("Continue crawling authenticated pages")
                                .step("Enter password into the login form for " + url)
                                .hint("Password required to move past this login gate.")
                                .build()
                );

        AIResponse response =
                missingRuntimeDataResponseWithContexts(contexts);

        response.setMessage(
                "I found a login gate while crawling this website. "
                        + "Please provide username and password so I can continue crawling authenticated pages before generating the framework."
        );

        return response;
    }

    private boolean generatedFrameworkExists(
            AutomationSession workspace
    ) {

        if (
                generatedProjectWriterService == null
                        ||
                        workspace.getSessionId() == null
                        ||
                        workspace.getSessionId()
                                .isBlank()
        ) {

            return false;
        }

        return Files.exists(
                generatedProjectWriterService
                        .getFrameworkRoot(
                                workspace.getSessionId()
                        )
                        .resolve("pom.xml")
        );
    }

    private boolean hasFrameworkArtifact(
            AutomationSession workspace
    ) {

        if (
                workspace.getArtifacts() == null
        ) {

            return false;
        }

        return workspace.getArtifacts()
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
    // GENERATE FEATURE
    // =====================================================

    private AIResponse generateFeature(
            AICommand command,
            String userId
    ) {

        AIResponse sessionGuard =
                guardFeatureSession(
                        command,
                        userId
                );

        if (
                sessionGuard != null
        ) {

            return sessionGuard;
        }

        List<DetectedFlow> flows =
                ensureWorkspaceFlows(
                        command,
                        userId
                );

        if (
                flows.isEmpty()
        ) {

            return AIResponse.builder()

                    .success(false)

                    .message(
                            "No workspace flows are available. Generate a framework first or include a website URL."
                    )

                    .type("error")

                    .build();
        }

        String featureName =
                firstNonBlank(
                        command.getFeatureName(),
                        command.getFlowName(),
                        "generated"
                );

        DetectedFlow flow =
                automationWorkspaceService
                        .findFlow(
                                userId,
                                featureName
                        );

        if (
                flow == null
        ) {

            flow =
                    flows.get(0);
        }

        GeneratedFramework featureFramework =
                requirementTestCaseGeneratorService
                        .generate(
                                command.getMessage(),
                                featureName,
                                firstNonBlank(
                                        command.getUrl(),
                                        flow.getPageUrl(),
                                        ""
                                ),
                                flows,
                                userId
                        );

        AutomationSession workspace =
                automationWorkspaceService
                        .getSession(userId);

        String frameworkPath =
                generatedProjectWriterService
                        .writeFramework(
                                featureFramework,
                                workspace.getSessionId(),
                                featureName
                        );

        automationWorkspaceService
                .storeFeature(
                        userId,
                        featureName,
                        featureFramework.getFeatureFile()
                );

        FlowGraph graph =
                detectedFlowAdapter
                        .convert(flow);

        applyWorkspaceVariables(
                graph,
                userId
        );

        executionMemoryService
                .storeFlowGraph(
                        userId,
                        graph
                );

        automationWorkspaceService
                .setActiveFlow(
                        userId,
                        graph.getName()
                );

        GeneratedArtifact artifact =
                zipAndStoreFrameworkArtifact(
                        workspace.getSessionId(),
                        userId
                );

        Map<String, Object> data =
                new HashMap<>();

        data.put("featureName", featureName);
        data.put("featureFile", featureFramework.getFeatureFile());
        data.put("frameworkPath", frameworkPath);
        data.put("artifact", artifact);
        data.put("downloadUrl", artifact.getDownloadUrl());
        data.put("variables", automationWorkspaceService.getVariableValues(userId));
        data.put("testCases", featureFramework.getTestCases());
        data.put(
                "testCaseCount",
                featureFramework.getTestCases() == null
                        ? 0
                        : featureFramework.getTestCases()
                        .size()
        );

        return AIResponse.builder()

                .success(true)

                .message(
                        featureFramework.getTestCases() != null
                                &&
                                !featureFramework.getTestCases()
                                        .isEmpty()
                                ? "Requirement tests generated as a test-case matrix and linked to the current workspace."
                                : "Feature generated and linked to the current workspace."
                )

                .type("feature")

                .downloadUrl(
                        artifact.getDownloadUrl()
                )

                .data(data)

                .build();
    }

    private AIResponse guardFeatureSession(

            AICommand command,

            String userId

    ) {

        AutomationSession workspace =
                automationWorkspaceService
                        .getSession(userId);

        if (
                !hasFrameworkContext(workspace)
                        ||
                        isBlank(command.getUrl())
        ) {

            return null;
        }

        String currentWebsite =
                workspace.getWebsiteUrl();

        if (
                isBlank(currentWebsite)
        ) {

            Map<String, Object> data =
                    new LinkedHashMap<>();

            data.put(
                    "requestedWebsite",
                    command.getUrl()
            );

            data.put(
                    "requestedDomain",
                    extractDomain(command.getUrl())
            );

            return AIResponse.builder()
                    .success(false)
                    .message(
                            "This chat already has an active framework context. Create a new chat from the left sidebar when you need to work with another website."
                    )
                    .type("session_guard")
                    .data(data)
                    .build();
        }

        String currentDomain =
                extractDomain(currentWebsite);

        String requestedDomain =
                extractDomain(command.getUrl());

        if (
                sameText(
                        currentDomain,
                        requestedDomain
                )
        ) {

            return null;
        }

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "currentWebsite",
                currentWebsite
        );

        data.put(
                "requestedWebsite",
                command.getUrl()
        );

        data.put(
                "currentDomain",
                currentDomain
        );

        data.put(
                "requestedDomain",
                requestedDomain
        );

        return AIResponse.builder()
                .success(false)
                .message(
                        "This chat is already attached to "
                                + currentDomain
                                + ". To generate tests for "
                                + requestedDomain
                                + ", create a new chat from the left sidebar so each chat keeps one website and one framework context."
                )
                .type("session_guard")
                .data(data)
                .build();
    }

    // =====================================================
    // UPDATE TEST DATA
    // =====================================================

    private AIResponse updateTestData(
            AICommand command,
            String userId
    ) {

        storeCommandVariables(
                command,
                userId
        );

        FlowGraph graph =
                executionMemoryService
                        .getActiveFlowGraph(userId);

        if (
                graph != null
        ) {

            applyWorkspaceVariables(
                    graph,
                    userId
            );

            executionMemoryService
                    .storeFlowGraph(
                            userId,
                            graph
                    );
        }

        String pendingGeneratedTestTarget =
                automationWorkspaceService
                        .consumePendingGeneratedTestExecution(userId);

        if (
                pendingGeneratedTestTarget != null
                        &&
                        !pendingGeneratedTestTarget.isBlank()
        ) {

            return executeGeneratedTests(
                    AICommand.builder()
                            .intent("EXECUTE_GENERATED_TESTS")
                            .target(pendingGeneratedTestTarget)
                            .userId(userId)
                            .build(),
                    userId
            );
        }

        String pendingFrameworkUrl =
                automationWorkspaceService
                        .getPendingFrameworkGeneration(userId);

        if (
                pendingFrameworkUrl != null
                        &&
                        !pendingFrameworkUrl.isBlank()
        ) {

            return generateFramework(
                    AICommand.builder()
                            .intent("GENERATE_FRAMEWORK")
                            .url(pendingFrameworkUrl)
                            .userId(userId)
                            .build(),
                    userId
            );
        }

        return AIResponse.builder()

                .success(true)

                .message(
                        "Test data updated for the current workspace."
                )

                .type("variables")

                .data(
                        automationWorkspaceService
                                .getVariableValues(userId)
                )

                .build();
    }

    // =====================================================
    // EXECUTE FEATURE
    // =====================================================

    private AIResponse executeFeature(
            String userId
    ) {

        FlowGraph graph =
                executionMemoryService
                        .getActiveFlowGraph(userId);

        if (
                graph == null
        ) {

            return AIResponse.builder()

                    .success(false)

                    .message(
                            "No generated feature is active in the workspace."
                    )

                    .type("error")

                    .build();
        }

        applyWorkspaceVariables(
                graph,
                userId
        );

        List<String> missingVariables =
                missingRuntimeVariables(graph);

        if (
                !missingVariables.isEmpty()
        ) {

            return AIResponse.builder()

                    .success(false)

                    .message(
                            "Missing runtime variables: "
                                    + String.join(
                                    ", ",
                                    missingVariables
                            )
                    )

                    .type("error")

                    .data(missingVariables)

                    .build();
        }

        ExecutionResult result =
                graphExecutionBridge
                        .execute(graph);

        storeExecutionResult(
                result,
                userId
        );

        return executionResponse(
                result
        );
    }

    // =====================================================
    // AI EXECUTION
    // =====================================================

    private AIResponse executeAIPlan(
            AICommand command,
            String userId
    ) {

        ExecutionResult result =
                aiExecutionRuntimeExecutor
                        .execute(
                                command.getExecutionPlan()
                        );

        storeExecutionResult(
                result,
                userId
        );

        return executionResponse(result);
    }

    // =====================================================
    // EXECUTE FLOW
    // =====================================================

    private AIResponse executeFlow(
            AICommand command,
            String userId
    ) {

        FlowGraph graph =
                executionMemoryService
                        .getActiveFlowGraph(userId);

        if (
                graph == null
        ) {

            graph =
                    loadGraphFromDatabase(
                            command.getTarget()
                    );
        }

        if (
                graph == null
        ) {

            return AIResponse.builder()

                    .success(false)

                    .message(
                            "No executable flow found."
                    )

                    .type("error")

                    .build();
        }

        applyWorkspaceVariables(
                graph,
                userId
        );

        ExecutionResult result =
                graphExecutionBridge
                        .execute(graph);

        storeExecutionResult(
                result,
                userId
        );

        return executionResponse(result);
    }

    // =====================================================
    // DOWNLOAD FRAMEWORK
    // =====================================================

    private AIResponse downloadFramework(
            String userId
    ) {

        AutomationSession workspace =
                automationWorkspaceService
                        .getSession(userId);

        GeneratedArtifact artifact =
                automationWorkspaceService
                        .getLatestArtifact(
                                userId,
                                "FRAMEWORK"
                        );

        if (
                artifact == null
        ) {

            artifact =
                    zipAndStoreFrameworkArtifact(
                            workspace.getSessionId(),
                            userId
                    );
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put("artifact", artifact);
        data.put("downloadUrl", artifact.getDownloadUrl());
        data.put("sessionId", workspace.getSessionId());

        return AIResponse.builder()

                .success(true)

                .message(
                        "Framework download is ready."
                )

                .type("download")

                .downloadUrl(
                        artifact.getDownloadUrl()
                )

                .data(data)

                .build();
    }

    // =====================================================
    // GENERATED TEST TAGS
    // =====================================================

    private AIResponse showGeneratedTestTags(
            String userId
    ) {

        AutomationSession workspace =
                automationWorkspaceService
                        .getSession(userId);

        GeneratedTestExecutionService.GeneratedTestCatalog catalog =
                generatedTestExecutionService
                        .listTags(
                                workspace.getSessionId()
                        );

        return AIResponse.builder()

                .success(true)

                .message(
                        catalog.getMessage()
                )

                .type("tags")

                .data(
                        catalog
                )

                .build();
    }

    private AIResponse showGeneratedTests(
            String userId
    ) {

        AutomationSession workspace =
                automationWorkspaceService
                        .getSession(userId);

        GeneratedTestExecutionService.GeneratedTestCatalog catalog =
                generatedTestExecutionService
                        .listTests(
                                workspace.getSessionId()
                        );

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put("featureName", "generated tests");
        data.put("frameworkPath", catalog.getFrameworkRoot());
        data.put("tags", catalog.getTags());
        data.put("testCases", catalog.getTestCases());
        data.put("testCaseCount", catalog.getTestCaseCount());

        return AIResponse.builder()

                .success(true)

                .message(
                        catalog.getMessage()
                )

                .type("generated-tests")

                .data(data)

                .build();
    }

    private AIResponse executeGeneratedTests(
            AICommand command,
            String userId
    ) {

        AutomationSession workspace =
                automationWorkspaceService
                        .getSession(userId);

        String tagExpression =
                generatedTestTarget(command);

        System.out.println(
                "GENERATED TEST EXECUTION REQUEST = message="
                        + command.getMessage()
                        + ", requestedTarget="
                        + command.getTarget()
                        + ", finalTarget="
                        + tagExpression
                        + ", sessionId="
                        + workspace.getSessionId()
        );

        command.setTarget(
                tagExpression
        );

        automationWorkspaceService
                .setLastGeneratedTestExecution(
                        userId,
                        tagExpression
                );

        Map<String, String> variables =
                automationWorkspaceService
                        .getVariableValues(userId);

        List<GeneratedTestExecutionService.RuntimeVariableContext> missingVariableContexts =
                generatedTestExecutionService
                        .missingRuntimeVariableContexts(
                                workspace.getSessionId(),
                                tagExpression,
                                variables
                        );

        if (
                !missingVariableContexts.isEmpty()
        ) {

            automationWorkspaceService
                    .setPendingGeneratedTestExecution(
                            userId,
                            tagExpression
                    );

            return missingRuntimeDataResponseWithContexts(
                    missingVariableContexts
            );
        }

        if (
                useQueuedGeneratedTestExecution()
        ) {

            GeneratedTestExecutionJobEntity job =
                    generatedTestExecutionQueueService.enqueue(
                            workspace.getSessionId(),
                            userId,
                            tagExpression,
                            variables
                    );

            GeneratedTestExecutionJobDto dto =
                    generatedTestExecutionQueueService.toDto(job);

            return AIResponse.builder()

                    .success(true)

                    .message(
                            "Generated Cucumber test execution has been queued for the worker."
                    )

                    .type("generated-test-execution-queued")

                    .data(dto)

                    .build();
        }

        GeneratedTestExecutionService.GeneratedTestRunResult result =
                generatedTestExecutionService
                        .runTests(
                                workspace.getSessionId(),
                                tagExpression,
                                variables
                        );

        if (
                result.getReportUrl() != null
        ) {

            executionMemoryService
                    .storeReport(
                            userId,
                            result.getReportUrl()
                    );

            automationWorkspaceService
                    .setLatestReport(
                            userId,
                            result.getReportUrl()
                    );
        }

        return AIResponse.builder()

                .success(
                        result.isSuccess()
                )

                .message(
                        result.getMessage()
                )

                .type("generated-test-execution")

                .reportUrl(
                        result.getReportUrl()
                )

                .data(
                        result
                )

                .build();
    }

    private boolean useQueuedGeneratedTestExecution() {

        if (
                generatedTestExecutionMode == null
        ) {

            return false;
        }

        String mode =
                generatedTestExecutionMode.trim()
                        .toLowerCase();

        return "worker".equals(mode)
                ||
                "queued".equals(mode)
                ||
                "async".equals(mode);
    }

    private boolean looksLikeGeneratedTestCommand(
            String message
    ) {

        if (
                message == null
                        ||
                        message.isBlank()
        ) {

            return false;
        }

        String lower =
                message.toLowerCase();

        boolean testReference =
                lower.contains("test")
                        ||
                        lower.contains("scenario")
                        ||
                        lower.contains("@generated")
                        ||
                        lower.contains("cucumber");

        boolean generatedContext =
                lower.contains("generated")
                        ||
                        lower.contains("@")
                        ||
                        lower.contains("all")
                        ||
                        lower.contains("negative")
                        ||
                        lower.contains("coverage");

        boolean commandVerb =
                java.util.regex.Pattern.compile(
                                "\\b(?:run|execute|rerun|re-run|add|append|extend|generate|create|write|cover|update)\\b"
                        )
                        .matcher(lower)
                        .find();

        return testReference
                &&
                generatedContext
                &&
                commandVerb;
    }

    private String generatedTestTarget(
            AICommand command
    ) {

        String messageLower =
                command == null
                        ||
                        command.getMessage() == null
                        ? ""
                        : command.getMessage()
                                .trim()
                                .toLowerCase();

        String authoritativeTarget =
                authoritativeGeneratedTestTarget(
                        messageLower
                );

        if (
                authoritativeTarget != null
        ) {

            return authoritativeTarget;
        }

        if (
                command == null
                        ||
                command.getTarget() == null
                        ||
                        command.getTarget()
                                .isBlank()
        ) {

            return "ALL";
        }

        String target =
                command.getTarget()
                .trim();

        String normalizedTarget =
                target.toLowerCase();

        authoritativeTarget =
                authoritativeGeneratedTestTarget(
                        normalizedTarget
                );

        if (
                authoritativeTarget != null
        ) {

            return authoritativeTarget;
        }

        return target;
    }

    private String authoritativeGeneratedTestTarget(
            String lower
    ) {

        if (
                lower == null
        ) {

            return null;
        }

        if (
                lower.contains("@generated")
        ) {

            return "@generated";
        }

        if (
                isAllGeneratedTestSuiteRequest(lower)
        ) {

            return "ALL";
        }

        return null;
    }

    private boolean isAllGeneratedTestSuiteRequest(
            String lower
    ) {

        if (
                lower == null
        ) {

            return false;
        }

        return java.util.regex.Pattern.compile(
                        "\\b(?:all|every)\\s+(?:the\\s+)?(?:generated\\s+)?(?:cucumber\\s+)?tests?\\b"
                )
                .matcher(lower)
                .find()
                ||
                java.util.regex.Pattern.compile(
                                "\\b(?:all|every)\\s+(?:the\\s+)?(?:generated\\s+)?scenarios?\\b"
                        )
                        .matcher(lower)
                        .find();
    }

    private AIResponse repairGeneratedTests(
            AICommand command,
            String userId
    ) {

        AutomationSession workspace =
                automationWorkspaceService
                        .getSession(userId);

        rememberLatestQueuedGeneratedTestTag(
                userId,
                workspace
        );

        GeneratedTestExecutionService.GeneratedTestRepairResult result =
                generatedTestExecutionService
                        .repairLatestFailure(
                                workspace.getSessionId(),
                                command == null
                                        ? ""
                                        : command.getMessage()
                        );

        return AIResponse.builder()

                .success(true)

                .message(
                        result.getMessage()
                )

                .type("generated-test-repair")

                .data(result)

                .build();
    }

    private void rememberLatestQueuedGeneratedTestTag(

            String userId,
            AutomationSession workspace

    ) {

        if (
                generatedTestExecutionQueueService == null
                        ||
                        workspace == null
                        ||
                        workspace.getSessionId() == null
                        ||
                        workspace.getSessionId()
                                .isBlank()
        ) {

            return;
        }

        generatedTestExecutionQueueService
                .findLatestForSession(
                        workspace.getSessionId()
                )
                .map(
                        GeneratedTestExecutionJobEntity::getTagExpression
                )
                .filter(
                        tagExpression ->
                                tagExpression != null
                                        &&
                                        !tagExpression.isBlank()
                )
                .ifPresent(
                        tagExpression ->
                                automationWorkspaceService
                                        .setLastGeneratedTestExecution(
                                                userId,
                                                tagExpression
                                        )
                );
    }

    // =====================================================
    // REPORT
    // =====================================================

    private AIResponse showReport(
            String userId
    ) {

        String reportPath =
                automationWorkspaceService
                        .getSession(userId)
                        .getLatestReportPath();

        if (
                reportPath == null
        ) {

                    reportPath =
                            executionMemoryService
                            .getSession(userId)
                            .getLastReportPath();
        }

        if (
                reportPath == null
        ) {

            reportPath =
                    "No execution report available yet.";
        }

        return AIResponse.builder()

                .success(true)

                .message(
                        "Execution report ready."
                )

                .type("report")

                .reportUrl(reportPath)

                .data(reportPath)

                .build();
    }

    // =====================================================
    // DATABASE
    // =====================================================

    private AIResponse showDatabase() {

        List<StepExecutionEntity> entries =
                stepExecutionRepository
                        .findAll();

        return AIResponse.builder()

                .success(true)

                .message(
                        "Database entries fetched."
                )

                .type("database")

                .data(entries)

                .build();
    }

    // =====================================================
    // FLOW DISCOVERY
    // =====================================================

    private List<DetectedFlow> ensureWorkspaceFlows(
            AICommand command,
            String userId
    ) {

        AutomationSession workspace =
                automationWorkspaceService
                        .getSession(userId);

        if (
                workspace.getDetectedFlows() != null
                        &&
                        !workspace.getDetectedFlows()
                                .isEmpty()
        ) {

            return workspace.getDetectedFlows();
        }

        String targetUrl =
                firstNonBlank(
                        command.getUrl(),
                        workspace.getWebsiteUrl(),
                        ""
                );

        if (
                isBlank(targetUrl)
        ) {

            return List.of();
        }

        return crawlAndStoreFlows(
                targetUrl,
                userId
        );
    }

    private List<DetectedFlow> crawlAndStoreFlows(
            String url,
            String userId
    ) {

        automationWorkspaceService
                .setWebsite(
                        userId,
                        url
                );

        automationWorkspaceService
                .setDomain(
                        userId,
                        extractDomain(url)
                );

        SiteMapResult siteMap =
                websiteCrawlerService
                        .crawl(
                                url,
                                automationWorkspaceService
                                        .getVariableValues(userId)
                        );

        List<DetectedFlow> allFlows =
                new ArrayList<>();

        for (
                PageNode page
                : siteMap.getPages()
        ) {

            List<DetectedFlow> flows =
                    FlowDetectionEngine
                            .detectFlows(
                                    page.getUrl(),
                                    page.getElements()
                            );

            allFlows.addAll(flows);
        }

        allFlows =
                deduplicateFlows(allFlows);

        if (
                !allFlows.isEmpty()
        ) {

            flowPersistenceService
                    .saveFlows(allFlows);

            automationWorkspaceService
                    .storeFlows(
                            userId,
                            allFlows
                    );
        }

        return allFlows;
    }

    private List<DetectedFlow> deduplicateFlows(
            List<DetectedFlow> flows
    ) {

        Map<String, DetectedFlow> unique =
                new LinkedHashMap<>();

        for (
                DetectedFlow flow
                : flows
        ) {

            unique.putIfAbsent(
                    flowSignature(flow),
                    flow
            );
        }

        return new ArrayList<>(
                unique.values()
        );
    }

    private List<Map<String, Object>> flowSummary(
            List<DetectedFlow> flows
    ) {

        List<Map<String, Object>> summaries =
                new ArrayList<>();

        if (
                flows == null
        ) {

            return summaries;
        }

        for (
                DetectedFlow flow
                : flows
        ) {

            if (
                    flow == null
            ) {

                continue;
            }

            Map<String, Object> summary =
                    new LinkedHashMap<>();

            summary.put("flowType", flow.getFlowType());
            summary.put("pageUrl", flow.getPageUrl());
            summary.put("steps", stepSummary(flow.getSteps()));

            summaries.add(summary);
        }

        return summaries;
    }

    private List<Map<String, Object>> stepSummary(
            List<FlowStep> steps
    ) {

        List<Map<String, Object>> summaries =
                new ArrayList<>();

        if (
                steps == null
        ) {

            return summaries;
        }

        for (
                FlowStep step
                : steps
        ) {

            if (
                    step == null
            ) {

                continue;
            }

            Map<String, Object> summary =
                    new LinkedHashMap<>();

            summary.put("action", step.getAction());
            summary.put("target", step.getTarget());
            summary.put("businessRole", step.getBusinessRole());

            summaries.add(summary);
        }

        return summaries;
    }

    private String flowSignature(
            DetectedFlow flow
    ) {

        StringBuilder signature =
                new StringBuilder();

        signature.append(
                flow.getFlowType()
        );

        if (
                flow.getSteps() != null
        ) {

            for (
                    com.axiomai.qa.models.FlowStep step
                    : flow.getSteps()
            ) {

                signature.append("|")
                        .append(step.getAction())
                        .append(":")
                        .append(step.getTarget())
                        .append(":")
                        .append(step.getSelector());
            }
        }

        return signature.toString();
    }

    private List<DetectedFlow> prioritizeFlowsForCommand(

            AICommand command,

            List<DetectedFlow> flows,

            String userId

    ) {

        if (
                !hasCredentialVariables(command)
        ) {

            return flows;
        }

        List<DetectedFlow> prioritized =
                new ArrayList<>(flows);

        prioritized.sort(
                (left, right) -> Boolean.compare(
                        isLoginFlow(right),
                        isLoginFlow(left)
                )
        );

        automationWorkspaceService
                .storeFlows(
                        userId,
                        prioritized
                );

        return prioritized;
    }

    private boolean hasCredentialVariables(
            AICommand command
    ) {

        if (
                command.getVariables() == null
        ) {

            return false;
        }

        return command.getVariables()
                .containsKey("username")
                ||
                command.getVariables()
                        .containsKey("password")
                ||
                command.getVariables()
                        .containsKey("email");
    }

    private boolean isLoginFlow(
            DetectedFlow flow
    ) {

        return flow.getFlowType() != null
                &&
                flow.getFlowType()
                        .toLowerCase()
                        .contains("login");
    }

    private FlowGraph loadGraphFromDatabase(
            String target
    ) {

        if (
                target == null
                        ||
                        target.isBlank()
        ) {

            return null;
        }

        FlowEntity flow =
                flowRepository
                        .findTopByFlowNameContainingIgnoreCaseOrderByIdDesc(
                                target
                        )
                        .orElse(null);

        if (
                flow == null
        ) {

            flow =
                    flowRepository
                            .findTopByDomainNameContainingIgnoreCaseOrderByIdDesc(
                                    target
                            )
                            .orElse(null);
        }

        if (
                flow == null
        ) {

            return null;
        }

        List<ActionNode> nodes =
                new ArrayList<>();

        List<FlowStepEntity> steps =
                flowStepRepository
                        .findByFlowIdOrderByStepOrderAsc(
                                flow.getId()
                        );

        for (
                FlowStepEntity step
                : steps
        ) {

            ActionNode node =
                    ActionNode.builder()

                            .nodeId(
                                    UUID.randomUUID()
                                            .toString()
                            )

                            .actionType(
                                    step.getAction()
                            )

                            .semanticTarget(
                                    step.getElementName()
                            )

                            .primaryLocator(
                                    step.getLocatorValue()
                            )

                            .fallbackLocators(
                                    step.getFallbackLocator() == null
                                            ? List.of()
                                            : List.of(
                                            step.getFallbackLocator()
                                    )
                            )

                            .inputValue(
                                    step.getInputValue()
                            )

                            .expectedValue(
                                    step.getExpectedValue()
                            )

                            .description(
                                    step.getAiSemanticDescription()
                            )

                            .build();

            nodes.add(node);
        }

        return FlowGraph.builder()

                .graphId(
                        UUID.randomUUID()
                                .toString()
                )

                .name(
                        flow.getFlowName()
                )

                .baseUrl(
                        flow.getBaseUrl()
                )

                .sourceType("DB_FLOW")

                .nodes(nodes)

                .build();
    }

    // =====================================================
    // VARIABLES
    // =====================================================

    private void storeCommandVariables(
            AICommand command,
            String userId
    ) {

        if (
                command == null
                        ||
                        !shouldStoreCommandVariables(
                                command.getIntent()
                        )
        ) {

            return;
        }

        if (
                command.getVariables() == null
                        ||
                        command.getVariables()
                                .isEmpty()
        ) {

            return;
        }

        automationWorkspaceService
                .putVariables(
                        userId,
                        command.getVariables()
                );

        executionMemoryService
                .putRuntimeVariables(
                        userId,
                        command.getVariables()
                );
    }

    private boolean shouldStoreCommandVariables(
            String intent
    ) {

        if (
                intent == null
                        ||
                        intent.isBlank()
        ) {

            return true;
        }

        return switch (intent) {
            case "REPAIR_GENERATED_TESTS",
                 "SHOW_GENERATED_TESTS",
                 "SHOW_GENERATED_TEST_TAGS",
                 "SHOW_REPORT",
                 "SHOW_DB",
                 "DOWNLOAD_FRAMEWORK" -> false;
            default -> true;
        };
    }

    private void applyWorkspaceVariables(
            FlowGraph graph,
            String userId
    ) {

        Map<String, String> variables =
                automationWorkspaceService
                        .getVariableValues(userId);

        if (
                graph.getMetadata() == null
        ) {

            graph.setMetadata(
                    new HashMap<>()
            );
        }

        graph.getMetadata()
                .put(
                        "variables",
                        variables
                );

        for (
                ActionNode node
                : graph.getNodes()
        ) {

            if (
                    node.getActionType() == null
                            ||
                            !"TYPE".equalsIgnoreCase(
                                    node.getActionType()
                            )
            ) {

                continue;
            }

            String key =
                    extractPlaceholderKey(
                            node.getInputValue()
                    );

            if (
                    key == null
            ) {

                key =
                        variableKey(
                                node.getSemanticTarget()
                        );
            }

            if (
                    key != null
                            &&
                            variables.containsKey(
                                    key.toLowerCase()
                            )
            ) {

                node.setInputValue(
                        variables.get(
                                key.toLowerCase()
                        )
                );
            }
        }
    }

    private String extractPlaceholderKey(
            String value
    ) {

        if (
                value == null
        ) {

            return null;
        }

        String trimmed =
                value.trim();

        if (
                trimmed.startsWith("${")
                        &&
                        trimmed.endsWith("}")
        ) {

            return trimmed.substring(
                    2,
                    trimmed.length() - 1
            );
        }

        return null;
    }

    private String variableKey(
            String target
    ) {

        if (
                target == null
        ) {

            return null;
        }

        String lower =
                target.toLowerCase();

        if (
                lower.contains("user")
                        ||
                        lower.contains("login")
        ) {

            return "username";
        }

        if (
                lower.contains("password")
                        ||
                        lower.contains("pass")
        ) {

            return "password";
        }

        if (
                lower.contains("email")
        ) {

            return "email";
        }

        if (
                lower.contains("search")
        ) {

            return "search";
        }

        String key =
                lower.replaceAll(
                        "[^a-z0-9]+",
                        ""
                );

        return key.isBlank()
                ? null
                : key;
    }

    private List<String> missingRuntimeVariables(
            FlowGraph graph
    ) {

        if (
                graph == null
                        ||
                        graph.getNodes() == null
        ) {

            return List.of();
        }

        Map<String, Object> variables =
                graphVariables(graph);

        List<String> missing =
                new ArrayList<>();

        for (
                ActionNode node
                : graph.getNodes()
        ) {

            if (
                    node.getActionType() == null
                            ||
                            !"TYPE".equalsIgnoreCase(
                                    node.getActionType()
                            )
            ) {

                continue;
            }

            String key =
                    extractPlaceholderKey(
                            node.getInputValue()
                    );

            if (
                    key == null
            ) {

                continue;
            }

            if (
                    !variables.containsKey(
                            key.toLowerCase()
                    )
                            &&
                            !missing.contains(
                                    key.toLowerCase()
                            )
            ) {

                missing.add(
                        key.toLowerCase()
                );
            }
        }

        return missing;
    }

    private Map<String, Object> graphVariables(
            FlowGraph graph
    ) {

        if (
                graph.getMetadata() == null
        ) {

            return Map.of();
        }

        Object variables =
                graph.getMetadata()
                        .get("variables");

        if (
                !(variables instanceof Map<?, ?> map)
        ) {

            return Map.of();
        }

        Map<String, Object> normalized =
                new HashMap<>();

        for (
                Map.Entry<?, ?> entry
                : map.entrySet()
        ) {

            if (
                    entry.getKey() == null
            ) {

                continue;
            }

            normalized.put(
                    entry.getKey()
                            .toString()
                            .toLowerCase(),
                    entry.getValue()
            );
        }

        return normalized;
    }

    // =====================================================
    // ARTIFACTS / RESULTS
    // =====================================================

    private GeneratedArtifact zipAndStoreFrameworkArtifact(
            String sessionId,
            String userId
    ) {

        String zipPath =
                generatedProjectWriterService
                        .zipFramework(sessionId);

        generatedFrameworkPersistenceService
                .persistFrameworkArchive(
                        sessionId,
                        java.nio.file.Path.of(zipPath)
                );

        GeneratedArtifact artifact =
                GeneratedArtifact.builder()

                        .name("framework.zip")

                        .type("FRAMEWORK")

                        .path(zipPath)

                        .downloadUrl(
                                publicBaseUrlResolver.url(
                                        "/api/workspace/artifacts/"
                                                + sessionId
                                                + "/framework.zip"
                                )
                        )

                        .build();

        automationWorkspaceService
                .addArtifact(
                        userId,
                        artifact
                );

        return artifact;
    }

    private void storeExecutionResult(
            ExecutionResult result,
            String userId
    ) {

        if (
                result == null
        ) {

            return;
        }

        automationWorkspaceService
                .addExecutionHistory(
                        userId,
                        result.getMessage()
                );

        if (
                result.getReportPath() != null
        ) {

            executionMemoryService
                    .storeReport(
                            userId,
                            result.getReportPath()
                    );

            automationWorkspaceService
                    .setLatestReport(
                            userId,
                            result.getReportPath()
                    );
        }
    }

    private AIResponse executionResponse(
            ExecutionResult result
    ) {

        return AIResponse.builder()

                .success(
                        result.isSuccess()
                )

                .message(
                        result.getMessage()
                )

                .type("execution")

                .reportUrl(
                        result.getReportPath()
                )

                .data(result)

                .build();
    }

    // =====================================================
    // STRING HELPERS
    // =====================================================

    private String firstNonBlank(

            String first,

            String second,

            String fallback

    ) {

        if (
                first != null
                        &&
                        !first.isBlank()
        ) {

            return first;
        }

        if (
                second != null
                        &&
                        !second.isBlank()
        ) {

            return second;
        }

        return fallback;
    }

    private boolean isBlank(
            String value
    ) {

        return value == null
                ||
                value.isBlank();
    }

    private boolean sameText(

            String left,

            String right

    ) {

        if (
                left == null
                        ||
                        right == null
        ) {

            return false;
        }

        return left.equalsIgnoreCase(right);
    }

    private String extractDomain(
            String url
    ) {

        if (
                url == null
        ) {

            return "unknown-domain";
        }

        return url.replace("https://", "")
                .replace("http://", "")
                .replace("www.", "")
                .split("/")[0];
    }
}
