package com.axiomai.qa.service;

import com.axiomai.ai.service.OpenAIService;
import com.axiomai.qa.flow.DetectedFlow;
import com.axiomai.qa.models.FlowStep;
import com.axiomai.qa.models.GeneratedFramework;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RequirementTestCaseGeneratorService {

    private final RequirementDocumentAnalyzer
            requirementDocumentAnalyzer =
            new RequirementDocumentAnalyzer();

    private final OpenAIService openAIService;

    private final FrameworkGeneratorService frameworkGeneratorService;

    private final FrameworkLearningService frameworkLearningService;

    public GeneratedFramework generate(

            String requirement,
            String featureName,
            String url,
            List<DetectedFlow> flows,
            String sessionId

    ) {

        GeneratedFramework baseFramework =
                frameworkGeneratorService.generate(flows);

        String launchUrl =
                normalizeLaunchUrlForFeature(
                        requirement,
                        featureName,
                        url
                );

        RequirementDocumentAnalyzer.Analysis analysis =
                requirementDocumentAnalyzer
                        .analyze(
                                requirement,
                                featureName,
                                launchUrl
                        );

        if (
                analysis.hasTestCases()
        ) {

            baseFramework.setFeatureFile(
                    analysis.featureFile()
            );

            baseFramework.setTestCases(
                    analysis.testCases()
            );

            return baseFramework;
        }

        String feature =
                generateFeatureFile(
                        requirement,
                        featureName,
                        launchUrl,
                        flows,
                        sessionId
                );

        baseFramework.setFeatureFile(feature);

        return baseFramework;
    }

    private String generateFeatureFile(

            String requirement,
            String featureName,
            String url,
            List<DetectedFlow> flows,
            String sessionId

    ) {

        String launchUrl =
                normalizeLaunchUrlForFeature(
                        requirement,
                        featureName,
                        url
                );

        String aiGenerated =
                generateWithAi(
                        requirement,
                        featureName,
                        launchUrl,
                        flows,
                        sessionId
                );

        String normalizedAi =
                normalizeGeneratedFeature(
                        aiGenerated,
                        requirement,
                        featureName
                );

        if (
                isUsableFeature(
                        normalizedAi,
                        requirement,
                        featureName
                )
        ) {

            return normalizedAi;
        }

        return fallbackFeature(
                requirement,
                featureName,
                launchUrl,
                flows
        );
    }

    private String generateWithAi(

            String requirement,
            String featureName,
            String url,
            List<DetectedFlow> flows,
            String sessionId

    ) {

        String learningSummary =
                frameworkLearningService.learningSummary(sessionId);

        String prompt =
                """
                You are an active QA requirement analysis agent.

                Analyse the user's plain-English requirement and produce executable Cucumber Gherkin test cases.

                Rules:
                - Return ONLY Gherkin text. Do not wrap it in Markdown.
                - Use the exact Feature/Scenario syntax.
                - Assign tags above every scenario.
                - Every scenario must include @generated, @ai_requirement, and one stable feature tag.
                - Use only this step vocabulary:
                  Given user launches "<url>"
                  When user enters "<value>" into "<target>"
                  And user enters "<value>" into "<target>"
                  When user clicks "<target>"
                  And user clicks "<target>"
                  When user refreshes page
                  Then user should see "<expected text>"
                  Then product list should be sorted by "<name ascending|name descending|price ascending|price descending>"
                  Then cart badge should show "<count>"
                  Then cart should contain "<product>"
                  Then cart should not contain "<product>"
                  Then checkout total should equal item total plus tax
                  Then flow should complete successfully
                - Use runtime placeholders for test data when appropriate: ${username}, ${password}, ${email}, ${search}, ${product}, ${quantity}, ${from}, ${to}, ${firstName}, ${lastName}.
                - For payment forms, use runtime placeholders when appropriate: ${payee}, ${address}, ${city}, ${state}, ${zip}, ${phone}, ${account}, ${amount}.
                - For travel flight selection, do not use a generic ${search} placeholder. Use explicit ${from} and ${to} placeholders and tag scenarios with @select_flight.
                - For Agile Travel return journeys, after the flight-search "continue" action the passenger details page shows First Name, Last Name, and a Next button. Enter ${firstName} and ${lastName}, then click "next".
                - Prefer crawler-observed controls and exact visible action labels. Do not invent buttons such as "search flights" unless the crawler observed that label.
                - If the crawler shows a submit action such as "Continue", click "continue" rather than a guessed domain phrase.
                - For dropdown/select controls, do not click option labels. Use `When user enters "<option value>" into "<dropdown target>"`.
                - For SauceDemo product sorting, use `When user enters "az" into "sort"` for Name A to Z, "za" for Name Z to A, "lohi" for Price low to high, and "hilo" for Price high to low.
                - Keep targets semantic and short, for example "username", "password", "login button", "search", "add to cart".
                - If a requested feature is behind login, include login steps before the feature steps.
                - For ParaBank bill pay, launch the ParaBank home page, log in, click "Bill Pay", fill "verify account" with ${account}, and click "send payment button".
                - For ParaBank successful bill pay, assert an app-observable payment success state instead of relying only on a brittle exact heading.
                - Treat "bill pay", "billpay", "bill payment", and "pay bill" as the same Bill Pay feature and tag it @bill_pay.
                - Generate meaningful positive, negative, required-field, and boundary scenarios when the requirement implies them.
                - When the user asks for more tests, additional tests, edge cases, negative tests, validation tests, or boundary tests, do not return only a happy path. Return a scenario set covering those categories.
                - For negative validation scenarios, do not guess brittle exact validation copy. Assert an observable validation outcome such as "amount validation error", "required field error", or another exact message only when the page is known to show it.
                - Today's date is %s. Convert partial dates into explicit dates and never output YYYY placeholders.
                - The launch URL is: %s
                - The requested feature name is: %s
                - Crawler-observed flows and controls:
                %s
                - Session learning from user uploads and runtime repair outcomes:
                %s

                Requirement:
                %s
                """.formatted(
                        LocalDate.now(),
                        safe(url),
                        safe(featureName),
                        crawlerContext(flows),
                        safe(learningSummary).isBlank()
                                ? "No user-uploaded framework modifications have been observed yet."
                                : safe(learningSummary),
                        safe(requirement)
                );

        String response =
                openAIService.ask(prompt);

        return extractFeature(response);
    }

    private String crawlerContext(
            List<DetectedFlow> flows
    ) {

        if (
                flows == null
                        ||
                        flows.isEmpty()
        ) {

            return "No crawler-observed flows were available.";
        }

        StringBuilder context =
                new StringBuilder();

        int count =
                0;

        for (DetectedFlow flow : flows) {

            context.append("- Flow ")
                    .append(safe(flow.getFlowType()))
                    .append(" at ")
                    .append(safe(flow.getPageUrl()))
                    .append("\n");

            for (FlowStep step : safeSteps(flow)) {

                context.append("  - ")
                        .append(safe(step.getAction()))
                        .append(" target=")
                        .append(safe(step.getTarget()))
                        .append(", role=")
                        .append(safe(step.getBusinessRole()))
                        .append(", selector=")
                        .append(safe(step.getSelector()))
                        .append("\n");

                count++;

                if (
                        count >= 40
                ) {

                    context.append("  - ... additional crawler controls omitted\n");
                    return context.toString();
                }
            }
        }

        return context.toString();
    }

    private String normalizeLaunchUrlForFeature(
            String requirement,
            String featureName,
            String url
    ) {

        String requestedFeature =
                (safe(requirement) + " " + safe(featureName))
                        .toLowerCase();

        if (
                isBillPayRequest(requestedFeature)
                        &&
                        isBlank(url)
        ) {

            return "https://parabank.parasoft.com/parabank/index.htm";
        }

        return url;
    }

    private boolean isBlank(
            String value
    ) {

        return value == null
                ||
                value.isBlank();
    }

    private String normalizeGeneratedFeature(
            String feature,
            String requirement,
            String featureName
    ) {

        if (
                feature == null
        ) {

            return "";
        }

        String normalized =
                feature.replaceAll(
                "(?i)\\bYYYY\\b",
                String.valueOf(
                        Year.now()
                                .getValue()
                )
        );

        return ensureScenarioTags(
                normalized,
                requirement,
                featureName
        );
    }

    private String ensureScenarioTags(
            String feature,
            String requirement,
            String featureName
    ) {

        if (
                feature == null
                        ||
                        feature.isBlank()
        ) {

            return "";
        }

        String requestedTag =
                stableFeatureTag(
                        requirement,
                        featureName
                );

        Set<String> requiredTags =
                new LinkedHashSet<>();

        requiredTags.add("@generated");
        requiredTags.add("@ai_requirement");
        requiredTags.add("@"
                + requestedTag);

        String[] lines =
                feature.split(
                        "\\R",
                        -1
                );

        List<String> output =
                new ArrayList<>();

        List<String> pendingTagLines =
                new ArrayList<>();

        for (String line : lines) {

            String trimmed =
                    line.trim();

            if (
                    trimmed.startsWith("@")
            ) {

                pendingTagLines.add(line);
                continue;
            }

            if (
                    isScenarioHeader(trimmed)
            ) {

                output.add(
                        scenarioIndent(line)
                                + mergedTagLine(
                                        pendingTagLines,
                                        requiredTags
                                )
                );
                pendingTagLines.clear();
                output.add(line);
                continue;
            }

            if (
                    !pendingTagLines.isEmpty()
            ) {

                output.addAll(pendingTagLines);
                pendingTagLines.clear();
            }

            output.add(line);
        }

        if (
                !pendingTagLines.isEmpty()
        ) {

            output.addAll(pendingTagLines);
        }

        return String.join(
                "\n",
                output
        )
                .replaceAll("\\s*$", "")
                + "\n";
    }

    private boolean isScenarioHeader(
            String trimmedLine
    ) {

        return trimmedLine.startsWith("Scenario:")
                ||
                trimmedLine.startsWith("Scenario Outline:");
    }

    private String scenarioIndent(
            String scenarioLine
    ) {

        int index =
                0;

        while (
                index < scenarioLine.length()
                        &&
                        Character.isWhitespace(
                                scenarioLine.charAt(index)
                        )
        ) {

            index++;
        }

        return index == 0
                ? "  "
                : scenarioLine.substring(
                        0,
                        index
                );
    }

    private String mergedTagLine(
            List<String> pendingTagLines,
            Set<String> requiredTags
    ) {

        Set<String> tags =
                new LinkedHashSet<>(requiredTags);

        for (String tagLine : pendingTagLines) {

            for (
                    String tag
                    : tagLine.trim()
                            .split("\\s+")
            ) {

                if (
                        tag.startsWith("@")
                ) {

                    tags.add(tag);
                }
            }
        }

        return String.join(
                " ",
                tags
        );
    }

    String fallbackFeature(

            String requirement,
            String featureName,
            String url,
            List<DetectedFlow> flows

    ) {

        String title =
                title(featureName, requirement);

        String tag =
                tag(title);

        String requestedText =
                (
                        safe(requirement)
                                + " "
                                + safe(featureName)
                )
                        .toLowerCase();

        StringBuilder feature =
                new StringBuilder();

        feature.append("Feature: ")
                .append(title)
                .append("\n\n");

        if (
                isBillPayRequest(requestedText)
        ) {

            return billPayFallbackFeature(
                    feature,
                    url
            );
        }

        if (
                isTravelFlightRequest(requestedText)
        ) {

            return travelFlightFallbackFeature(
                    feature,
                    url,
                    flows
            );
        }

        feature.append("  @generated @ai_requirement @")
                .append(tag)
                .append("\n");

        if (
                requestedText.contains("cart")
                        ||
                        requestedText.contains("product")
                        ||
                        requestedText.contains("shopping")
        ) {

            feature.append("  Scenario: Add a product to the cart\n")
                    .append("    Given user launches \"")
                    .append(safe(url))
                    .append("\"\n")
                    .append("    When user enters \"${username}\" into \"username\"\n")
                    .append("    And user enters \"${password}\" into \"password\"\n")
                    .append("    And user clicks \"login button\"\n")
                    .append("    And user clicks \"${product}\"\n")
                    .append("    And user clicks \"add to cart\"\n")
                    .append("    Then user should see \"cart\"\n\n");

            return feature.toString();
        }

        if (
                requestedText.contains("search")
        ) {

            feature.append("  Scenario: Search for content\n")
                    .append("    Given user launches \"")
                    .append(safe(url))
                    .append("\"\n")
                    .append("    When user enters \"${search}\" into \"search\"\n")
                    .append("    And user clicks \"search button\"\n")
                    .append("    Then flow should complete successfully\n\n");

            return feature.toString();
        }

        if (
                requestedText.contains("login")
                        ||
                        requestedText.contains("sign in")
        ) {

            feature.append("  Scenario: Login with valid credentials\n")
                    .append("    Given user launches \"")
                    .append(safe(url))
                    .append("\"\n")
                    .append("    When user enters \"${username}\" into \"username\"\n")
                    .append("    And user enters \"${password}\" into \"password\"\n")
                    .append("    And user clicks \"login button\"\n")
                    .append("    Then flow should complete successfully\n\n");

            return feature.toString();
        }

        feature.append("  Scenario: Validate requested application behavior\n")
                .append("    Given user launches \"")
                .append(safe(url))
                .append("\"\n")
                .append("    Then flow should complete successfully\n\n");

        return feature.toString();
    }

    private String billPayFallbackFeature(

            StringBuilder feature,
            String url

    ) {

        feature.append("  @generated @ai_requirement @bill_pay @positive\n")
                .append("  Scenario: Successful bill payment with valid information\n");

        appendBillPayStart(
                feature,
                url
        );

        appendBillPayValidDetails(feature);

        feature.append("    And user enters \"${amount}\" into \"amount\"\n")
                .append("    And user clicks \"send payment button\"\n")
                .append("    Then user should see \"Bill Payment Complete\"\n\n");

        feature.append("  @generated @ai_requirement @bill_pay @required_field @negative\n")
                .append("  Scenario: Bill payment requires payee name\n");

        appendBillPayStart(
                feature,
                url
        );

        feature.append("    And user enters \"${address}\" into \"address\"\n")
                .append("    And user enters \"${city}\" into \"city\"\n")
                .append("    And user enters \"${state}\" into \"state\"\n")
                .append("    And user enters \"${zip}\" into \"zip\"\n")
                .append("    And user enters \"${phone}\" into \"phone\"\n")
                .append("    And user enters \"${account}\" into \"account\"\n")
                .append("    And user enters \"${account}\" into \"verify account\"\n")
                .append("    And user enters \"${amount}\" into \"amount\"\n")
                .append("    And user clicks \"send payment button\"\n")
                .append("    Then user should see \"required field error\"\n\n");

        feature.append("  @generated @ai_requirement @bill_pay @required_field @negative\n")
                .append("  Scenario: Bill payment requires amount\n");

        appendBillPayStart(
                feature,
                url
        );

        appendBillPayValidDetails(feature);

        feature.append("    And user clicks \"send payment button\"\n")
                .append("    Then user should see \"required field error\"\n\n");

        feature.append("  @generated @ai_requirement @bill_pay @validation @negative\n")
                .append("  Scenario: Bill payment rejects invalid amount\n");

        appendBillPayStart(
                feature,
                url
        );

        appendBillPayValidDetails(feature);

        feature.append("    And user enters \"abc\" into \"amount\"\n")
                .append("    And user clicks \"send payment button\"\n")
                .append("    Then user should see \"amount validation error\"\n\n");

        feature.append("  @generated @ai_requirement @bill_pay @validation @negative\n")
                .append("  Scenario: Bill payment detects account confirmation mismatch\n");

        appendBillPayStart(
                feature,
                url
        );

        feature.append("    And user enters \"${payee}\" into \"payee name\"\n")
                .append("    And user enters \"${address}\" into \"address\"\n")
                .append("    And user enters \"${city}\" into \"city\"\n")
                .append("    And user enters \"${state}\" into \"state\"\n")
                .append("    And user enters \"${zip}\" into \"zip\"\n")
                .append("    And user enters \"${phone}\" into \"phone\"\n")
                .append("    And user enters \"${account}\" into \"account\"\n")
                .append("    And user enters \"99999\" into \"verify account\"\n")
                .append("    And user enters \"${amount}\" into \"amount\"\n")
                .append("    And user clicks \"send payment button\"\n")
                .append("    Then user should see \"invalid account\"\n\n");

        feature.append("  @generated @ai_requirement @bill_pay @boundary\n")
                .append("  Scenario: Bill payment handles a high amount boundary\n");

        appendBillPayStart(
                feature,
                url
        );

        appendBillPayValidDetails(feature);

        feature.append("    And user enters \"999999.99\" into \"amount\"\n")
                .append("    And user clicks \"send payment button\"\n")
                .append("    Then user should see \"Bill Payment Complete\"\n\n");

        return feature.toString();
    }

    private String travelFlightFallbackFeature(

            StringBuilder feature,
            String url,
            List<DetectedFlow> flows

    ) {

        String flightSubmitAction =
                travelFlightSubmitActionTarget(flows);

        feature.append("  @generated @ai_requirement @select_flight @return_journey @positive\n")
                .append("  Scenario: User successfully selects a return flight journey\n");

        appendTravelFlightStart(
                feature,
                url,
                flightSubmitAction
        );

        feature.append("    Then user should see \"First Name\"\n")
                .append("    And user should see \"Last Name\"\n")
                .append("    And user enters \"${firstName}\" into \"First Name\"\n")
                .append("    And user enters \"${lastName}\" into \"Last Name\"\n")
                .append("    And user clicks \"next\"\n")
                .append("    Then flow should complete successfully\n\n");

        feature.append("  @generated @ai_requirement @select_flight @return_journey @negative @required_field\n")
                .append("  Scenario: User cannot continue without selecting return flight\n");

        appendTravelFlightStart(
                feature,
                url,
                flightSubmitAction
        );

        feature.append("    Then user should see \"First Name\"\n")
                .append("    And user should see \"Last Name\"\n")
                .append("    And user clicks \"next\"\n")
                .append("    Then user should see \"required field error\"\n\n");

        feature.append("  @generated @ai_requirement @select_flight @return_journey @negative @validation\n")
                .append("  Scenario: User cannot select the same city for origin and destination\n")
                .append("    Given user launches \"")
                .append(safe(url))
                .append("\"\n")
                .append("    When user enters \"${username}\" into \"username\"\n")
                .append("    And user enters \"${password}\" into \"password\"\n")
                .append("    And user clicks \"login button\"\n")
                .append("    And user clicks \"return journey\"\n")
                .append("    And user enters \"New York\" into \"from\"\n")
                .append("    And user enters \"New York\" into \"to\"\n")
                .append("    And user clicks \"")
                .append(safe(flightSubmitAction))
                .append("\"\n")
                .append("    Then user should see \"route validation error\"\n\n");

        return feature.toString();
    }

    private void appendTravelFlightStart(

            StringBuilder feature,
            String url,
            String flightSubmitAction

    ) {

        feature.append("    Given user launches \"")
                .append(safe(url))
                .append("\"\n")
                .append("    When user enters \"${username}\" into \"username\"\n")
                .append("    And user enters \"${password}\" into \"password\"\n")
                .append("    And user clicks \"login button\"\n")
                .append("    And user clicks \"return journey\"\n")
                .append("    And user enters \"${from}\" into \"from\"\n")
                .append("    And user enters \"${to}\" into \"to\"\n")
                .append("    And user clicks \"")
                .append(safe(flightSubmitAction))
                .append("\"\n");
    }

    private String travelFlightSubmitActionTarget(
            List<DetectedFlow> flows
    ) {

        String observed =
                firstObservedActionTarget(
                        flows,
                        true
                );

        if (
                !isBlank(observed)
        ) {

            return observed;
        }

        observed =
                firstObservedActionTarget(
                        flows,
                        false
                );

        if (
                !isBlank(observed)
        ) {

            return observed;
        }

        return "continue";
    }

    private String firstObservedActionTarget(
            List<DetectedFlow> flows,
            boolean featureFlowsOnly
    ) {

        if (
                flows == null
        ) {

            return "";
        }

        for (DetectedFlow flow : flows) {

            String flowType =
                    safe(
                            flow.getFlowType()
                    )
                            .toUpperCase();

            if (
                    featureFlowsOnly
                            &&
                            !flowType.equals("FORM_SUBMISSION")
                            &&
                            !flowType.equals("SEARCH")
            ) {

                continue;
            }

            if (
                    flowType.equals("LOGIN")
            ) {

                continue;
            }

            for (FlowStep step : safeSteps(flow)) {

                if (
                        !"CLICK".equalsIgnoreCase(
                                safe(step.getAction())
                        )
                ) {

                    continue;
                }

                String target =
                        observedActionTarget(step);

                if (
                        !isBlank(target)
                ) {

                    return target;
                }
            }
        }

        return "";
    }

    private List<FlowStep> safeSteps(
            DetectedFlow flow
    ) {

        if (
                flow == null
                        ||
                        flow.getSteps() == null
        ) {

            return List.of();
        }

        return flow.getSteps();
    }

    private String observedActionTarget(
            FlowStep step
    ) {

        String role =
                safe(step.getBusinessRole())
                        .toUpperCase();

        String target =
                normalizeActionTarget(
                        step.getTarget()
                );

        if (
                !isGenericActionTarget(target)
        ) {

            return target;
        }

        String selectorLabel =
                actionLabelFromSelectors(step);

        if (
                !isBlank(selectorLabel)
        ) {

            return selectorLabel;
        }

        if (
                role.equals("NEXT_BUTTON")
        ) {

            return "continue";
        }

        if (
                role.equals("SEARCH_BUTTON")
        ) {

            return "search button";
        }

        return "";
    }

    private String actionLabelFromSelectors(
            FlowStep step
    ) {

        String label =
                actionLabelFromSelector(
                        step.getSelector()
                );

        if (
                !isBlank(label)
        ) {

            return label;
        }

        if (
                step.getFallbackSelectors() == null
        ) {

            return "";
        }

        for (String selector : step.getFallbackSelectors()) {

            label =
                    actionLabelFromSelector(selector);

            if (
                    !isBlank(label)
            ) {

                return label;
            }
        }

        return "";
    }

    private String actionLabelFromSelector(
            String selector
    ) {

        String valueLabel =
                normalizeActionTarget(
                        quotedSelectorValue(
                                selector,
                                "value="
                        )
                );

        if (
                !isGenericActionTarget(valueLabel)
        ) {

            return valueLabel;
        }

        String textLabel =
                normalizeActionTarget(
                        quotedSelectorValue(
                                selector,
                                "has-text("
                        )
                );

        if (
                !isGenericActionTarget(textLabel)
        ) {

            return textLabel;
        }

        String lower =
                safe(selector)
                        .toLowerCase();

        if (
                lower.contains("continue")
                        ||
                        lower.contains("next")
        ) {

            return "continue";
        }

        if (
                lower.contains("search")
        ) {

            return "search button";
        }

        return "";
    }

    private String quotedSelectorValue(
            String selector,
            String marker
    ) {

        String source =
                safe(selector);

        int markerIndex =
                source.indexOf(marker);

        if (
                markerIndex < 0
        ) {

            return "";
        }

        int start =
                markerIndex + marker.length();

        while (
                start < source.length()
                        &&
                        Character.isWhitespace(
                                source.charAt(start)
                        )
        ) {

            start++;
        }

        if (
                start >= source.length()
        ) {

            return "";
        }

        char quote =
                source.charAt(start);

        if (
                quote != '\''
                        &&
                        quote != '"'
        ) {

            return "";
        }

        int end =
                source.indexOf(
                        quote,
                        start + 1
                );

        if (
                end < 0
        ) {

            return "";
        }

        return source.substring(
                start + 1,
                end
        );
    }

    private String normalizeActionTarget(
            String target
    ) {

        return safe(target)
                .replace("${", "")
                .replace("}", "")
                .replace("_", " ")
                .replace("-", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    private boolean isGenericActionTarget(
            String target
    ) {

        String normalized =
                safe(target)
                        .replaceAll("[^a-z0-9]+", "_")
                        .replaceAll("^_+|_+$", "");

        return normalized.isBlank()
                ||
                normalized.equals("submit_button")
                ||
                normalized.equals("primary_action_button")
                ||
                normalized.equals("button")
                ||
                normalized.equals("login_button")
                ||
                normalized.equals("search_button");
    }

    private void appendBillPayStart(

            StringBuilder feature,
            String url

    ) {

        feature.append("    Given user launches \"")
                .append(safe(url))
                .append("\"\n")
                .append("    When user enters \"${username}\" into \"username\"\n")
                .append("    And user enters \"${password}\" into \"password\"\n")
                .append("    And user clicks \"login button\"\n")
                .append("    And user clicks \"Bill Pay\"\n");
    }

    private void appendBillPayValidDetails(
            StringBuilder feature
    ) {

        feature.append("    And user enters \"${payee}\" into \"payee name\"\n")
                .append("    And user enters \"${address}\" into \"address\"\n")
                .append("    And user enters \"${city}\" into \"city\"\n")
                .append("    And user enters \"${state}\" into \"state\"\n")
                .append("    And user enters \"${zip}\" into \"zip\"\n")
                .append("    And user enters \"${phone}\" into \"phone\"\n")
                .append("    And user enters \"${account}\" into \"account\"\n")
                .append("    And user enters \"${account}\" into \"verify account\"\n");
    }

    private String extractFeature(
            String response
    ) {

        if (
                response == null
        ) {

            return "";
        }

        String cleaned =
                response.trim();

        if (
                cleaned.startsWith("```")
        ) {

            cleaned =
                    cleaned.replaceFirst(
                            "^```(?:gherkin|cucumber)?\\s*",
                            ""
                    );

            cleaned =
                    cleaned.replaceFirst(
                            "\\s*```$",
                            ""
                    )
                            .trim();
        }

        int index =
                cleaned.indexOf("Feature:");

        if (
                index >= 0
        ) {

            return cleaned.substring(index)
                    .trim()
                    + "\n";
        }

        return cleaned;
    }

    private boolean isUsableFeature(
            String feature,
            String requirement,
            String featureName
    ) {

        boolean basicFeature =
                feature != null
                &&
                feature.contains("Feature:")
                &&
                feature.contains("Scenario:")
                &&
                feature.contains("@generated")
                &&
                feature.contains("@ai_requirement")
                &&
                feature.toLowerCase()
                        .contains(
                                "@"
                                        + stableFeatureTag(
                                                requirement,
                                                featureName
                                        )
                        );

        if (
                !basicFeature
        ) {

            return false;
        }

        String requestedText =
                safe(requirement)
                        + " "
                        + safe(featureName);

        if (
                isBillPayRequest(
                        requestedText
                )
        ) {

            if (
                    !isUsableBillPayFeature(feature)
            ) {

                return false;
            }

            if (
                    isExpandedCoverageRequest(requirement)
            ) {

                return scenarioCount(feature) >= 2
                        &&
                        hasExpandedCoverage(feature);
            }

            return true;
        }

        if (
                isTravelFlightRequest(requestedText)
        ) {

            return isUsableTravelFlightFeature(feature);
        }

        if (
                !isExpandedCoverageRequest(requirement)
        ) {

            return true;
        }

        if (
                scenarioCount(feature) < 2
        ) {

            return false;
        }

        return true;
    }

    private boolean isUsableBillPayFeature(
            String feature
    ) {

        String lower =
                safe(feature)
                        .toLowerCase();

        return lower.contains("parabank.parasoft.com")
                &&
                (
                        lower.contains("@bill_pay")
                                ||
                                lower.contains("@billpay")
                )
                &&
                lower.contains("verify account")
                &&
                lower.contains("send payment");
    }

    private boolean isUsableTravelFlightFeature(
            String feature
    ) {

        String lower =
                safe(feature)
                        .toLowerCase();

        return lower.contains("@select_flight")
                &&
                lower.contains("${from}")
                &&
                lower.contains("${to}")
                &&
                !lower.contains("${search}");
    }

    private boolean isExpandedCoverageRequest(
            String requirement
    ) {

        String lower =
                safe(requirement)
                        .toLowerCase();

        return lower.contains("more")
                ||
                lower.contains("additional")
                ||
                lower.contains("edge case")
                ||
                lower.contains("edge-case")
                ||
                lower.contains("negative")
                ||
                lower.contains("boundary")
                ||
                lower.contains("required field")
                ||
                lower.contains("validation");
    }

    private int scenarioCount(
            String feature
    ) {

        if (
                feature == null
        ) {

            return 0;
        }

        int count =
                0;

        for (
                String line
                : feature.split("\\R")
        ) {

            if (
                    line.trim()
                            .startsWith("Scenario:")
                            ||
                            line.trim()
                                    .startsWith("Scenario Outline:")
            ) {

                count++;
            }
        }

        return count;
    }

    private boolean hasExpandedCoverage(
            String feature
    ) {

        String lower =
                safe(feature)
                        .toLowerCase();

        return (
                lower.contains("@negative")
                        ||
                        lower.contains("@validation")
                        ||
                        lower.contains("@required_field")
        )
                &&
                lower.contains("@boundary");
    }

    private boolean isBillPayRequest(
            String value
    ) {

        String lower =
                safe(value)
                        .toLowerCase();

        return lower.contains("bill pay")
                ||
                lower.contains("billpay")
                ||
                lower.contains("bill payment")
                ||
                lower.contains("pay bill");
    }

    private boolean isTravelFlightRequest(
            String value
    ) {

        String lower =
                safe(value)
                        .toLowerCase();

        return lower.contains("flight")
                &&
                (
                        lower.contains("select")
                                ||
                                lower.contains("return")
                                ||
                                lower.contains("journey")
                                ||
                                lower.contains("booking")
                                ||
                                lower.contains("travel")
                );
    }

    private String title(

            String featureName,
            String requirement

    ) {

        if (
                featureName != null
                        &&
                        !featureName.isBlank()
        ) {

            return toTitle(featureName);
        }

        String value =
                safe(requirement)
                        .replaceAll(
                                "\\s+",
                                " "
                        )
                        .trim();

        if (
                value.length() > 70
        ) {

            value =
                    value.substring(0, 70)
                            .trim();
        }

        return value.isBlank()
                ? "AI Generated Requirement Tests"
                : toTitle(value);
    }

    private String toTitle(
            String value
    ) {

        String[] parts =
                safe(value)
                        .replaceAll(
                                "[^A-Za-z0-9]+",
                                " "
                        )
                        .trim()
                        .split("\\s+");

        StringBuilder title =
                new StringBuilder();

        for (
                String part
                : parts
        ) {

            if (
                    part.isBlank()
            ) {

                continue;
            }

            if (
                    !title.isEmpty()
            ) {

                title.append(" ");
            }

            title.append(
                    part.substring(0, 1)
                            .toUpperCase()
            );

            if (
                    part.length() > 1
            ) {

                title.append(
                        part.substring(1)
                                .toLowerCase()
                );
            }
        }

        return title.isEmpty()
                ? "AI Generated Requirement Tests"
                : title.toString();
    }

    private String tag(
            String value
    ) {

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

            return "ai_generated";
        }

        if (
                Character.isDigit(
                        tag.charAt(0)
                )
        ) {

            return "test_"
                    + tag;
        }

        return tag;
    }

    private String stableFeatureTag(
            String requirement,
            String featureName
    ) {

        String requestedText =
                safe(requirement)
                        + " "
                        + safe(featureName);

        if (
                isBillPayRequest(requestedText)
        ) {

            return "bill_pay";
        }

        if (
                isTravelFlightRequest(requestedText)
        ) {

            return "select_flight";
        }

        return tag(
                title(
                        featureName,
                        requirement
                )
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
