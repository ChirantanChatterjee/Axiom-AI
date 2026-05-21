package com.axiomai.qa.service;

import com.axiomai.ai.service.OpenAIService;
import com.axiomai.qa.flow.DetectedFlow;
import com.axiomai.qa.models.GeneratedFramework;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequirementTestCaseGeneratorService {

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

        String feature =
                generateFeatureFile(
                        requirement,
                        featureName,
                        url,
                        sessionId
                );

        baseFramework.setFeatureFile(feature);

        return baseFramework;
    }

    private String generateFeatureFile(

            String requirement,
            String featureName,
            String url,
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
                        sessionId
                );

        if (
                isUsableFeature(aiGenerated)
        ) {

            return normalizeGeneratedFeature(aiGenerated);
        }

        return fallbackFeature(
                requirement,
                featureName,
                launchUrl
        );
    }

    private String generateWithAi(

            String requirement,
            String featureName,
            String url,
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
                  Then user should see "<expected text>"
                  Then flow should complete successfully
                - Use runtime placeholders for test data when appropriate: ${username}, ${password}, ${email}, ${search}, ${product}, ${quantity}.
                - For payment forms, use runtime placeholders when appropriate: ${payee}, ${address}, ${city}, ${state}, ${zip}, ${phone}, ${account}, ${amount}.
                - Keep targets semantic and short, for example "username", "password", "login button", "search", "add to cart".
                - If a requested feature is behind login, include login steps before the feature steps.
                - For ParaBank bill pay, launch the ParaBank home page, log in, click "Bill Pay", fill "verify account" with ${account}, and click "send payment button".
                - For ParaBank successful bill pay, assert an app-observable payment success state instead of relying only on a brittle exact heading.
                - Generate meaningful positive, negative, required-field, and boundary scenarios when the requirement implies them.
                - For negative validation scenarios, do not guess brittle exact validation copy. Assert an observable validation outcome such as "amount validation error", "required field error", or another exact message only when the page is known to show it.
                - Today's date is %s. Convert partial dates into explicit dates and never output YYYY placeholders.
                - The launch URL is: %s
                - The requested feature name is: %s
                - Session learning from user uploads and runtime repair outcomes:
                %s

                Requirement:
                %s
                """.formatted(
                        LocalDate.now(),
                        safe(url),
                        safe(featureName),
                        safe(learningSummary).isBlank()
                                ? "No user-uploaded framework modifications have been observed yet."
                                : safe(learningSummary),
                        safe(requirement)
                );

        String response =
                openAIService.ask(prompt);

        return extractFeature(response);
    }

    private String normalizeLaunchUrlForFeature(
            String requirement,
            String featureName,
            String url
    ) {

        String safeUrl =
                safe(url);

        String requestedFeature =
                (safe(requirement) + " " + safe(featureName))
                        .toLowerCase();

        if (
                safeUrl.toLowerCase()
                        .contains("parabank")
                        &&
                        requestedFeature.contains("bill")
                        &&
                        requestedFeature.contains("pay")
        ) {

            return "https://parabank.parasoft.com/parabank/index.htm";
        }

        return url;
    }

    private String normalizeGeneratedFeature(
            String feature
    ) {

        if (
                feature == null
        ) {

            return "";
        }

        return feature.replaceAll(
                "(?i)\\bYYYY\\b",
                String.valueOf(
                        Year.now()
                                .getValue()
                )
        );
    }

    private String fallbackFeature(

            String requirement,
            String featureName,
            String url

    ) {

        String title =
                title(featureName, requirement);

        String tag =
                tag(title);

        String lower =
                safe(requirement)
                        .toLowerCase();

        StringBuilder feature =
                new StringBuilder();

        feature.append("Feature: ")
                .append(title)
                .append("\n\n");

        feature.append("  @generated @ai_requirement @")
                .append(tag)
                .append("\n");

        if (
                lower.contains("cart")
                        ||
                        lower.contains("product")
                        ||
                        lower.contains("shopping")
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
                lower.contains("search")
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
                lower.contains("login")
                        ||
                        lower.contains("sign in")
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
            String feature
    ) {

        return feature != null
                &&
                feature.contains("Feature:")
                &&
                feature.contains("Scenario:")
                &&
                feature.contains("@generated");
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

    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }
}
