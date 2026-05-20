package com.axiomai.core.runtime;

import com.axiomai.core.execution.ExecutionResult;
import com.axiomai.core.graph.ActionNode;
import com.axiomai.core.graph.FlowGraph;
import com.axiomai.qa.models.FlowStep;
import com.axiomai.qa.runtime.AIActionExecutor;
import com.axiomai.qa.runtime.PlaywrightBrowserFactory;
import com.microsoft.playwright.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;

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

        hydrateVariables(
                graph,
                context
        );

        try (

                Playwright playwright =
                        Playwright.create()

        ) {

            Browser browser =

                    PlaywrightBrowserFactory
                            .launchVisibleChromium(playwright);

            BrowserContext browserContext =
                    browser.newContext(
                            new Browser.NewContextOptions()
                                    .setViewportSize(null)
                    );

            Page page =
                    browserContext.newPage();

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

            browserContext.close();

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

            ExecutionResult result =
                    executionTracker
                            .failExecution(
                                    context,
                                    e
                            );

            try {

                String reportPath =
                        reportManager
                                .generateReport(
                                        context
                                );

                reportManager.attachReport(
                        result,
                        reportPath
                );

            } catch (Exception reportException) {

                reportException.printStackTrace();
            }

            return result;
        }
    }

    // =====================================================
    // EXECUTE NODE
    // =====================================================

    private void executeNode(

            UnifiedRuntimeContext context,

            ActionNode node

    ) {

        long started =
                System.currentTimeMillis();

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
                            resolveInputValue(
                                    context,
                                    node
                            );

                    System.out.println(
                            "[RUNTIME DATA] "
                                    + node.getSemanticTarget()
                                    + " = "
                                    + maskIfSensitive(
                                    node.getSemanticTarget(),
                                    value
                            )
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

            recordStep(
                    context,
                    node,
                    "PASSED",
                    started,
                    null
            );

        } catch (Exception e) {

            context.getFailedNodes()
                    .add(node.getNodeId());

            recoveryManager.handleFailure(
                    context,
                    node,
                    e
            );

            recordStep(
                    context,
                    node,
                    "FAILED",
                    started,
                    e
            );

            throw new RuntimeException(e);
        }
    }

    private void recordStep(

            UnifiedRuntimeContext context,
            ActionNode node,
            String status,
            long started,
            Exception error

    ) {

        int stepOrder =
                context.getStepReports()
                        .size()
                        + 1;

        String screenshotPath =
                captureScreenshot(
                        context,
                        stepOrder,
                        status
                );

        long duration =
                System.currentTimeMillis()
                        - started;

        context.getStepReports()
                .add(
                        RuntimeStepReport.builder()
                                .stepOrder(stepOrder)
                                .nodeId(node.getNodeId())
                                .action(node.getActionType())
                                .target(node.getSemanticTarget())
                                .status(status)
                                .durationMs(duration)
                                .screenshotPath(screenshotPath)
                                .errorMessage(
                                        error == null
                                                ? null
                                                : error.getMessage()
                                )
                                .executedAt(
                                        LocalDateTime.now()
                                )
                                .build()
                );
    }

    private String captureScreenshot(

            UnifiedRuntimeContext context,
            int stepOrder,
            String status

    ) {

        try {

            if (
                    context.getPage() == null
            ) {

                return null;
            }

            Path screenshotFolder =
                    Paths.get(
                            "reports",
                            "screenshots"
                    );

            Files.createDirectories(
                    screenshotFolder
            );

            Path screenshotPath =
                    screenshotFolder.resolve(
                            context.getExecutionId()
                                    + "_step_"
                                    + stepOrder
                                    + "_"
                                    + status.toLowerCase()
                                    + ".png"
                    );

            context.getPage()
                    .screenshot(
                            new Page.ScreenshotOptions()
                                    .setPath(screenshotPath)
                                    .setFullPage(true)
                    );

            String path =
                    screenshotPath
                            .toAbsolutePath()
                            .normalize()
                            .toString();

            context.getScreenshots()
                    .add(path);

            return path;

        } catch (Exception e) {

            System.out.println(
                    "[REPORT] Screenshot capture failed: "
                            + e.getMessage()
            );

            return null;
        }
    }

    // =====================================================
    // RUNTIME INPUT RESOLUTION
    // =====================================================

    private String resolveInputValue(

            UnifiedRuntimeContext context,

            ActionNode node
    ) {

        // =================================================
        // EXPLICIT VALUE / PLACEHOLDER
        // =================================================

        if (

                node.getInputValue() != null
                        &&
                        !node.getInputValue().isBlank()

        ) {

            String inputValue =
                    node.getInputValue();

            String placeholderKey =
                    extractPlaceholderKey(
                            inputValue
                    );

            if (
                    placeholderKey == null
            ) {

                return inputValue;
            }

            Object resolved =
                    context.getVariables()
                            .get(
                                    placeholderKey.toLowerCase()
                            );

            if (
                    resolved != null
            ) {

                return resolved.toString();
            }

            throw new RuntimeException(
                    "Missing runtime variable: "
                            + placeholderKey
            );
        }

        String semanticKey =

                node.getSemanticTarget() != null
                        ?
                        variableKey(
                                node.getSemanticTarget()
                        )
                        :
                        "";

        // =================================================
        // WORKSPACE VARIABLE
        // =================================================

        if (
                !semanticKey.isBlank()

        ) {

            Object resolved =
                    context.getVariables()
                            .get(
                                    semanticKey.toLowerCase()
                            );

            if (
                    resolved != null
            ) {

                return resolved.toString();
            }
        }

        throw new RuntimeException(
                "No input value provided for "
                        + node.getSemanticTarget()
        );
    }

    // =====================================================
    // VARIABLES
    // =====================================================

    private void hydrateVariables(

            FlowGraph graph,

            UnifiedRuntimeContext context

    ) {

        if (
                graph.getMetadata() == null
        ) {

            return;
        }

        Object variables =
                graph.getMetadata()
                        .get("variables");

        if (
                variables instanceof Map<?, ?> map
        ) {

            for (
                    Map.Entry<?, ?> entry
                    : map.entrySet()
            ) {

                if (
                        entry.getKey() != null
                                &&
                                entry.getValue() != null
                ) {

                    context.getVariables()
                            .put(
                                    entry.getKey()
                                            .toString()
                                            .toLowerCase(),
                                    entry.getValue()
                            );
                }
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
            String semanticTarget
    ) {

        String semantic =
                semanticTarget == null
                        ? ""
                        : semanticTarget.toLowerCase();

        if (
                semantic.contains("user")
                        ||
                        semantic.contains("auth")
                        ||
                        semantic.contains("login")
        ) {

            return "username";
        }

        if (
                semantic.contains("password")
                        ||
                        semantic.contains("pass")
        ) {

            return "password";
        }

        if (
                semantic.contains("email")
        ) {

            return "email";
        }

        if (
                semantic.contains("search")
        ) {

            return "search";
        }

        return semantic.replaceAll(
                "[^a-z0-9]+",
                ""
        );
    }

    private String maskIfSensitive(

            String key,

            String value

    ) {

        if (
                key == null
                        ||
                        value == null
        ) {

            return value;
        }

        String lower =
                key.toLowerCase();

        if (
                lower.contains("password")
                        ||
                        lower.contains("token")
                        ||
                        lower.contains("secret")
                        ||
                        lower.contains("otp")
                        ||
                        lower.contains("username")
                        ||
                        lower.contains("email")
                        ||
                        lower.equals("user")
        ) {

            return "<redacted>";
        }

        return value;
    }
}
