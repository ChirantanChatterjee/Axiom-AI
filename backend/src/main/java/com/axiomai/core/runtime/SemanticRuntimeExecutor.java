package com.axiomai.core.runtime;

import com.axiomai.core.execution.ExecutionResult;
import com.axiomai.core.graph.ActionNode;
import com.axiomai.core.graph.FlowGraph;
import com.axiomai.qa.models.FlowStep;
import com.axiomai.qa.runtime.AIActionExecutor;
import com.microsoft.playwright.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor

public class SemanticRuntimeExecutor {

    private final RuntimeExecutionTracker
            executionTracker;

    private final RuntimeRecoveryManager
            recoveryManager;

    private final RuntimeReportManager
            reportManager;

    private final LocatorResolverFacade
            locatorResolverFacade;

    // =====================================================
    // EXECUTE FLOW GRAPH
    // =====================================================

    public ExecutionResult execute(
            FlowGraph graph
    ) {

        UnifiedRuntimeContext context =
                executionTracker
                        .startExecution(
                                graph.getName()
                        );

        try (

                Playwright playwright =
                        Playwright.create()

        ) {

            Browser browser =

                    playwright.chromium()
                            .launch(

                                    new BrowserType
                                            .LaunchOptions()

                                            .setHeadless(false)

                            );

            Page page =
                    browser.newPage();

            context.setPage(page);

            // =================================================
            // NAVIGATION
            // =================================================

            if (

                    graph.getBaseUrl() != null
                            &&
                            !graph.getBaseUrl().isBlank()

            ) {

                page.navigate(
                        graph.getBaseUrl()
                );

                page.waitForLoadState();
            }

            // =================================================
            // EXECUTE NODES
            // =================================================

            for (

                    ActionNode node
                    : graph.getNodes()

            ) {

                executeNode(
                        context,
                        node
                );
            }

            browser.close();

            // =================================================
            // REPORT
            // =================================================

            String reportPath =
                    reportManager
                            .generateReport(
                                    context
                            );

            // =================================================
            // RESULT
            // =================================================

            ExecutionResult result =
                    executionTracker
                            .completeExecution(
                                    context
                            );

            reportManager.attachReport(
                    result,
                    reportPath
            );

            return result;

        } catch (Exception e) {

            e.printStackTrace();

            return executionTracker
                    .failExecution(
                            context,
                            e
                    );
        }
    }

    // =====================================================
    // EXECUTE NODE
    // =====================================================

    private void executeNode(

            UnifiedRuntimeContext context,

            ActionNode node

    ) {

        try {

            System.out.println(
                    "[NODE] "
                            + node.getActionType()
                            + " -> "
                            + node.getSemanticTarget()
            );

            FlowStep flowStep =
                    locatorResolverFacade
                            .resolve(node);

            switch (

                    node.getActionType()
                            .toUpperCase()

            ) {

                // ============================================
                // CLICK
                // ============================================

                case "CLICK" ->

                        AIActionExecutor.click(

                                context.getPage(),

                                flowStep

                        );

                // ============================================
                // TYPE
                // ============================================

                case "TYPE" -> {

                    String value =
                            resolveInputValue(node);

                    System.out.println(
                            "[RUNTIME DATA] "
                                    + node.getSemanticTarget()
                                    + " = "
                                    + value
                    );

                    AIActionExecutor.type(

                            context.getPage(),

                            flowStep,

                            value

                    );
                }

                // ============================================
                // WAIT
                // ============================================

                case "WAIT" ->

                        context.getPage()
                                .waitForTimeout(2000);

                // ============================================
                // NAVIGATE
                // ============================================

                case "NAVIGATE" -> {

                    if (
                            node.getInputValue() != null
                    ) {

                        context.getPage()
                                .navigate(
                                        node.getInputValue()
                                );

                        context.getPage()
                                .waitForLoadState();
                    }
                }

                // ============================================
                // UNSUPPORTED
                // ============================================

                default -> throw new RuntimeException(

                        "Unsupported action type: "
                                + node.getActionType()

                );
            }

            context.getExecutedNodes()
                    .add(node.getNodeId());

        } catch (Exception e) {

            context.getFailedNodes()
                    .add(node.getNodeId());

            recoveryManager.handleFailure(
                    context,
                    node,
                    e
            );

            throw new RuntimeException(e);
        }
    }

    // =====================================================
    // RUNTIME INPUT RESOLUTION
    // =====================================================

    private String resolveInputValue(
            ActionNode node
    ) {

        // =================================================
        // EXPLICIT VALUE
        // =================================================

        if (

                node.getInputValue() != null
                        &&
                        !node.getInputValue().isBlank()

        ) {

            return node.getInputValue();
        }

        String semantic =

                node.getSemanticTarget() != null
                        ?
                        node.getSemanticTarget()
                                .toLowerCase()
                        :
                        "";

        // =================================================
        // USERNAME
        // =================================================

        if (

                semantic.contains("user")
                        ||
                        semantic.contains("email")
                        ||
                        semantic.contains("login")

        ) {

            return "standard_user";
        }

        // =================================================
        // PASSWORD
        // =================================================

        if (

                semantic.contains("password")
                        ||
                        semantic.contains("pass")

        ) {

            return "secret_sauce";
        }

        // =================================================
        // DEFAULT
        // =================================================

        return "test-data";
    }
}