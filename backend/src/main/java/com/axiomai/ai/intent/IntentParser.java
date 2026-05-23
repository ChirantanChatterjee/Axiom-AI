package com.axiomai.ai.intent;

import com.axiomai.ai.dto.AICommand;
import com.axiomai.ai.execution.AIExecutionPlan;
import com.axiomai.ai.model.GPTIntentResponse;
import com.axiomai.ai.planner.ScenarioPlanner;
import com.axiomai.ai.service.OpenAIIntentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        Map<String, String> variables =
                extractVariables(message);

        if (
                containsRequirementDocument(message)
        ) {

            return AICommand.builder()
                    .intent("GENERATE_FEATURE")
                    .flowName("requirements")
                    .featureName("requirements")
                    .url(
                            extractUrl(message)
                    )
                    .variables(variables)
                    .message(message)
                    .build();
        }

        if (
                containsGeneratedTestTagRequest(message)
        ) {

            return AICommand.builder()

                    .intent("SHOW_GENERATED_TEST_TAGS")

                    .message(message)

                    .build();
        }

        if (
                containsGeneratedTestRepairRequest(message)
        ) {

            return AICommand.builder()

                    .intent("REPAIR_GENERATED_TESTS")

                    .message(message)

                    .build();
        }

        AICommand compoundCommand =
                parseCompoundCommand(
                        message,
                        variables
                );

        if (
                compoundCommand != null
        ) {

            return compoundCommand;
        }

        if (
                containsGeneratedTestExecutionRequest(message)
        ) {

            return AICommand.builder()

                    .intent("EXECUTE_GENERATED_TESTS")

                    .target(
                            extractTagExpression(message)
                    )

                    .variables(variables)

                    .message(message)

                    .build();
        }

        GPTIntentResponse response =

                openAIIntentService
                        .interpret(message);

        System.out.println(
                "GPT INTENT = "
                        + response.getIntent()
        );

        // =====================================================
        // FEATURE GENERATION MUST NOT FALL INTO AI_EXECUTION
        // =====================================================

        if (
                containsFeatureGenerationIntent(message)
        ) {

            return AICommand.builder()

                    .intent("GENERATE_FEATURE")

                    .flowName(
                            firstNonBlank(
                                    response.getFlowName(),
                                    extractFeatureName(message)
                            )
                    )

                    .featureName(
                            firstNonBlank(
                                    response.getFeatureName(),
                                    extractFeatureName(message)
                            )
                    )

                    .url(
                            extractUrl(message)
                    )

                    .variables(
                            mergeVariables(
                                    response.getVariables(),
                                    variables
                            )
                    )

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

                    .featureName(
                            response.getFeatureName()
                    )

                    .artifactName(
                            response.getArtifactName()
                    )

                    .variables(
                            mergeVariables(
                                    response.getVariables(),
                                    variables
                            )
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

        return !containsFrameworkGenerationIntent(message)
                &&
                !containsFeatureGenerationIntent(message)
                &&
                !lower.contains("execute")
                &&
                !lower.contains("run")
                &&
                !lower.contains("start")
                &&
                (

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
    // FEATURE GENERATION DETECTION
    // =====================================================

    private boolean containsFeatureGenerationIntent(
            String message
    ) {

        String lower =
                message.toLowerCase();

        if (
                containsRequirementDocument(message)
        ) {

            return true;
        }

        if (
                lower.contains("framework")
        ) {

            return false;
        }

        boolean generationVerb =
                lower.contains("generate")
                        ||
                        lower.contains("create")
                        ||
                        lower.contains("add")
                        ||
                        lower.contains("write")
                        ||
                        lower.contains("produce");

        boolean testArtifact =
                lower.contains("feature")
                        ||
                        lower.contains("scenario")
                        ||
                        lower.contains("test")
                        ||
                        lower.contains("case");

        boolean expandedCoverage =
                lower.contains("more")
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

        return (
                generationVerb
                        &&
                        (
                                testArtifact
                                        ||
                                        expandedCoverage
                        )
        )
                ||
                (
                        testArtifact
                                &&
                                expandedCoverage
                                &&
                                extractFeatureName(message) != null
                );
    }

    // =====================================================
    // FRAMEWORK GENERATION DETECTION
    // =====================================================

    private boolean containsFrameworkGenerationIntent(
            String message
    ) {

        String lower =
                message.toLowerCase();

        return lower.contains("generate")
                &&
                lower.contains("framework");
    }

    private boolean containsRequirementDocument(
            String message
    ) {

        if (
                message == null
        ) {

            return false;
        }

        String lower =
                message.toLowerCase();

        boolean hasStoryId =
                Pattern.compile("(?i)\\bUS[-_]?\\d+\\s*:")
                        .matcher(message)
                        .find();

        boolean hasRequirementShape =
                lower.contains("acceptance criteria")
                        &&
                        (
                                lower.contains("user story")
                                        ||
                                        lower.contains("user stories")
                                        ||
                                        lower.contains("as a ")
                                        ||
                                        lower.contains("as an ")
                        );

        return (
                hasStoryId
                        &&
                        (
                                lower.contains("acceptance criteria")
                                        ||
                                        lower.contains("user stories")
                                        ||
                                        lower.contains("as a ")
                        )
        )
                ||
                hasRequirementShape;
    }

    // =====================================================
    // LOCAL RULE ENGINE
    // =====================================================

    private AICommand parseCompoundCommand(
            String message,
            Map<String, String> variables
    ) {

        List<String> clauses =
                splitCompoundClauses(message);

        if (
                clauses.size() < 2
        ) {

            return null;
        }

        List<AICommand> commands =
                new ArrayList<>();

        for (
                String clause
                : clauses
        ) {

            AICommand command =
                    parseCompoundClause(
                            clause,
                            variables
                    );

            if (
                    command != null
            ) {

                commands.add(command);
            }
        }

        if (
                commands.size() < 2
        ) {

            return null;
        }

        return AICommand.builder()
                .intent("COMPOUND_COMMAND")
                .commands(commands)
                .variables(variables)
                .message(message)
                .build();
    }

    private List<String> splitCompoundClauses(
            String message
    ) {

        if (
                message == null
                        ||
                        message.isBlank()
        ) {

            return List.of();
        }

        String[] parts =
                Pattern.compile(
                                "(?i)\\s+(?:and\\s+then|then|after\\s+that)\\s+|;\\s*|\\s+and\\s+(?=(?:can\\s+you\\s+|please\\s+)?(?:run|execute|start|create|generate|add|write|produce)\\b)"
                        )
                        .split(message);

        List<String> clauses =
                new ArrayList<>();

        for (
                String part
                : parts
        ) {

            String clause =
                    part.trim();

            if (
                    !clause.isBlank()
            ) {

                clauses.add(clause);
            }
        }

        return clauses;
    }

    private AICommand parseCompoundClause(
            String clause,
            Map<String, String> variables
    ) {

        if (
                containsFrameworkGenerationIntent(clause)
        ) {

            return AICommand.builder()
                    .intent("GENERATE_FRAMEWORK")
                    .url(
                            extractUrl(clause)
                    )
                    .variables(variables)
                    .message(clause)
                    .build();
        }

        if (
                containsFeatureGenerationIntent(clause)
        ) {

            String featureName =
                    extractFeatureName(clause);

            return AICommand.builder()
                    .intent("GENERATE_FEATURE")
                    .flowName(featureName)
                    .featureName(featureName)
                    .url(
                            extractUrl(clause)
                    )
                    .variables(variables)
                    .message(clause)
                    .build();
        }

        if (
                containsGeneratedTestExecutionRequest(clause)
        ) {

            return AICommand.builder()
                    .intent("EXECUTE_GENERATED_TESTS")
                    .target(
                            extractTagExpression(clause)
                    )
                    .variables(variables)
                    .message(clause)
                    .build();
        }

        return null;
    }

    private AICommand localRuleParse(
            String message
    ) {

        String lower =
                message.toLowerCase();

        Map<String, String> variables =
                extractVariables(message);

        // =====================================================
        // DOWNLOAD FRAMEWORK
        // =====================================================

        if (
                lower.contains("download")
                        ||
                        (
                                variables.isEmpty()
                                        &&
                                        containsStandaloneZipRequest(lower)
                        )
        ) {

            return AICommand.builder()

                    .intent("DOWNLOAD_FRAMEWORK")

                    .artifactName("framework")

                    .message(message)

                    .build();
        }

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
                containsFrameworkGenerationIntent(message)
        ) {

            return AICommand.builder()

                    .intent("GENERATE_FRAMEWORK")

                    .url(
                            extractUrl(message)
                    )

                    .variables(variables)

                    .message(message)

                    .build();
        }

        // =====================================================
        // GENERATE FEATURE
        // =====================================================

        if (
                containsFeatureGenerationIntent(message)
        ) {

            String featureName =
                    extractFeatureName(message);

            return AICommand.builder()

                    .intent("GENERATE_FEATURE")

                    .flowName(featureName)

                    .featureName(featureName)

                    .url(
                            extractUrl(message)
                    )

                    .variables(variables)

                    .message(message)

                    .build();
        }

        // =====================================================
        // UPDATE TEST DATA
        // =====================================================

        if (
                !variables.isEmpty()
        ) {

            return AICommand.builder()

                    .intent("UPDATE_TEST_DATA")

                    .variables(variables)

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

            String featureName =
                    extractFeatureName(message);

            if (
                    lower.contains("feature")
                            ||
                            featureName != null
            ) {

                return AICommand.builder()

                        .intent("EXECUTE_FEATURE")

                        .featureName(featureName)

                        .target(
                                extractExecutionTarget(message)
                        )

                        .url(
                                extractUrl(message)
                        )

                        .message(message)

                        .build();
            }

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

    private boolean containsGeneratedTestTagRequest(
            String message
    ) {

        String lower =
                message.toLowerCase();

        return lower.contains("tag")
                &&
                (
                        lower.contains("give")
                                ||
                                lower.contains("provide")
                                ||
                                lower.contains("show")
                                ||
                                lower.contains("list")
                                ||
                                lower.contains("what")
                                ||
                                lower.contains("which")
                )
                &&
                (
                        lower.contains("generated")
                                ||
                                lower.contains("test")
                );
    }

    private boolean containsStandaloneZipRequest(
            String lower
    ) {

        return Pattern.compile("\\bzip\\b")
                .matcher(lower)
                .find()
                &&
                !lower.contains("zip code")
                &&
                !lower.contains("zipcode");
    }

    private boolean containsGeneratedTestExecutionRequest(
            String message
    ) {

        String lower =
                message.toLowerCase();

        boolean hasRunIntent =
                (
                lower.contains("run")
                        ||
                        lower.contains("execute")
                        ||
                        lower.contains("start")
                );

        boolean referencesTests =
                (
                        lower.contains("test")
                                ||
                                lower.contains("generated")
                                ||
                                lower.contains("@")
                );

        boolean hasExplicitTarget =
                (
                        lower.contains("tag")
                                ||
                        lower.contains("@")
                                ||
                                lower.contains("all")
                                ||
                        lower.contains("bill pay")
                                ||
                                lower.contains("billpay")
                );

        return hasRunIntent
                &&
                referencesTests
                &&
                (
                        hasExplicitTarget
                                ||
                                containsGeneratedTestFeatureTarget(message)
                );
    }

    private String extractTagExpression(
            String message
    ) {

        String lower =
                message.toLowerCase();

        String explicitTagExpression =
                extractExplicitTagExpression(
                        message,
                        lower
                );

        if (
                explicitTagExpression != null
        ) {

            return explicitTagExpression;
        }

        if (
                lower.contains("bill pay")
                        ||
                        lower.contains("billpay")
        ) {

            return billPayTagExpression(lower);
        }

        if (
                lower.contains("register")
                        ||
                        lower.contains("registration")
                        ||
                        lower.contains("signup")
                        ||
                        lower.contains("sign up")
        ) {

            return featureTagExpression(
                    "(@register or @registration)",
                    lower
            );
        }

        if (
                lower.contains("all")
                        &&
                lower.contains("generated")
                        &&
                        lower.contains("test")
        ) {

            return "ALL";
        }

        Pattern pattern =
                Pattern.compile(
                        "@\\s*[A-Za-z0-9_\\-]+"
                );

        Matcher matcher =
                pattern.matcher(message);

        List<String> tags =
                new ArrayList<>();

        while (
                matcher.find()
        ) {

            tags.add(
                    matcher.group()
                            .replaceAll(
                                    "\\s+",
                                    ""
                            )
            );
        }

        if (
                tags.isEmpty()
        ) {

            String tagName =
                    extractPlainTagName(message);

            if (
                    tagName != null
            ) {

                return "@"
                        + tagName;
            }

            String featureTagName =
                    extractGeneratedTestFeatureTagName(message);

            if (
                    featureTagName != null
            ) {

                return featureTagExpression(
                        "@"
                                + featureTagName,
                        lower
                );
            }

            return "ALL";
        }

        String operator =
                lower.contains(" or ")
                        ? " or "
                        : " and ";

        return String.join(
                operator,
                tags
        );
    }

    private String extractExplicitTagExpression(
            String message,
            String lower
    ) {

        Pattern pattern =
                Pattern.compile(
                        "@\\s*[A-Za-z0-9_\\-]+"
                );

        Matcher matcher =
                pattern.matcher(message);

        List<String> tags =
                new ArrayList<>();

        while (
                matcher.find()
        ) {

            tags.add(
                    matcher.group()
                            .replaceAll(
                                    "\\s+",
                                    ""
                            )
            );
        }

        if (
                !tags.isEmpty()
        ) {

            String operator =
                    lower.contains(" or ")
                            ? " or "
                            : " and ";

            return String.join(
                    operator,
                    tags
            );
        }

        String tagName =
                extractPlainTagName(message);

        if (
                tagName == null
        ) {

            return null;
        }

        return "@"
                + tagName;
    }

    private boolean containsGeneratedTestFeatureTarget(
            String message
    ) {

        if (
                extractFeatureName(message) != null
        ) {

            return true;
        }

        String target =
                extractGeneratedTestTargetPhrase(message);

        return target != null
                &&
                !target.isBlank();
    }

    private String extractGeneratedTestFeatureTagName(
            String message
    ) {

        String featureName =
                extractFeatureName(message);

        if (
                featureName != null
        ) {

            return toTagName(featureName);
        }

        String target =
                extractGeneratedTestTargetPhrase(message);

        if (
                target == null
        ) {

            return null;
        }

        return toTagName(target);
    }

    private String extractGeneratedTestTargetPhrase(
            String message
    ) {

        List<Pattern> patterns =
                List.of(
                        Pattern.compile(
                                "(?i)\\b(?:run|execute|start)\\s+(?:the\\s+)?(?:generated\\s+)?tests?\\s+(?:for|of|on)\\s+(.+)$"
                        ),
                        Pattern.compile(
                                "(?i)\\b(?:run|execute|start)\\s+(?:the\\s+)?(.+?)\\s+(?:generated\\s+)?tests?\\b"
                        )
                );

        for (
                Pattern pattern
                : patterns
        ) {

            Matcher matcher =
                    pattern.matcher(message);

            if (
                    matcher.find()
            ) {

                String cleaned =
                        cleanGeneratedTestTargetPhrase(
                                matcher.group(1)
                        );

                if (
                        !cleaned.isBlank()
                ) {

                    return cleaned;
                }
            }
        }

        return null;
    }

    private String cleanGeneratedTestTargetPhrase(
            String value
    ) {

        if (
                value == null
        ) {

            return "";
        }

        String cleaned =
                cleanToken(value)
                        .replaceAll(
                                "(?i)\\b(?:with|using)\\s+(?:tag|tags|filter)\\b.*$",
                                ""
                        )
                        .replaceAll(
                                "(?i)\\b(?:now|please)$",
                                ""
                        )
                        .replaceAll(
                                "(?i)\\b(?:page|feature|flow|scenario|test|tests)$",
                                ""
                        )
                        .replaceAll(
                                "(?i)^a\\s+",
                                ""
                        )
                        .trim();

        if (
                cleaned.equalsIgnoreCase("user")
        ) {

            return "";
        }

        return cleaned;
    }

    private String toTagName(
            String value
    ) {

        if (
                value == null
        ) {

            return null;
        }

        String tagName =
                value.toLowerCase()
                        .replaceAll(
                                "\\b(?:a|an|the|user)\\b",
                                " "
                        )
                        .replaceAll(
                                "[^a-z0-9]+",
                                "_"
                        )
                        .replaceAll(
                                "^_+|_+$",
                                ""
                        );

        return tagName.isBlank()
                ? null
                : tagName;
    }

    private String featureTagExpression(
            String baseExpression,
            String lower
    ) {

        List<String> coverageTags =
                coverageTags(lower);

        if (
                coverageTags.isEmpty()
        ) {

            return baseExpression;
        }

        String coverageExpression =
                coverageTags.size() == 1
                        ? coverageTags.get(0)
                        : "("
                        + String.join(
                        " or ",
                        coverageTags
                )
                        + ")";

        return baseExpression
                + " and "
                + coverageExpression;
    }

    private String extractPlainTagName(
            String message
    ) {

        Pattern pattern =
                Pattern.compile(
                        "(?i)\\btag(?:ged)?\\s+(?:@\\s*)?([A-Za-z0-9_\\-]+)"
                );

        Matcher matcher =
                pattern.matcher(message);

        if (
                !matcher.find()
        ) {

            return null;
        }

        return matcher.group(1);
    }

    private String billPayTagExpression(
            String lower
    ) {

        return featureTagExpression(
                "(@bill_pay or @billpay)",
                lower
        );
    }

    private List<String> coverageTags(
            String lower
    ) {

        List<String> coverageTags =
                new ArrayList<>();

        if (
                lower.contains("positive")
                        ||
                        lower.contains("successful")
                        ||
                        lower.contains("happy path")
        ) {

            coverageTags.add("@positive");
        }

        if (
                lower.contains("negative")
        ) {

            coverageTags.add("@negative");
        }

        if (
                lower.contains("required")
                        ||
                        lower.contains("mandatory")
        ) {

            coverageTags.add("@required_field");
        }

        if (
                lower.contains("validation")
                        ||
                        lower.contains("invalid")
        ) {

            coverageTags.add("@validation");
        }

        if (
                lower.contains("boundary")
                        ||
                        lower.contains("edge")
        ) {

            coverageTags.add("@boundary");
        }

        return coverageTags;
    }

    private boolean containsGeneratedTestRepairRequest(
            String message
    ) {

        String lower =
                message.toLowerCase();

        boolean assertionCorrection =
                lower.contains("assertion")
                        &&
                        (
                                lower.contains("actual")
                                        ||
                                        lower.contains("actually")
                                        ||
                                        lower.contains("should be")
                                        ||
                                        lower.contains("should say")
                                        ||
                                        lower.contains("replace")
                                        ||
                                        lower.contains("correct")
                        );

        if (
                assertionCorrection
        ) {

            return true;
        }

        boolean actualAssertionFollowUp =
                (
                        lower.contains("actual sentence")
                                ||
                                lower.contains("actual text")
                                ||
                                lower.contains("actual message")
                                ||
                                lower.contains("real sentence")
                                ||
                                lower.contains("real text")
                                ||
                                lower.contains("real message")
                )
                        &&
                        (
                                lower.contains(" is ")
                                        ||
                                        lower.contains(" was ")
                                        ||
                                        lower.contains("-->")
                                        ||
                                        lower.contains(":")
                        );

        if (
                actualAssertionFollowUp
        ) {

            return true;
        }

        boolean failureSignal =
                lower.contains("failed")
                        ||
                        lower.contains("fails")
                        ||
                        lower.contains("failing")
                        ||
                        lower.contains("failure")
                        ||
                        lower.contains("failures")
                        ||
                        lower.contains("error")
                        ||
                        lower.contains("broken")
                        ||
                        lower.contains("last test")
                        ||
                        lower.contains("look at it again")
                        ||
                        lower.contains("check it again");

        boolean repairSignal =
                lower.contains("fix")
                        ||
                        lower.contains("repair")
                        ||
                        lower.contains("rectify")
                        ||
                        lower.contains("correct")
                        ||
                        lower.contains("update")
                        ||
                        lower.contains("look at")
                        ||
                        lower.contains("check");

        boolean generatedTestContext =
                lower.contains("last test")
                        ||
                        lower.contains("generated test")
                        ||
                        lower.contains("test")
                        ||
                        lower.contains("scenario")
                        ||
                        lower.contains("cucumber")
                        ||
                        lower.contains("report")
                        ||
                        lower.contains("test failed")
                        ||
                        lower.contains("failed test")
                        ||
                        lower.contains("failures")
                        ||
                        lower.contains("feature")
                        ||
                        lower.contains("generated")
                        ||
                        containsWholeWord(
                                lower,
                                "it"
                        )
                        ||
                        containsWholeWord(
                                lower,
                                "this"
                        )
                        ||
                        containsWholeWord(
                                lower,
                                "that"
                        )
                        ||
                        lower.contains("look at it again")
                        ||
                        lower.contains("check it again");

        return failureSignal
                &&
                repairSignal
                &&
                generatedTestContext;
    }

    private boolean containsWholeWord(
            String lower,
            String word
    ) {

        return Pattern.compile(
                        "\\b" + Pattern.quote(word) + "\\b"
                )
                .matcher(lower)
                .find();
    }

    // =====================================================
    // TARGET EXTRACTION
    // =====================================================

    private String extractExecutionTarget(
            String message
    ) {

        String lower =
                message.toLowerCase();

        String featureName =
                extractFeatureName(message);

        if (
                featureName != null
        ) {

            return featureName;
        }

        String url =
                extractUrl(message);

        if (
                url != null
        ) {

            return extractDomain(url);
        }

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

        Pattern pattern =
                Pattern.compile(
                        "(https?://[^\\s\"']+)|((?:www\\.)?[A-Za-z0-9-]+\\.[A-Za-z]{2,}(?:/[^\\s\"']*)?)"
                );

        Matcher matcher =
                pattern.matcher(message);

        if (
                matcher.find()
        ) {

            String value =
                    cleanToken(
                            matcher.group()
                    );

            if (
                    !value.startsWith("http://")
                            &&
                            !value.startsWith("https://")
            ) {

                value =
                        "https://"
                                + value;
            }

            return value;
        }

        return null;
    }

    // =====================================================
    // FEATURE NAME EXTRACTION
    // =====================================================

    private String extractFeatureName(
            String message
    ) {

        String lower =
                message.toLowerCase();

        if (
                lower.contains("bill pay")
                        ||
                        lower.contains("billpay")
                        ||
                        lower.contains("bill payment")
                        ||
                        lower.contains("pay bill")
        ) {

            return "bill pay";
        }

        if (
                lower.contains("login")
        ) {

            return "login";
        }

        if (
                lower.contains("search")
        ) {

            return "search";
        }

        if (
                lower.contains("register")
                        ||
                        lower.contains("signup")
                        ||
                        lower.contains("sign up")
        ) {

            return "registration";
        }

        if (
                lower.contains("checkout")
        ) {

            return "checkout";
        }

        if (
                lower.contains("form")
        ) {

            return "form";
        }

        return null;
    }

    // =====================================================
    // VARIABLE EXTRACTION
    // =====================================================

    private Map<String, String> extractVariables(
            String message
    ) {

        Map<String, String> variables =
                new HashMap<>();

        Pattern quoted =
                Pattern.compile(
                        "\\b([A-Za-z][A-Za-z0-9_ -]{1,40})\\s*(?:=|:|is|as)\\s*[\"']([^\"']+)[\"']",
                        Pattern.CASE_INSENSITIVE
                );

        Matcher quotedMatcher =
                quoted.matcher(message);

        while (
                quotedMatcher.find()
        ) {

            putVariable(
                    variables,
                    quotedMatcher.group(1),
                    quotedMatcher.group(2)
            );
        }

        Pattern unquoted =
                Pattern.compile(
                        "\\b(username|user|password|pass|confirm password|confirmation password|email|ssn|search term|search|first name|last name|phone|payee|address|city|state|postal code|zip code|postcode|zip|account|verify account|amount|product|quantity|token|otp)\\s*(?:=|:|is|as)\\s*([^,;]+?)(?=\\s+(?:and\\s+)?(?:username|user|password|pass|confirm password|confirmation password|email|ssn|search term|search|first name|last name|phone|payee|address|city|state|postal code|zip code|postcode|zip|account|verify account|amount|product|quantity|token|otp)\\s*(?:=|:|is|as)\\s*|$|[,;])",
                        Pattern.CASE_INSENSITIVE
                );

        Matcher unquotedMatcher =
                unquoted.matcher(message);

        while (
                unquotedMatcher.find()
        ) {

            putVariable(
                    variables,
                    unquotedMatcher.group(1),
                    unquotedMatcher.group(2)
            );
        }

        return variables;
    }

    private void putVariable(

            Map<String, String> variables,

            String key,

            String value

    ) {

        String normalizedKey =
                normalizeVariableKey(key);

        if (
                normalizedKey == null
                        ||
                        value == null
                        ||
                        value.isBlank()
        ) {

            return;
        }

        variables.put(
                normalizedKey,
                cleanToken(value)
        );
    }

    private String normalizeVariableKey(
            String key
    ) {

        if (
                key == null
        ) {

            return null;
        }

        String normalized =
                key.trim()
                        .toLowerCase()
                        .replace("-", " ")
                        .replace("_", " ");

        if (
                normalized.endsWith(" username")
        ) {

            normalized =
                    "username";
        }

        if (
                normalized.endsWith(" password")
        ) {

            normalized =
                    "password";
        }

        return switch (normalized) {

            case "user" -> "username";

            case "pass" -> "password";

            case "confirm password",
                 "confirmation password" -> "confirmPassword";

            case "search term" -> "search";

            case "first name" -> "firstName";

            case "last name" -> "lastName";

            case "postal code",
                 "zip code",
                 "postcode" -> "zip";

            case "verify account" -> "account";

            default -> normalized.replace(" ", "");
        };
    }

    private Map<String, String> mergeVariables(

            Map<String, String> first,

            Map<String, String> second

    ) {

        Map<String, String> merged =
                new HashMap<>();

        if (
                first != null
        ) {

            merged.putAll(first);
        }

        if (
                second != null
        ) {

            merged.putAll(second);
        }

        return merged;
    }

    private String firstNonBlank(

            String first,

            String second

    ) {

        if (
                first != null
                        &&
                        !first.isBlank()
        ) {

            return first;
        }

        return second;
    }

    private String extractDomain(
            String url
    ) {

        if (
                url == null
        ) {

            return null;
        }

        return url.replace("https://", "")
                .replace("http://", "")
                .replace("www.", "")
                .split("/")[0];
    }

    private String cleanToken(
            String value
    ) {

        if (
                value == null
        ) {

            return null;
        }

        return value.trim()
                .replaceAll("[\\)\\].,;]+$", "");
    }
}
