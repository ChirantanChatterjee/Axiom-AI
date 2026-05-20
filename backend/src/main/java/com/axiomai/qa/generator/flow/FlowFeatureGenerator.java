package com.axiomai.qa.generator.flow;

import com.axiomai.qa.flow.DetectedFlow;
import com.axiomai.qa.models.FlowStep;

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

        feature.append(
                "Feature: AI Generated Flow Automation\n\n"
        );

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

        sb.append("  @generated @flow_")
                .append(
                        tagFor(flow)
                )
                .append(" @")
                .append(
                        tagFor(flow)
                )
                .append("\n");

        sb.append("  Scenario: ");

        sb.append(
                buildScenarioName(flow)
        );

        sb.append("\n");

        sb.append("    Given user launches \"")
                .append(flow.getPageUrl())
                .append("\"\n");

        for (FlowStep step : flow.getSteps()) {

            String action =
                    step.getAction();

            String role =
                    step.getTarget();

            if (
                    "TYPE".equalsIgnoreCase(action)
            ) {

                sb.append(
                                "    When user enters \"${"
                        )

                        .append(
                                variableKey(role)
                        )

                        .append(
                                "}\" into \""
                        )

                        .append(
                                targetLabel(role)
                        )

                        .append("\"\n");
            }

            if (
                    "CLICK".equalsIgnoreCase(action)
            ) {

                sb.append(
                                "    And user clicks "
                        )

                        .append("\"")
                        .append(
                                targetLabel(role)
                        )
                        .append("\"\n");
            }
        }

        sb.append(
                "    Then flow should complete successfully\n\n"
        );

        return sb.toString();
    }

    private String tagFor(
            DetectedFlow flow
    ) {

        String value =
                flow == null
                        ? "generated_flow"
                        : flow.getFlowType();

        String tag =
                safe(value)
                        .toLowerCase()
                        .replaceAll(
                                "[^a-z0-9]+",
                                "_"
                        )
                        .replaceAll(
                                "^_+|_+$",
                                ""
                        );

        if (
                tag.isBlank()
        ) {

            return "generated_flow";
        }

        if (
                Character.isDigit(
                        tag.charAt(0)
                )
        ) {

            return "flow_"
                    + tag;
        }

        return tag;
    }

    private String targetLabel(
            String role
    ) {

        return safe(role)
                .toLowerCase()
                .replace("_", " ")
                .trim();
    }

    // =====================================================
    // SCENARIO NAME
    // =====================================================

    private String buildScenarioName(
            DetectedFlow flow
    ) {

        String flowType =

                flow.getFlowType() == null

                        ? ""

                        : flow.getFlowType()
                        .toUpperCase();

        if (
                flowType.contains("SEARCH")
        ) {

            return "User performs search";
        }

        if (
                flowType.contains("LOGIN")
        ) {

            return "User logs into application";
        }

        if (
                flowType.contains("REGISTRATION")
                        ||
                        flowType.contains("SIGNUP")
        ) {

            return "User registers into application";
        }

        if (
                flowType.contains("FORM")
        ) {

            return "User submits form";
        }

        return "User performs generated flow";
    }

    // =====================================================
    // VARIABLE KEY
    // =====================================================

    private String variableKey(
            String role
    ) {

        if (
                role == null
                        ||
                        role.isBlank()
        ) {

            return "value";
        }

        String lower =
                role.toLowerCase();

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

        return lower.replaceAll(
                "[^a-z0-9]+",
                ""
        );
    }

    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }
}
