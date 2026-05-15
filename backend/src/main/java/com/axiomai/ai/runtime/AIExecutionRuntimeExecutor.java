package com.axiomai.ai.runtime;

import com.axiomai.ai.execution.AIExecutionPlan;
import com.axiomai.ai.execution.PlannedAction;
import com.axiomai.ai.execution.ScenarioPlan;
import com.axiomai.qa.models.FlowStep;
import com.axiomai.qa.runtime.AIActionExecutor;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor

public class AIExecutionRuntimeExecutor {

    private final SemanticActionMapper
            semanticActionMapper;

    private final RuntimeValueResolver
            valueResolver;

    // =====================================================
    // EXECUTE PLAN
    // =====================================================

    public void execute(
            AIExecutionPlan plan
    ) {

        ExecutionSession session =
                ExecutionSession.builder()

                        .status("RUNNING")

                        .targetUrl(
                                plan.getTargetUrl()
                        )

                        .build();

        System.out.println(
                "[AI EXECUTION STARTED]"
        );

        System.out.println(
                "[SESSION] "
                        + session.getSessionId()
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

                                            .setHeadless(
                                                    plan.isHeadless()
                                            )

                            );

            Page page =
                    browser.newPage();

            page.navigate(
                    plan.getTargetUrl()
            );

            page.waitForLoadState();

            RuntimeExecutionContext context =

                    RuntimeExecutionContext
                            .builder()

                            .page(page)

                            .variableContext(
                                    plan.getVariables()
                            )

                            .build();

            // =================================================
            // EXECUTE SCENARIOS
            // =================================================

            for (

                    ScenarioPlan scenario
                    : plan.getScenarios()

            ) {

                executeScenario(
                        context,
                        scenario
                );
            }

            session.setStatus("COMPLETED");

            System.out.println(
                    "[AI EXECUTION COMPLETED]"
            );

            browser.close();

        } catch (Exception e) {

            session.setStatus("FAILED");

            System.out.println(
                    "[AI EXECUTION FAILED]"
            );

            e.printStackTrace();

            throw new RuntimeException(e);
        }
    }

    // =====================================================
    // EXECUTE SCENARIO
    // =====================================================

    private void executeScenario(

            RuntimeExecutionContext context,

            ScenarioPlan scenario

    ) {

        System.out.println(
                "[SCENARIO] "
                        + scenario.getScenarioName()
        );

        context.setCurrentScenario(
                scenario.getScenarioName()
        );

        for (

                PlannedAction action
                : scenario.getActions()

        ) {

            executeAction(
                    context,
                    action
            );
        }
    }

    // =====================================================
    // EXECUTE ACTION
    // =====================================================

    private void executeAction(

            RuntimeExecutionContext context,

            PlannedAction action

    ) {

        System.out.println(
                "[ACTION] "
                        + action.getActionType()
                        + " -> "
                        + action.getSemanticTarget()
        );

        FlowStep flowStep =
                semanticActionMapper
                        .map(action);

        switch (

                action.getActionType()
                        .toUpperCase()

        ) {

            // =============================================
            // TYPE
            // =============================================

            case "TYPE" -> {

                String value =
                        valueResolver.resolve(

                                context.getVariableContext(),

                                action.getVariableKey()

                        );

                AIActionExecutor.type(

                        context.getPage(),

                        flowStep,

                        value

                );
            }

            // =============================================
            // CLICK
            // =============================================

            case "CLICK" ->

                    AIActionExecutor.click(

                            context.getPage(),

                            flowStep

                    );

            // =============================================
            // UNSUPPORTED
            // =============================================

            default -> throw new RuntimeException(

                    "Unsupported action type: "
                            + action.getActionType()

            );
        }
    }
}