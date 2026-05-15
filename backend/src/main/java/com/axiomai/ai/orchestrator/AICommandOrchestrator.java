package com.axiomai.ai.orchestrator;

import com.axiomai.ai.dto.AICommand;
import com.axiomai.ai.dto.AIResponse;
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
import com.axiomai.runtime.engine.RuntimeFlowExecutor;
import com.axiomai.ai.runtime.AIExecutionRuntimeExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor

public class AICommandOrchestrator {

    private final RuntimeFlowExecutor
            runtimeFlowExecutor;

    private final StepExecutionRepository
            stepExecutionRepository;

    private final FlowRepository
            flowRepository;

    private final WebsiteCrawlerService
            websiteCrawlerService;

    private final FrameworkGeneratorService
            frameworkGeneratorService;

    private final FlowPersistenceService
            flowPersistenceService;

    private final AIExecutionRuntimeExecutor
            aiExecutionRuntimeExecutor;

    // =====================================================
    // MAIN EXECUTOR
    // =====================================================

    public AIResponse execute(
            AICommand command
    ) {

        try {

            switch (command.getIntent()) {

                // =====================================================
                // GENERATE FRAMEWORK
                // =====================================================

                case "GENERATE_FRAMEWORK" -> {

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

                    // =====================================================
                    // CRAWL WEBSITE
                    // =====================================================

                    SiteMapResult siteMap =

                            websiteCrawlerService
                                    .crawl(
                                            command.getUrl()
                                    );

                    // =====================================================
                    // DETECT FLOWS
                    // =====================================================

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

                    // =====================================================
                    // NO FLOWS DETECTED
                    // =====================================================

                    if (allFlows.isEmpty()) {

                        return AIResponse.builder()

                                .success(false)

                                .message(
                                        "No executable flows detected for: "
                                                + command.getUrl()
                                )

                                .build();
                    }

                    // =====================================================
                    // GENERATE FRAMEWORK
                    // =====================================================

                    GeneratedFramework framework =

                            frameworkGeneratorService
                                    .generate(allFlows);

                    // =====================================================
                    // DEBUG LOGGING
                    // =====================================================

                    System.out.println(
                            "TOTAL FLOWS DETECTED = "
                                    + allFlows.size()
                    );

                    for (DetectedFlow flow : allFlows) {

                        System.out.println(
                                "FLOW DETECTED -> "
                                        + flow.getFlowType()
                                        + " | "
                                        + flow.getPageUrl()
                        );
                    }

                    // =====================================================
                    // SAVE FLOWS
                    // =====================================================

                    flowPersistenceService
                            .saveFlows(allFlows);

                    // =====================================================
                    // RESPONSE
                    // =====================================================

                    return AIResponse.builder()

                            .success(true)

                            .message(
                                    "Framework generated successfully for "
                                            + command.getUrl()
                            )

                            .data(framework)

                            .build();
                }

                // =====================================================
                // AI EXECUTION
                // =====================================================

                case "AI_EXECUTION" -> {

                    aiExecutionRuntimeExecutor.execute(
                            command.getExecutionPlan()
                    );

                    return AIResponse.builder()

                            .success(true)

                            .message(
                                    "AI execution completed successfully."
                            )

                            .data(
                                    command.getExecutionPlan()
                            )

                            .build();
                }

                // =====================================================
                // EXECUTE FLOW
                // =====================================================

                case "EXECUTE_FLOW" -> {

                    FlowEntity flow = null;

                    // ================================================
                    // SEARCH TARGET
                    // ================================================

                    if (
                            command.getTarget() != null
                                    &&
                                    !command.getTarget().isBlank()
                    ) {

                        // ============================================
                        // NORMALIZE TARGET
                        // ============================================

                        String target =

                                command.getTarget()

                                        .toLowerCase()

                                        .replace("tests", "")
                                        .replace("test", "")
                                        .replace("flows", "")
                                        .replace("flow", "")
                                        .replace("run", "")
                                        .replace("execute", "")
                                        .trim();

                        System.out.println(
                                "EXECUTION TARGET = "
                                        + target
                        );

                        // ============================================
                        // SEARCH BY FLOW NAME
                        // ============================================

                        flow = flowRepository

                                .findTopByFlowNameContainingIgnoreCaseOrderByIdDesc(
                                        target
                                )

                                .orElse(null);

                        // ============================================
                        // SEARCH DOMAIN
                        // ============================================

                        if (flow == null) {

                            flow = flowRepository

                                    .findTopByDomainNameContainingIgnoreCaseOrderByIdDesc(
                                            target
                                    )

                                    .orElse(null);
                        }
                    }

                    // ================================================
                    // FLOW NOT FOUND
                    // ================================================

                    if (flow == null) {

                        return AIResponse.builder()

                                .success(false)

                                .message(
                                        "No generated flow found for: "
                                                + command.getTarget()
                                )

                                .build();
                    }

                    // ================================================
                    // DEBUG
                    // ================================================

                    System.out.println(
                            "EXECUTING FLOW = "
                                    + flow.getFlowName()
                    );

                    System.out.println(
                            "FLOW BASE URL = "
                                    + flow.getBaseUrl()
                    );

                    // ================================================
                    // EXECUTION
                    // ================================================

                    runtimeFlowExecutor.executeFlow(
                            flow.getId()
                    );

                    return AIResponse.builder()

                            .success(true)

                            .message(
                                    "Flow executed successfully: "
                                            + flow.getFlowName()
                            )

                            .data(flow)

                            .build();
                }

                // =====================================================
                // SHOW REPORT
                // =====================================================

                case "SHOW_REPORT" -> {

                    return AIResponse.builder()

                            .success(true)

                            .message(
                                    "Execution report ready."
                            )

                            .data(
                                    "http://localhost:8080/reports/execution_15.html"
                            )

                            .build();
                }

                // =====================================================
                // SHOW DATABASE
                // =====================================================

                case "SHOW_DB" -> {

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

                // =====================================================
                // UNKNOWN
                // =====================================================

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
}