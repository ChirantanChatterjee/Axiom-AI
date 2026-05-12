package com.axiomai.qa.generator.flow;

import com.axiomai.qa.flow.DetectedFlow;
import com.axiomai.qa.flow.FlowStep;
import com.axiomai.qa.flow.FlowType;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FlowFeatureGenerator {

    // =====================================================
    // MAIN GENERATOR
    // =====================================================

    public String generate(
            List<DetectedFlow> flows
    ) {

        StringBuilder feature =
                new StringBuilder();

        feature.append("Feature: AI Generated Flow Automation\n\n");

        for (DetectedFlow flow : flows) {

            feature.append(
                    generateScenario(flow)
            );
        }

        return feature.toString();
    }

    // =====================================================
    // GENERATE SCENARIO
    // =====================================================

    private String generateScenario(
            DetectedFlow flow
    ) {

        StringBuilder sb =
                new StringBuilder();

        sb.append("  Scenario: ");

        sb.append(buildScenarioName(flow));

        sb.append("\n");

        sb.append("    Given user launches \"")
                .append(flow.getPageUrl())
                .append("\"\n");

        for (FlowStep step : flow.getSteps()) {

            String action =
                    step.getAction();

            String role =
                    step.getTarget();

            if ("TYPE".equalsIgnoreCase(action)) {

                sb.append("    When user enters \"Playwright Java\" into ")
                        .append(role.toLowerCase())
                        .append("\n");
            }

            if ("CLICK".equalsIgnoreCase(action)) {

                sb.append("    And user clicks ")
                        .append(role.toLowerCase())
                        .append("\n");
            }
        }

        sb.append("    Then flow should complete successfully\n\n");

        return sb.toString();
    }

    // =====================================================
    // SCENARIO NAME
    // =====================================================

    private String buildScenarioName(
            DetectedFlow flow
    ) {

        FlowType type =
                flow.getFlowType();

        if (type == FlowType.SEARCH) {

            return "User performs search";
        }

        if (type == FlowType.LOGIN) {

            return "User logs into application";
        }

        if (type == FlowType.REGISTRATION) {

            return "User registers into application";
        }

        return "User performs generated flow";
    }
}