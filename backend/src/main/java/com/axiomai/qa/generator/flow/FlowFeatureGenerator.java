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

            feature.append(
                    generateValidationScenarios(flow)
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

        if (
                isFlowType(
                        flow,
                        "PRODUCT_SORT"
                )
        ) {

            return generateProductSortScenario(flow);
        }

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

        boolean actionStarted =
                false;

        for (FlowStep step : flow.getSteps()) {

            String action =
                    step.getAction();

            String role =
                    step.getTarget();

            String keyword =
                    actionStarted
                            ? "And"
                            : "When";

            if (
                    "TYPE".equalsIgnoreCase(action)
            ) {

                sb.append(
                                "    "
                                        + keyword
                                        + " user enters \"${"
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

                actionStarted =
                        true;
            }

            if (
                    "CLICK".equalsIgnoreCase(action)
            ) {

                sb.append(
                                "    "
                                        + keyword
                                        + " user clicks "
                        )

                        .append("\"")
                        .append(
                                targetLabel(role)
                        )
                        .append("\"\n");

                actionStarted =
                        true;
            }
        }

        appendFlowAssertion(
                sb,
                flow
        );

        return sb.toString();
    }

    private String generateProductSortScenario(
            DetectedFlow flow
    ) {

        StringBuilder sb =
                new StringBuilder();

        sb.append("  @generated @flow_")
                .append(tagFor(flow))
                .append(" @")
                .append(tagFor(flow))
                .append(" @positive\n");

        sb.append("  Scenario: User sorts product inventory\n");

        sb.append("    Given user launches \"")
                .append(flow.getPageUrl())
                .append("\"\n")
                .append("    When user enters \"az\" into \"sort\"\n")
                .append("    Then product list should be sorted by \"name ascending\"\n")
                .append("    When user enters \"za\" into \"sort\"\n")
                .append("    Then product list should be sorted by \"name descending\"\n")
                .append("    When user enters \"lohi\" into \"sort\"\n")
                .append("    Then product list should be sorted by \"price ascending\"\n")
                .append("    When user enters \"hilo\" into \"sort\"\n")
                .append("    Then product list should be sorted by \"price descending\"\n\n");

        return sb.toString();
    }

    private String generateValidationScenarios(
            DetectedFlow flow
    ) {

        if (
                flow == null
        ) {

            return "";
        }

        String flowType =
                safe(flow.getFlowType())
                        .toUpperCase();

        if (
                flowType.contains("LOGIN")
        ) {

            return generateInvalidLoginScenario(flow);
        }

        if (
                flowType.contains("FORM")
                        ||
                        flowType.contains("CHECKOUT_INFORMATION")
        ) {

            return generateRequiredFieldScenario(flow);
        }

        return "";
    }

    private String generateInvalidLoginScenario(
            DetectedFlow flow
    ) {

        StringBuilder sb =
                new StringBuilder();

        sb.append("  @generated @negative @flow_")
                .append(tagFor(flow))
                .append(" @")
                .append(tagFor(flow))
                .append("\n");

        sb.append("  Scenario: Invalid login shows validation\n")
                .append("    Given user launches \"")
                .append(flow.getPageUrl())
                .append("\"\n");

        boolean actionStarted =
                false;

        for (
                FlowStep step
                : flow.getSteps()
        ) {

            String keyword =
                    actionStarted
                            ? "And"
                            : "When";

            String target =
                    targetLabel(
                            step.getTarget()
                    );

            if (
                    "TYPE".equalsIgnoreCase(
                            step.getAction()
                    )
            ) {

                String value =
                        target.contains("password")
                                ? "wrong_password"
                                : "invalid_user";

                sb.append("    ")
                        .append(keyword)
                        .append(" user enters \"")
                        .append(value)
                        .append("\" into \"")
                        .append(target)
                        .append("\"\n");

                actionStarted =
                        true;
            }

            if (
                    "CLICK".equalsIgnoreCase(
                            step.getAction()
                    )
            ) {

                sb.append("    ")
                        .append(keyword)
                        .append(" user clicks \"")
                        .append(target)
                        .append("\"\n");

                actionStarted =
                        true;
            }
        }

        sb.append("    Then user should see \"login error\"\n\n");

        return sb.toString();
    }

    private String generateRequiredFieldScenario(
            DetectedFlow flow
    ) {

        FlowStep submitStep =
                lastClickStep(flow);

        if (
                submitStep == null
        ) {

            return "";
        }

        StringBuilder sb =
                new StringBuilder();

        sb.append("  @generated @negative @flow_")
                .append(tagFor(flow))
                .append(" @")
                .append(tagFor(flow))
                .append("\n");

        sb.append("  Scenario: Required fields are validated for ")
                .append(
                        buildScenarioName(flow)
                                .toLowerCase()
                )
                .append("\n")
                .append("    Given user launches \"")
                .append(flow.getPageUrl())
                .append("\"\n")
                .append("    When user clicks \"")
                .append(
                        targetLabel(
                                submitStep.getTarget()
                        )
                )
                .append("\"\n")
                .append("    Then user should see \"required field error\"\n\n");

        return sb.toString();
    }

    private void appendFlowAssertion(
            StringBuilder sb,
            DetectedFlow flow
    ) {

        String flowType =
                safe(flow.getFlowType())
                        .toUpperCase();

        if (
                flowType.contains("ADD_TO_CART")
        ) {

            sb.append("    Then cart badge should show \"1\"\n");

            String product =
                    productFromFlow(flow);

            if (
                    !product.isBlank()
                            &&
                            hasTarget(
                                    flow,
                                    "cart"
                            )
            ) {

                sb.append("    Then cart should contain \"")
                        .append(product)
                        .append("\"\n");
            }

            sb.append("\n");
            return;
        }

        if (
                flowType.contains("REMOVE_FROM_CART")
        ) {

            sb.append("    Then cart badge should show \"0\"\n\n");
            return;
        }

        if (
                flowType.contains("CART_NAVIGATION")
        ) {

            sb.append("    Then user should see \"Your Cart\"\n\n");
            return;
        }

        if (
                flowType.contains("CHECKOUT")
        ) {

            sb.append("    Then user should see \"Checkout\"\n\n");
            return;
        }

        sb.append(
                "    Then flow should complete successfully\n\n"
        );
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

    private boolean isFlowType(
            DetectedFlow flow,
            String expected
    ) {

        return flow != null
                &&
                safe(flow.getFlowType())
                        .equalsIgnoreCase(expected);
    }

    private FlowStep lastClickStep(
            DetectedFlow flow
    ) {

        FlowStep lastClick =
                null;

        if (
                flow == null
                        ||
                        flow.getSteps() == null
        ) {

            return null;
        }

        for (
                FlowStep step
                : flow.getSteps()
        ) {

            if (
                    "CLICK".equalsIgnoreCase(
                            step.getAction()
                    )
            ) {

                lastClick =
                        step;
            }
        }

        return lastClick;
    }

    private boolean hasTarget(
            DetectedFlow flow,
            String expected
    ) {

        if (
                flow == null
                        ||
                        flow.getSteps() == null
        ) {

            return false;
        }

        String normalizedExpected =
                safe(expected)
                        .trim()
                        .toLowerCase();

        return flow.getSteps()
                .stream()
                .anyMatch(step ->
                        safe(step.getTarget())
                                .trim()
                                .toLowerCase()
                                .equals(normalizedExpected)
                );
    }

    private String productFromFlow(
            DetectedFlow flow
    ) {

        if (
                flow == null
                        ||
                        flow.getSteps() == null
        ) {

            return "";
        }

        for (
                FlowStep step
                : flow.getSteps()
        ) {

            String target =
                    safe(step.getTarget());

            String product =
                    target.replaceAll(
                                    "(?i)\\b(add|remove|to|cart)\\b",
                                    " "
                            )
                            .trim()
                            .replaceAll("\\s+", " ");

            if (
                    !product.isBlank()
                            &&
                            !product.equalsIgnoreCase(target)
            ) {

                return product;
            }
        }

        return "";
    }

    private String targetLabel(
            String role
    ) {

        String lower =
                safe(role)
                        .toLowerCase();

        if (
                lower.contains("auth")
                        ||
                        lower.contains("identifier")
        ) {

            return "username";
        }

        if (
                lower.contains("pass")
        ) {

            return "password";
        }

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
                flowType.contains("PRODUCT_SORT")
        ) {

            return "User sorts product inventory";
        }

        if (
                flowType.contains("ADD_TO_CART")
        ) {

            return "User adds product to cart";
        }

        if (
                flowType.contains("REMOVE_FROM_CART")
        ) {

            return "User removes product from cart";
        }

        if (
                flowType.contains("CART_NAVIGATION")
        ) {

            return "User opens cart";
        }

        if (
                flowType.contains("CHECKOUT")
        ) {

            return "User completes checkout flow";
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
                lower.contains("auth")
                        ||
                        lower.contains("identifier")
                        ||
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
