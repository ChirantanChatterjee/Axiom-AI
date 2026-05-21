package com.axiomai.runtime.engine;

import com.axiomai.execution.entity.FlowExecutionEntity;
import com.axiomai.execution.entity.StepExecutionEntity;
import com.axiomai.execution.service.ExecutionTrackingService;
import com.axiomai.flow.entity.FlowEntity;
import com.axiomai.flow.repository.FlowRepository;
import com.axiomai.flowstep.entity.FlowStepEntity;
import com.axiomai.flowstep.repository.FlowStepRepository;
import com.axiomai.qa.runtime.PlaywrightBrowserFactory;
import com.axiomai.qa.runtime.SmartActionEngine;
import com.axiomai.reporting.service.HtmlReportService;
import com.axiomai.runtime.assertion.AssertionResult;
import com.axiomai.runtime.assertion.SmartAssertionEngine;
import com.axiomai.runtime.model.ActionExecutionResult;
import com.axiomai.runtime.screenshot.ScreenshotService;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor

public class RuntimeFlowExecutor {

    private final FlowRepository flowRepository;

    private final FlowStepRepository flowStepRepository;

    private final ScreenshotService screenshotService;

    private final HtmlReportService htmlReportService;

    private final ExecutionTrackingService
            executionTrackingService;

    // =====================================================
    // MAIN EXECUTION
    // =====================================================

    public void executeFlow(Long flowId) {

        FlowExecutionEntity execution =
                executionTrackingService
                        .startExecution(flowId);

        try {

            FlowEntity flow =
                    flowRepository.findById(flowId)
                            .orElseThrow();

            List<FlowStepEntity> steps =
                    flowStepRepository
                            .findByFlowIdOrderByStepOrderAsc(flowId);

            if (steps.isEmpty()) {

                throw new RuntimeException(
                        "No steps found for flow."
                );
            }

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

                // ================================================
                // DYNAMIC URL
                // ================================================

                if (
                        flow.getBaseUrl() == null
                                ||
                                flow.getBaseUrl().isBlank()
                ) {

                    throw new RuntimeException(
                            "Flow base URL is missing."
                    );
                }

                page.navigate(
                        flow.getBaseUrl()
                );

                page.waitForLoadState();

                System.out.println(
                        "EXECUTING FLOW ON = "
                                + flow.getBaseUrl()
                );

                for (FlowStepEntity step : steps) {

                    executeStep(
                            page,
                            execution,
                            step
                    );
                }

                executionTrackingService
                        .completeExecution(
                                execution
                        );

                List<StepExecutionEntity> reportSteps =
                        executionTrackingService
                                .getStepExecutions(
                                        execution.getId()
                                );

                String reportPath =
                        htmlReportService.generateReport(
                                execution,
                                reportSteps
                        );

                System.out.println(
                        "REPORT GENERATED = "
                                + reportPath
                );

                browserContext.close();

                browser.close();
            }

        } catch (Exception e) {

            executionTrackingService
                    .failExecution(
                            execution,
                            e
                    );

            throw new RuntimeException(e);
        }
    }

    // =====================================================
    // EXECUTE STEP
    // =====================================================

    private void executeStep(

            Page page,

            FlowExecutionEntity execution,

            FlowStepEntity step

    ) {

        long startTime =
                System.currentTimeMillis();

        ActionExecutionResult actionResult =
                null;

        AssertionResult assertionResult =
                null;

        try {

            // ================================================
            // ACTION
            // ================================================

            if (isActionStep(step)) {

                List<String> selectors =
                        buildSelectors(step);

                actionResult =
                        executeSmartAction(
                                page,
                                step,
                                selectors
                        );
            }

            // ================================================
            // ASSERTION
            // ================================================

            if (isAssertionStep(step)) {

                assertionResult =
                        executeAssertion(
                                page,
                                step
                        );
            }

            long duration =
                    System.currentTimeMillis()
                            - startTime;

            String screenshotPath =

                    screenshotService
                            .takeScreenshot(

                                    page,

                                    execution.getId(),

                                    step.getStepOrder(),

                                    "PASSED"

                            );

            StepExecutionEntity entity =
                    StepExecutionEntity
                            .builder()
                            .flowExecutionId(
                                    execution.getId()
                            )
                            .stepOrder(
                                    step.getStepOrder()
                            )
                            .action(
                                    step.getAction()
                            )
                            .elementName(
                                    step.getElementName()
                            )
                            .status("PASSED")
                            .locatorStrategy(
                                    actionResult != null
                                            ? actionResult.getLocatorStrategy()
                                            : "ASSERTION"
                            )
                            .durationMs(duration)
                            .screenshotPath(
                                    screenshotPath
                            )
                            .executedAt(
                                    LocalDateTime.now()
                            )
                            .build();

            executionTrackingService
                    .saveStepExecution(entity);

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    // =====================================================
    // SMART ACTION
    // =====================================================

    private ActionExecutionResult executeSmartAction(

            Page page,

            FlowStepEntity step,

            List<String> selectors

    ) {

        return switch (
                step.getAction().toUpperCase()
                ) {

            case "TYPE" ->

                    SmartActionEngine.type(

                            page,

                            selectors,

                            step.getInputValue()

                    );

            case "CLICK" ->

                    SmartActionEngine.click(

                            page,

                            selectors

                    );

            default -> throw new RuntimeException(
                    "Unsupported action."
            );
        };
    }

    // =====================================================
    // ASSERTION
    // =====================================================

    private AssertionResult executeAssertion(
            Page page,
            FlowStepEntity step
    ) {

        String locator =
                buildLocator(step);

        return switch (
                step.getAction().toUpperCase()
                ) {

            case "VERIFY_VISIBLE" ->

                    SmartAssertionEngine
                            .verifyVisible(
                                    page,
                                    locator
                            );

            case "VERIFY_TEXT" ->

                    SmartAssertionEngine
                            .verifyText(
                                    page,
                                    locator,
                                    step.getExpectedValue()
                            );

            default -> throw new RuntimeException(
                    "Unsupported assertion."
            );
        };
    }

    // =====================================================
    // ACTION STEP
    // =====================================================

    private boolean isActionStep(
            FlowStepEntity step
    ) {

        return switch (
                step.getAction().toUpperCase()
                ) {

            case "TYPE",
                 "CLICK" -> true;

            default -> false;
        };
    }

    // =====================================================
    // ASSERTION STEP
    // =====================================================

    private boolean isAssertionStep(
            FlowStepEntity step
    ) {

        return switch (
                step.getAction().toUpperCase()
                ) {

            case "VERIFY_VISIBLE",
                 "VERIFY_TEXT" -> true;

            default -> false;
        };
    }

    // =====================================================
    // BUILD SELECTORS
    // =====================================================

    private List<String> buildSelectors(
            FlowStepEntity step
    ) {

        List<String> selectors =
                new ArrayList<>();

        selectors.add(
                buildLocator(step)
        );

        if (

                step.getFallbackLocator() != null
                        &&
                        !step.getFallbackLocator().isBlank()

        ) {

            selectors.add(
                    step.getFallbackLocator()
            );
        }

        return selectors;
    }

    // =====================================================
    // BUILD LOCATOR
    // =====================================================

    private String buildLocator(
            FlowStepEntity step
    ) {

        return switch (
                step.getLocatorType().toUpperCase()
                ) {

            case "ID" ->
                    "#" + step.getLocatorValue();

            case "XPATH" ->
                    "xpath=" + step.getLocatorValue();

            case "CSS" ->
                    step.getLocatorValue();

            default ->
                    step.getLocatorValue();
        };
    }
}
