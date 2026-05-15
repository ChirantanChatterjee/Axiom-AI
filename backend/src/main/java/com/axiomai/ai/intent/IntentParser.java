package com.axiomai.ai.intent;

import com.axiomai.ai.dto.AICommand;
import com.axiomai.ai.execution.AIExecutionPlan;
import com.axiomai.ai.model.GPTIntentResponse;
import com.axiomai.ai.planner.ScenarioPlanner;
import com.axiomai.ai.service.OpenAIIntentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor

public class IntentParser {

    private final OpenAIIntentService
            openAIIntentService;

    private final ScenarioPlanner
            scenarioPlanner;

    // =====================================================
    // MAIN PARSER
    // =====================================================

    public AICommand parse(
            String message
    ) {

        GPTIntentResponse response =

                openAIIntentService
                        .interpret(message);

        System.out.println(
                "GPT INTENT = "
                        + response.getIntent()
        );

        // =====================================================
        // AI EXECUTION PLAN DETECTION
        // =====================================================

        if (

                containsScenarioIntent(message)

        ) {

            AIExecutionPlan plan =
                    scenarioPlanner.plan(
                            message
                    );

            return AICommand.builder()

                    .intent("AI_EXECUTION")

                    .executionPlan(plan)

                    .message(message)

                    .build();
        }

        // =====================================================
        // OPENAI SUCCESS
        // =====================================================

        if (
                response.getIntent() != null
                        &&
                        !response.getIntent()
                                .equalsIgnoreCase("FALLBACK")
                        &&
                        !response.getIntent()
                                .equalsIgnoreCase("UNKNOWN")
        ) {

            return AICommand.builder()

                    .intent(
                            response.getIntent()
                    )

                    .flowName(
                            response.getFlowName()
                    )

                    .url(
                            response.getUrl()
                    )

                    .target(
                            extractExecutionTarget(
                                    message
                            )
                    )

                    .message(message)

                    .build();
        }

        // =====================================================
        // FALLBACK
        // =====================================================

        return localRuleParse(message);
    }

    // =====================================================
    // SCENARIO DETECTION
    // =====================================================

    private boolean containsScenarioIntent(
            String message
    ) {

        String lower =
                message.toLowerCase();

        return (

                lower.contains("login")
                        ||
                        lower.contains("search")
                        ||
                        lower.contains("play video")

        ) && (

                lower.contains("generate")
                        ||
                        lower.contains("test")

        );
    }

    // =====================================================
    // LOCAL RULE ENGINE
    // =====================================================

    private AICommand localRuleParse(
            String message
    ) {

        String lower =
                message.toLowerCase();

        // =====================================================
        // REPORT
        // =====================================================

        if (
                lower.contains("report")
        ) {

            return AICommand.builder()

                    .intent("SHOW_REPORT")

                    .message(message)

                    .build();
        }

        // =====================================================
        // DATABASE
        // =====================================================

        if (
                lower.contains("database")
                        ||
                        lower.contains("db")
        ) {

            return AICommand.builder()

                    .intent("SHOW_DB")

                    .message(message)

                    .build();
        }

        // =====================================================
        // GENERATE FRAMEWORK
        // =====================================================

        if (
                lower.contains("generate")
                        &&
                        (
                                lower.contains("framework")
                                        ||
                                        lower.contains("test")
                        )
        ) {

            return AICommand.builder()

                    .intent("GENERATE_FRAMEWORK")

                    .url(
                            extractUrl(message)
                    )

                    .message(message)

                    .build();
        }

        // =====================================================
        // EXECUTE
        // =====================================================

        if (
                lower.contains("execute")
                        ||
                        lower.contains("run")
        ) {

            return AICommand.builder()

                    .intent("EXECUTE_FLOW")

                    .target(
                            extractExecutionTarget(
                                    message
                            )
                    )

                    .message(message)

                    .build();
        }

        return AICommand.builder()

                .intent("UNKNOWN")

                .message(message)

                .build();
    }

    // =====================================================
    // TARGET EXTRACTION
    // =====================================================

    private String extractExecutionTarget(
            String message
    ) {

        String lower =
                message.toLowerCase();

        if (lower.contains("youtube")) {

            return "youtube";
        }

        if (lower.contains("orangehrm")) {

            return "orangehrm";
        }

        if (lower.contains("google")) {

            return "google";
        }

        return lower;
    }

    // =====================================================
    // URL EXTRACTION
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

        return null;
    }
}