package com.axiomai.ai.orchestrator;

import com.axiomai.ai.dto.AICommand;
import com.axiomai.ai.dto.AIResponse;
import com.axiomai.ai.runtime.AIExecutionRuntimeExecutor;
import com.axiomai.core.adapter.DetectedFlowAdapter;
import com.axiomai.core.adapter.ScenarioPlanAdapter;
import com.axiomai.core.execution.ExecutionResult;
import com.axiomai.core.execution.GraphExecutionBridge;
import com.axiomai.core.graph.FlowGraph;
import com.axiomai.core.memory.ExecutionMemoryService;
import com.axiomai.execution.entity.StepExecutionEntity;
import com.axiomai.execution.repository.StepExecutionRepository;
import com.axiomai.flow.entity.FlowEntity;
import com.axiomai.flow.repository.FlowRepository;
import com.axiomai.qa.flow.DetectedFlow;
import com.axiomai.qa.flow.FlowDetectionEngine;
import com.axiomai.qa.models.GeneratedFramework;
import com.axiomai.qa.models.PageNode;
import com.axiomai.qa.models.SiteMapResult;
import com.axiomai.qa.service.FlowPersistenceService;
import com.axiomai.qa.service.FrameworkGeneratorService;
import com.axiomai.qa.service.WebsiteCrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor

public class AICommandOrchestrator {

    private final FlowRepository
            flowRepository;

    private final StepExecutionRepository
            stepExecutionRepository;

    private final WebsiteCrawlerService
            websiteCrawlerService;

    private final FrameworkGeneratorService
            frameworkGeneratorService;

    private final FlowPersistenceService
            flowPersistenceService;

    private final AIExecutionRuntimeExecutor
            aiExecutionRuntimeExecutor;

    private final ExecutionMemoryService
            executionMemoryService;

    private final DetectedFlowAdapter
            detectedFlowAdapter;

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

        String userId =
                "default-user";

        try {

            System.out.println(
                    "ORCHESTRATOR INTENT = "
                            + command.getIntent()
            );

            switch (command.getIntent()) {

                case "GENERATE_FRAMEWORK" -> {

                    return generateFramework(
                            userId,
                            command
                    );
                }

                case "AI_EXECUTION" -> {

                    return executeAIPlan(
                            userId,
                            command
                    );
                }

                case "EXECUTE_FLOW" -> {

                    return executeFlow(
                            userId,
                            command
                    );
                }

                case "SHOW_REPORT" -> {

                    return showReport(
                            userId
                    );
                }

                case "SHOW_DB" -> {

                    return showDatabase();
                }

                default -> {

                    return AIResponse.builder()

                            .success(false)

                            .message(
                                    "AIF could not understand the request."
                            )

                            .build();
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            return AIResponse.builder()

                    .success(false)

                    .message(
                            e.getMessage()
                    )

                    .build();
        }
    }

    // =====================================================
    // GENERATE FRAMEWORK
    // =====================================================

    private AIResponse generateFramework(

            String userId,

            AICommand command

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

                    .build();
        }

        SiteMapResult siteMap =

                websiteCrawlerService
                        .crawl(
                                command.getUrl()
                        );

        List<DetectedFlow> allFlows =
                new ArrayList<>();

        for (PageNode page : siteMap.getPages()) {

            List<DetectedFlow> flows =

                    FlowDetectionEngine
                            .detectFlows(

                                    page.getUrl(),
                                    page.getElements()
                            );

            allFlows.addAll(flows);
        }

        if (allFlows.isEmpty()) {

            return AIResponse.builder()

                    .success(false)

                    .message(
                            "No executable flows detected."
                    )

                    .build();
        }

        GeneratedFramework framework =

                frameworkGeneratorService
                        .generate(allFlows);

        flowPersistenceService
                .saveFlows(allFlows);

        // =================================================
        // STORE PRIMARY GRAPH
        // =================================================

        DetectedFlow primaryFlow =
                allFlows.get(0);

        FlowGraph graph =

                detectedFlowAdapter
                        .convert(primaryFlow);

        executionMemoryService
                .storeFlowGraph(
                        userId,
                        graph
                );

        executionMemoryService
                .getSession(userId)
                .setActiveUrl(
                        command.getUrl()
                );

        System.out.println(
                "FLOW GRAPH STORED = "
                        + graph.getName()
        );

        return AIResponse.builder()

                .success(true)

                .message(
                        "Framework generated successfully."
                )

                .data(framework)

                .build();
    }

    // =====================================================
    // AI EXECUTION
    // =====================================================

    private AIResponse executeAIPlan(

            String userId,

            AICommand command

    ) {

        ExecutionResult result =

                aiExecutionRuntimeExecutor
                        .execute(
                                command.getExecutionPlan()
                        );

        return AIResponse.builder()

                .success(
                        result.isSuccess()
                )

                .message(
                        result.getMessage()
                )

                .data(result)

                .build();
    }

    // =====================================================
    // EXECUTE FLOW
    // =====================================================

    private AIResponse executeFlow(

            String userId,

            AICommand command

    ) {

        // =================================================
        // ALWAYS PRIORITIZE MEMORY GRAPH
        // =================================================

        FlowGraph graph =

                executionMemoryService
                        .getActiveFlowGraph(
                                userId
                        );

        // =================================================
        // DEBUG
        // =================================================

        if (graph != null) {

            System.out.println(
                    "USING MEMORY FLOW GRAPH = "
                            + graph.getName()
            );

            System.out.println(
                    "NODE COUNT = "
                            + graph.getNodes().size()
            );
        }

        // =================================================
        // FALLBACK TO DB
        // =================================================

        if (graph == null) {

            FlowEntity flow =

                    flowRepository

                            .findTopByFlowNameContainingIgnoreCaseOrderByIdDesc(
                                    command.getTarget()
                            )

                            .orElse(null);

            if (flow == null) {

                return AIResponse.builder()

                        .success(false)

                        .message(
                                "No executable flow found."
                        )

                        .build();
            }

            graph = FlowGraph.builder()

                    .name(
                            flow.getFlowName()
                    )

                    .baseUrl(
                            flow.getBaseUrl()
                    )

                    .sourceType("DB_FLOW")

                    .build();
        }

        // =================================================
        // EXECUTE
        // =================================================

        ExecutionResult result =

                graphExecutionBridge
                        .execute(graph);

        return AIResponse.builder()

                .success(
                        result.isSuccess()
                )

                .message(
                        result.getMessage()
                )

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

                executionMemoryService

                        .getSession(userId)

                        .getLastReportPath();

        if (reportPath == null) {

            reportPath =
                    "No execution report available yet.";
        }

        return AIResponse.builder()

                .success(true)

                .message(
                        "Execution report ready."
                )

                .data(reportPath)

                .build();
    }

    // =====================================================
    // DATABASE
    // =====================================================

    private AIResponse showDatabase() {

        List<StepExecutionEntity>
                entries =

                stepExecutionRepository
                        .findAll();

        return AIResponse.builder()

                .success(true)

                .message(
                        "Database entries fetched."
                )

                .data(entries)

                .build();
    }
}