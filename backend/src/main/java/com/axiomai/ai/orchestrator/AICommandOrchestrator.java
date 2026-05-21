package com.axiomai.ai.orchestrator;

import com.axiomai.ai.dto.AICommand;
import com.axiomai.ai.dto.AIResponse;
import com.axiomai.ai.runtime.AIExecutionRuntimeExecutor;
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
import com.axiomai.qa.models.GeneratedFramework;
import com.axiomai.qa.models.PageNode;
import com.axiomai.qa.models.SiteMapResult;
import com.axiomai.qa.service.FlowPersistenceService;
import com.axiomai.qa.service.FrameworkGeneratorService;
import com.axiomai.qa.service.GeneratedTestExecutionService;
import com.axiomai.qa.service.GeneratedProjectWriterService;
import com.axiomai.qa.service.RequirementTestCaseGeneratorService;
import com.axiomai.qa.service.WebsiteCrawlerService;
import com.axiomai.workspace.AutomationSession;
import com.axiomai.workspace.AutomationWorkspaceService;
import com.axiomai.workspace.GeneratedArtifact;
import lombok.RequiredArgsConstructor;
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

    private final GeneratedTestExecutionService
            generatedTestExecutionService;

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

    // =====================================================
    // MAIN EXECUTION
    // =====================================================

    public AIResponse execute(
            AICommand command
    ) {

        try {

            String userId =
                    userId(command);

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

                case "EXECUTE_GENERATED_TESTS" ->
                        executeGeneratedTests(
                                command,
                                userId
                        );

                case "REPAIR_GENERATED_TESTS" ->
                        repairGeneratedTests(userId);

                case "SHOW_REPORT" ->
                        showReport(userId);

                case "SHOW_DB" ->
                        showDatabase();

                default -> AIResponse.builder()

                        .success(false)

                        .message(
                                "AIF could not understand the request."
                        )

                        .type("error")

                        .build();
            };

        } catch (Exception e) {

            e.printStackTrace();

            AIResponse missingRuntimeDataResponse =
                    missingRuntimeDataResponse(e);

            if (
                    missingRuntimeDataResponse != null
            ) {

                return missingRuntimeDataResponse;
            }

            return AIResponse.builder()

                    .success(false)

                    .message(
                            e.getMessage()
                    )

                    .type("error")

                    .build();
        }
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
                isBlank(currentWebsite)
                        ||
                        isBlank(requestedWebsite)
        ) {

            return null;
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
                        ||
                        isBlank(
                                workspace.getWebsiteUrl()
                        )
        ) {

            return false;
        }

        return hasFrameworkArtifact(workspace)
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

        return AIResponse.builder()

                .success(true)

                .message(
                        "Feature generated and linked to the current workspace."
                )

                .type("feature")

                .downloadUrl(
                        artifact.getDownloadUrl()
                )

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

    private AIResponse executeGeneratedTests(
            AICommand command,
            String userId
    ) {

        AutomationSession workspace =
                automationWorkspaceService
                        .getSession(userId);

        Map<String, String> variables =
                automationWorkspaceService
                        .getVariableValues(userId);

        GeneratedTestExecutionService.GeneratedTestRunResult result =
                generatedTestExecutionService
                        .runTests(
                                workspace.getSessionId(),
                                command.getTarget(),
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

    private AIResponse repairGeneratedTests(
            String userId
    ) {

        AutomationSession workspace =
                automationWorkspaceService
                        .getSession(userId);

        GeneratedTestExecutionService.GeneratedTestRepairResult result =
                generatedTestExecutionService
                        .repairLatestFailure(
                                workspace.getSessionId()
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

        if (
                command.getUrl() == null
                        ||
                        command.getUrl().isBlank()
        ) {

            return List.of();
        }

        return crawlAndStoreFlows(
                command.getUrl(),
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
                        .crawl(url);

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

        GeneratedArtifact artifact =
                GeneratedArtifact.builder()

                        .name("framework.zip")

                        .type("FRAMEWORK")

                        .path(zipPath)

                        .downloadUrl(
                                "http://localhost:8080/api/workspace/artifacts/"
                                        + sessionId
                                        + "/framework.zip"
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
