package com.axiomai.ai.planner;

import com.axiomai.ai.execution.AIExecutionPlan;
import com.axiomai.ai.execution.PlannedAction;
import com.axiomai.ai.execution.RuntimeVariableContext;
import com.axiomai.ai.execution.ScenarioPlan;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component

public class ScenarioPlanner {

    // =====================================================
    // MAIN PLANNER
    // =====================================================

    public AIExecutionPlan plan(
            String message
    ) {

        ScenarioExtractionResult extraction =
                extract(message);

        RuntimeVariableContext context =
                buildRuntimeContext(extraction);

        List<ScenarioPlan> scenarioPlans =
                buildScenarioPlans(
                        extraction.getScenarios()
                );

        return AIExecutionPlan.builder()

                .targetUrl(
                        extraction.getTargetUrl()
                )

                .variables(context)

                .scenarios(scenarioPlans)

                .executionMode("AI_RUNTIME")

                .headless(false)

                .overlayHandlingEnabled(true)

                .build();
    }

    // =====================================================
    // EXTRACTION
    // =====================================================

    private ScenarioExtractionResult extract(
            String message
    ) {

        String lower =
                message.toLowerCase();

        List<String> scenarios =
                new ArrayList<>();

        Map<String, String> variables =
                new HashMap<>();

        // =================================================
        // URL EXTRACTION
        // =================================================

        String targetUrl = extractUrl(message);

        // =================================================
        // LOGIN
        // =================================================

        if (

                lower.contains("login")
                        ||
                        lower.contains("sign in")

        ) {

            scenarios.add("LOGIN");
        }

        // =================================================
        // SEARCH
        // =================================================

        if (

                lower.contains("search")

        ) {

            scenarios.add("SEARCH");
        }

        // =================================================
        // PLAY VIDEO
        // =================================================

        if (

                lower.contains("play video")
                        ||
                        lower.contains("play first video")

        ) {

            scenarios.add("PLAY_VIDEO");
        }

        // =================================================
        // USERNAME
        // =================================================

        if (lower.contains("username")) {

            String username =
                    extractVariable(
                            message,
                            "username"
                    );

            variables.put(
                    "username",
                    username
            );
        }

        // =================================================
        // PASSWORD
        // =================================================

        if (lower.contains("password")) {

            String password =
                    extractVariable(
                            message,
                            "password"
                    );

            variables.put(
                    "password",
                    password
            );
        }

        // =================================================
        // SEARCH TEXT
        // =================================================

        if (

                lower.contains("search text")

        ) {

            String searchText =
                    extractVariable(
                            message,
                            "search text"
                    );

            variables.put(
                    "searchText",
                    searchText
            );
        }

        return ScenarioExtractionResult.builder()

                .targetUrl(targetUrl)

                .scenarios(scenarios)

                .variables(variables)

                .build();
    }

    // =====================================================
    // BUILD CONTEXT
    // =====================================================

    private RuntimeVariableContext buildRuntimeContext(

            ScenarioExtractionResult extraction

    ) {

        RuntimeVariableContext context =
                new RuntimeVariableContext();

        extraction.getVariables()

                .forEach(
                        context::addVariable
                );

        return context;
    }

    // =====================================================
    // BUILD SCENARIOS
    // =====================================================

    private List<ScenarioPlan> buildScenarioPlans(

            List<String> scenarios

    ) {

        List<ScenarioPlan> plans =
                new ArrayList<>();

        for (String scenario : scenarios) {

            switch (scenario) {

                case "LOGIN" ->

                        plans.add(
                                buildLoginScenario()
                        );

                case "SEARCH" ->

                        plans.add(
                                buildSearchScenario()
                        );

                case "PLAY_VIDEO" ->

                        plans.add(
                                buildPlayVideoScenario()
                        );
            }
        }

        return plans;
    }

    // =====================================================
    // LOGIN PLAN
    // =====================================================

    private ScenarioPlan buildLoginScenario() {

        List<PlannedAction> actions =
                new ArrayList<>();

        actions.add(

                PlannedAction.builder()

                        .actionType("CLICK")

                        .semanticTarget("SIGN_IN_BUTTON")

                        .description(
                                "Click sign in button"
                        )

                        .build()
        );

        actions.add(

                PlannedAction.builder()

                        .actionType("TYPE")

                        .semanticTarget("USERNAME_FIELD")

                        .variableKey("username")

                        .description(
                                "Enter username"
                        )

                        .build()
        );

        actions.add(

                PlannedAction.builder()

                        .actionType("TYPE")

                        .semanticTarget("PASSWORD_FIELD")

                        .variableKey("password")

                        .description(
                                "Enter password"
                        )

                        .build()
        );

        actions.add(

                PlannedAction.builder()

                        .actionType("CLICK")

                        .semanticTarget("LOGIN_BUTTON")

                        .description(
                                "Submit login"
                        )

                        .build()
        );

        return ScenarioPlan.builder()

                .scenarioName("LOGIN")

                .description(
                        "User login scenario"
                )

                .actions(actions)

                .enabled(true)

                .build();
    }

    // =====================================================
    // SEARCH PLAN
    // =====================================================

    private ScenarioPlan buildSearchScenario() {

        List<PlannedAction> actions =
                new ArrayList<>();

        actions.add(

                PlannedAction.builder()

                        .actionType("TYPE")

                        .semanticTarget("SEARCH_BOX")

                        .variableKey("searchText")

                        .description(
                                "Search for content"
                        )

                        .build()
        );

        actions.add(

                PlannedAction.builder()

                        .actionType("CLICK")

                        .semanticTarget("SEARCH_BUTTON")

                        .description(
                                "Execute search"
                        )

                        .build()
        );

        return ScenarioPlan.builder()

                .scenarioName("SEARCH")

                .description(
                        "Search scenario"
                )

                .actions(actions)

                .enabled(true)

                .build();
    }

    // =====================================================
    // PLAY VIDEO PLAN
    // =====================================================

    private ScenarioPlan buildPlayVideoScenario() {

        List<PlannedAction> actions =
                new ArrayList<>();

        actions.add(

                PlannedAction.builder()

                        .actionType("CLICK")

                        .semanticTarget("FIRST_VIDEO")

                        .description(
                                "Play first video"
                        )

                        .build()
        );

        return ScenarioPlan.builder()

                .scenarioName("PLAY_VIDEO")

                .description(
                        "Play searched video"
                )

                .actions(actions)

                .enabled(true)

                .build();
    }

    // =====================================================
    // EXTRACT URL
    // =====================================================

    private String extractUrl(
            String message
    ) {

        String[] tokens =
                message.split(" ");

        for (String token : tokens) {

            if (

                    token.startsWith("http://")
                            ||
                            token.startsWith("https://")

            ) {

                return token;
            }
        }

        if (

                message.toLowerCase()
                        .contains("youtube")

        ) {

            return "https://www.youtube.com";
        }

        return null;
    }

    // =====================================================
    // VARIABLE EXTRACTION
    // =====================================================

    private String extractVariable(

            String message,
            String variableName

    ) {

        try {

            String lower =
                    message.toLowerCase();

            int index =
                    lower.indexOf(variableName);

            if (index == -1) {

                return null;
            }

            String remaining =
                    message.substring(index);

            String[] split =
                    remaining.split(":");

            if (split.length < 2) {

                return null;
            }

            String value =
                    split[1]
                            .split(",")[0]
                            .trim();

            return value;

        } catch (Exception e) {

            return null;
        }
    }
}