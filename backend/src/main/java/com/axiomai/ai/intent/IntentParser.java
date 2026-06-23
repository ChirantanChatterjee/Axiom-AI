package com.axiomai.ai.intent;

import com.axiomai.ai.dto.AICommand;
import com.axiomai.ai.execution.AIExecutionPlan;
import com.axiomai.ai.model.GPTIntentResponse;
import com.axiomai.ai.planner.ScenarioPlanner;
import com.axiomai.ai.service.OpenAIIntentService;
import com.axiomai.ml.AIFMLPredictionService;
import com.axiomai.ml.IntentClassificationLabel;
import com.axiomai.ml.MLPrediction;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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

    private AIFMLPredictionService
            aifMLPredictionService;

    @Autowired(required = false)
    public void setAifMLPredictionService(
            AIFMLPredictionService aifMLPredictionService
    ) {

        this.aifMLPredictionService =
                aifMLPredictionService;
    }

    // =====================================================
    // MAIN PARSER
    // =====================================================

    public AICommand parse(
            String message
    ) {

        Map<String, String> variables =
                extractRuntimeVariables(message);

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
                containsGeneratedTestUpdateIntent(message)
        ) {

            String featureName =
                    extractFeatureName(message);

            return AICommand.builder()
                    .intent("GENERATE_FEATURE")
                    .flowName(
                            featureName == null
                                    ? "generated"
                                    : featureName
                    )
                    .featureName(
                            featureName == null
                                    ? "generated"
                                    : featureName
                    )
                    .url(
                            extractUrl(message)
                    )
                    .variables(variables)
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

        if (
                isVariableUpdateOnlyMessage(message)
                        &&
                        !variables.isEmpty()
        ) {

            return AICommand.builder()

                    .intent("UPDATE_TEST_DATA")

                    .variables(variables)

                    .message(message)

                    .build();
        }

        MLPrediction mlIntentPrediction =
                predictIntentWithML(message);

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

            AICommand command =
                    AICommand.builder()

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

            return completeMlIntentPrediction(
                    command,
                    mlIntentPrediction,
                    true
            );
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

            AICommand command =
                    AICommand.builder()

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

            return completeMlIntentPrediction(
                    command,
                    mlIntentPrediction,
                    true
            );
        }

        AICommand mlCommand =
                commandFromMLPrediction(
                        mlIntentPrediction,
                        message,
                        variables
                );

        if (
                mlCommand != null
        ) {

            return completeMlIntentPrediction(
                    mlCommand,
                    mlIntentPrediction,
                    true
            );
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

            AICommand command =
                    AICommand.builder()

                    .intent("AI_EXECUTION")

                    .executionPlan(plan)

                    .message(message)

                    .build();

            return completeMlIntentPrediction(
                    command,
                    mlIntentPrediction,
                    true
            );
        }

        // =====================================================
        // FALLBACK
        // =====================================================

        AICommand command =
                localRuleParse(message);

        return completeMlIntentPrediction(
                command,
                mlIntentPrediction,
                true
        );
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

        if (
                lower.contains("test data")
                        ||
                        lower.contains("runtime data")
                        ||
                        lower.contains("runtime variable")
                        ||
                        lower.contains("runtime value")
        ) {

            return false;
        }

        boolean generationVerb =
                Pattern.compile(
                                "\\b(?:generate|create|add|write|produce|update|modify|extend|enhance|improve)\\b"
                        )
                        .matcher(lower)
                        .find();

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

        long distinctIntents =
                commands.stream()
                        .map(AICommand::getIntent)
                        .filter(intent -> intent != null)
                        .map(String::toUpperCase)
                        .distinct()
                        .count();

        if (
                distinctIntents < 2
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
                                "(?i)\\s+(?:and\\s+then|then|after\\s+that)\\s+|;\\s*|\\s+and\\s+(?=(?:can\\s+you\\s+|please\\s+)?(?:run|execute|start|create|generate|add|write|produce|update|modify|extend|fix|repair|resolve|heal|rectify|correct|stabilize|analyse|analyze|diagnose|inspect)\\b)"
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

        if (
                containsGeneratedTestRepairRequest(clause)
        ) {

            return AICommand.builder()
                    .intent("REPAIR_GENERATED_TESTS")
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
                extractRuntimeVariables(message);

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
                        ||
                        lower.contains("rerun")
                        ||
                        lower.contains("re-run")
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
                                ||
                                containsConversationalGeneratedTestRerun(lower)
                );
    }

    private boolean containsConversationalGeneratedTestRerun(
            String lower
    ) {

        if (
                lower == null
        ) {

            return false;
        }

        return lower.contains("rerun")
                ||
                lower.contains("re-run")
                ||
                lower.contains("again")
                ||
                lower.contains("same")
                ||
                lower.contains("last");
    }

    private boolean containsGeneratedTestUpdateIntent(
            String message
    ) {

        if (
                message == null
                        ||
                        message.isBlank()
        ) {

            return false;
        }

        String lower =
                message.toLowerCase();

        if (
                lower.contains("test data")
                        ||
                        lower.contains("runtime data")
                        ||
                        lower.contains("runtime variable")
                        ||
                        lower.contains("runtime value")
        ) {

            return false;
        }

        boolean updateVerb =
                Pattern.compile(
                                "\\b(?:update|modify|extend|enhance|improve|add|append|include|cover|create|generate)\\b"
                        )
                        .matcher(lower)
                        .find();

        boolean testArtifact =
                lower.contains("generated test")
                        ||
                        lower.contains("tests")
                        ||
                        lower.contains("test cases")
                        ||
                        lower.contains("scenarios")
                        ||
                        lower.contains("coverage");

        boolean testUpdateShape =
                Pattern.compile(
                                "\\b(?:tests?|scenarios?|test\\s+cases?)\\s+(?:to\\s+)?(?:add|include|cover|update|modify|extend)\\b"
                        )
                        .matcher(lower)
                        .find()
                        ||
                        lower.contains("tests_to_add");

        boolean failureReference =
                lower.contains("failed")
                        ||
                        lower.contains("failing")
                        ||
                        lower.contains("failure")
                        ||
                        lower.contains("error");

        if (
                failureReference
                        &&
                        !containsExplicitNewGeneratedTestRequest(lower)
        ) {

            return false;
        }

        return updateVerb
                &&
                (
                        testArtifact
                                ||
                                testUpdateShape
                        );
    }

    private boolean containsExplicitNewGeneratedTestRequest(
            String lower
    ) {

        if (
                lower == null
                        ||
                        lower.isBlank()
        ) {

            return false;
        }

        return Pattern.compile(
                        "\\b(?:add|append|include|cover|create|generate|write|produce|extend|enhance|improve)\\s+(?:more\\s+|additional\\s+|these\\s+|new\\s+)?(?:generated\\s+)?(?:tests?|scenarios?|test\\s+cases?|coverage)\\b"
                )
                .matcher(lower)
                .find()
                ||
                Pattern.compile(
                                "\\b(?:tests?|scenarios?|test\\s+cases?|coverage)\\s+(?:for|around|covering|to\\s+cover)\\b"
                        )
                        .matcher(lower)
                        .find();
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
                explicitTagOperator(lower);

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
                    explicitTagOperator(lower);

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

    private String explicitTagOperator(
            String lower
    ) {

        if (
                lower == null
        ) {

            return " or ";
        }

        if (
                lower.contains("must have all")
                        ||
                        lower.contains("matching all")
                        ||
                        lower.contains("with all tags")
                        ||
                        lower.contains("containing all tags")
        ) {

            return " and ";
        }

        if (
                lower.contains(" or ")
                        ||
                        lower.contains(" and ")
                        ||
                        lower.contains(",")
        ) {

            return " or ";
        }

        return " and ";
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
                                "(?i)\\b(?:run|rerun|re-run|execute|start)\\s+(?:the\\s+)?(?:generated\\s+)?tests?\\s+(?:for|of|on)\\s+(.+)$"
                        ),
                        Pattern.compile(
                                "(?i)\\b(?:run|rerun|re-run|execute|start)\\s+(?:the\\s+)?(.+?)\\s+(?:generated\\s+)?tests?\\b"
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
                        !isGenericGeneratedTestTarget(cleaned)
                ) {

                    return cleaned;
                }
            }
        }

        return null;
    }

    private boolean isGenericGeneratedTestTarget(
            String target
    ) {

        if (
                target == null
        ) {

            return true;
        }

        String lower =
                target.trim()
                        .toLowerCase();

        return lower.isBlank()
                ||
                lower.equals("me")
                ||
                lower.equals("us")
                ||
                lower.equals("generated")
                ||
                lower.equals("generated test")
                ||
                lower.equals("generated tests")
                ||
                lower.equals("test")
                ||
                lower.equals("tests")
                ||
                lower.equals("same")
                ||
                lower.equals("last")
                ||
                lower.equals("current");
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
                                "[?!]+$",
                                ""
                        )
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

        if (
                containsGeneratedFieldValueCorrection(lower)
        ) {

            return true;
        }

        if (
                containsGeneratedElementTextCorrection(lower)
        ) {

            return true;
        }

        if (
                containsDirectGeneratedRepairCommand(lower)
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
                        lower.contains("incorrect")
                        ||
                        lower.contains("wrong")
                        ||
                        lower.contains("heal")
                        ||
                        lower.contains("healing")
                        ||
                        lower.contains("filled with")
                        ||
                        lower.contains("getting filled")
                        ||
                        lower.contains("value provided for")
                        ||
                        lower.contains("last test")
                        ||
                        lower.contains("look at it again")
                        ||
                        lower.contains("check it again");

        boolean repairSignal =
                        lower.contains("fix")
                        ||
                        lower.contains("resolve")
                        ||
                        lower.contains("repair")
                        ||
                        lower.contains("heal")
                        ||
                        lower.contains("healing")
                        ||
                        lower.contains("rectify")
                        ||
                        lower.contains("correct")
                        ||
                        lower.contains("incorrect")
                        ||
                        lower.contains("wrong")
                        ||
                        lower.contains("invalid")
                        ||
                        lower.contains("update")
                        ||
                        lower.contains("look at")
                        ||
                        lower.contains("check")
                        ||
                        lower.contains("analyse")
                        ||
                        lower.contains("analyze")
                        ||
                        lower.contains("diagnose")
                        ||
                        lower.contains("inspect")
                        ||
                        lower.contains("stabilize")
                        ||
                        lower.contains("make it pass")
                        ||
                        lower.contains("make the test pass")
                        ||
                        lower.contains("make tests pass")
                        ||
                        lower.contains("use what you learned")
                        ||
                        lower.contains("apply learned");

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

        boolean diagnosticRequest =
                (
                        lower.contains("why")
                                ||
                                lower.contains("analyse")
                                ||
                                lower.contains("analyze")
                                ||
                                lower.contains("diagnose")
                                ||
                                lower.contains("inspect")
                                ||
                                lower.contains("investigate")
                )
                        &&
                        (
                                lower.contains("failed")
                                        ||
                                        lower.contains("failure")
                                        ||
                                        lower.contains("error")
                                        ||
                                        lower.contains("last")
                        );

        return generatedTestContext
                &&
                (
                        (
                                failureSignal
                                        &&
                                        repairSignal
                        )
                                ||
                                diagnosticRequest
                );
    }

    private boolean containsDirectGeneratedRepairCommand(
            String lower
    ) {

        if (
                lower == null
                        ||
                        lower.isBlank()
        ) {

            return false;
        }

        if (
                lower.contains("test data")
                        ||
                        lower.contains("runtime data")
        ) {

            return false;
        }

        boolean repairVerb =
                Pattern.compile(
                                "\\b(?:fix|repair|resolve|heal|rectify|correct|stabilize|stabilise|analy[sz]e|diagnose|inspect|investigate)\\b"
                        )
                        .matcher(lower)
                        .find()
                        ||
                        lower.contains("make it pass")
                        ||
                        lower.contains("make the test pass")
                        ||
                        lower.contains("make tests pass")
                        ||
                        lower.contains("use what you learned")
                        ||
                        lower.contains("apply learned");

        if (
                !repairVerb
        ) {

            return false;
        }

        boolean generatedContext =
                lower.contains("generated")
                        ||
                        lower.contains("cucumber")
                        ||
                        lower.contains("gherkin")
                        ||
                        lower.contains("test")
                        ||
                        lower.contains("scenario")
                        ||
                        lower.contains("last")
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
                        );

        return generatedContext;
    }

    private boolean containsGeneratedFieldValueCorrection(
            String lower
    ) {

        if (
                lower == null
                        ||
                        lower.isBlank()
        ) {

            return false;
        }

        boolean fieldContext =
                lower.contains("field")
                        ||
                        lower.contains("username")
                        ||
                        lower.contains("password")
                        ||
                        lower.contains("auth");

        boolean valueMismatch =
                lower.contains("filled with")
                        ||
                        lower.contains("getting filled")
                        ||
                        lower.contains("value provided for")
                        ||
                        lower.contains("entered is incorrect")
                        ||
                        lower.contains("is incorrect")
                        ||
                        lower.contains("wrong value")
                        ||
                        lower.contains("incorrect value");

        boolean correctionRequest =
                lower.contains("fix")
                        ||
                        lower.contains("resolve")
                        ||
                        lower.contains("rectify")
                        ||
                        lower.contains("correct")
                        ||
                        lower.contains("please");

        return fieldContext
                &&
                valueMismatch
                &&
                correctionRequest;
    }

    private boolean containsGeneratedElementTextCorrection(
            String lower
    ) {

        if (
                lower == null
                        ||
                        lower.isBlank()
        ) {

            return false;
        }

        boolean elementContext =
                lower.contains("element")
                        ||
                        lower.contains("option")
                        ||
                        lower.contains("dropdown")
                        ||
                        lower.contains("drop-down")
                        ||
                        lower.contains("select")
                        ||
                        lower.contains("label")
                        ||
                        lower.contains("text");

        boolean correctionContext =
                lower.contains("expected")
                        ||
                        lower.contains("should be")
                        ||
                        lower.contains("actual")
                        ||
                        lower.contains("actually")
                        ||
                        lower.contains("correct")
                        ||
                        lower.contains("is called")
                        ||
                        lower.contains("is labelled")
                        ||
                        lower.contains("is labeled")
                        ||
                        lower.contains("text is")
                        ||
                        Pattern.compile(
                                        "\\b(?:element|option|dropdown|drop-down|select|label|text)\\s+(?:is|are)\\b"
                                )
                                .matcher(lower)
                                .find();

        boolean quotedValue =
                Pattern.compile("\"[^\"]+\"|'[^']+'")
                        .matcher(lower)
                        .find();

        return elementContext
                &&
                correctionContext
                &&
                (
                        quotedValue
                                ||
                                containsKnownSortOptionText(lower)
                );
    }

    private boolean containsKnownSortOptionText(
            String lower
    ) {

        if (
                lower == null
        ) {

            return false;
        }

        String normalized =
                lower.replaceAll(
                                "[\\u2010-\\u2015]",
                                "-"
                        )
                        .replaceAll(
                                "\\s+",
                                " "
                        );

        return normalized.contains("name (a to z)")
                ||
                normalized.contains("name (z to a)")
                ||
                normalized.contains("price (low to high)")
                ||
                normalized.contains("price (high to low)")
                ||
                normalized.contains("a-z")
                ||
                normalized.contains("z-a")
                ||
                normalized.contains("low-high")
                ||
                normalized.contains("high-low")
                ||
                normalized.contains("low to high")
                ||
                normalized.contains("high to low");
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
                containsWholeWord(
                        lower,
                        "form"
                )
        ) {

            return "form";
        }

        return null;
    }

    // =====================================================
    // VARIABLE EXTRACTION
    // =====================================================

    private Map<String, String> extractRuntimeVariables(
            String message
    ) {

        Map<String, String> variables =
                extractVariables(message);

        if (
                variables.isEmpty()
                        &&
                        isVariableUpdateOnlyMessage(message)
        ) {

            return extractGenericVariables(message);
        }

        return variables;
    }

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
                        "\\b(username|user|password|pass|confirm password|confirmation password|email|ssn|search term|search|first name|firstname|last name|lastname|phone|payee|address|city|state|postal code|zip code|postcode|zip|account|verify account|amount|product|quantity|token|otp|from|to|origin|destination|journey type|journey)\\s*(?:=|:|is|as)\\s*([^,;]+?)(?=\\s+(?:and\\s+)?(?:username|user|password|pass|confirm password|confirmation password|email|ssn|search term|search|first name|firstname|last name|lastname|phone|payee|address|city|state|postal code|zip code|postcode|zip|account|verify account|amount|product|quantity|token|otp|from|to|origin|destination|journey type|journey)\\s*(?:=|:|is|as)\\s*|$|[,;])",
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

    private Map<String, String> extractGenericVariables(
            String message
    ) {

        Map<String, String> variables =
                new HashMap<>();

        Pattern generic =
                Pattern.compile(
                        "\\b([A-Za-z][A-Za-z0-9_-]{0,40})\\s*(?:=|:|\\bis\\b|\\bas\\b)\\s*([^,;]+?)(?=\\s*(?:[,;]|\\band\\b)?\\s*[A-Za-z][A-Za-z0-9_-]{0,40}\\s*(?:=|:|\\bis\\b|\\bas\\b)\\s*|$|[,;])",
                        Pattern.CASE_INSENSITIVE
                );

        Matcher matcher =
                generic.matcher(message);

        while (
                matcher.find()
        ) {

            putGenericVariable(
                    variables,
                    matcher.group(1),
                    matcher.group(2)
            );
        }

        return variables;
    }

    private boolean isVariableUpdateOnlyMessage(
            String message
    ) {

        if (
                message == null
                        ||
                        message.isBlank()
        ) {

            return false;
        }

        String lowerMessage =
                message.toLowerCase();

        if (
                containsNonVariableUpdateLanguage(lowerMessage)
        ) {

            return false;
        }

        Matcher assignment =
                Pattern.compile(
                        "\\b[A-Za-z][A-Za-z0-9_ -]{0,40}\\s*(?:=|:|\\bis\\b|\\bas\\b)",
                        Pattern.CASE_INSENSITIVE
                )
                        .matcher(message);

        if (
                !assignment.find()
        ) {

            return false;
        }

        String prefix =
                message.substring(
                                0,
                                assignment.start()
                        )
                        .toLowerCase();

        return !Pattern.compile(
                        "\\b(run|execute|start|generate|create|add|write|produce|provide|show|give|download|report|framework|feature|scenario|database|db|tag|tags|failed|failure|failing|repair|fix|gherkin|invalid|button|field|element|option|dropdown|select|label|page|click|clicking|after|because)\\b"
                )
                .matcher(prefix)
                .find();
    }

    private boolean containsNonVariableUpdateLanguage(
            String lower
    ) {

        if (
                lower == null
                        ||
                        lower.isBlank()
        ) {

            return false;
        }

        return Pattern.compile(
                        "\\b(run|execute|start|generate|create|add|write|produce|provide|show|give|download|report|framework|feature|scenario|database|db|tag|tags|failed|failure|failing|repair|fix|resolve|rectify|incorrect|wrong|invalid|button|field|element|option|dropdown|select|label|page|click|clicking|after|because|screenshot|screenshots)\\b"
                )
                .matcher(lower)
                .find()
                ||
                lower.contains("filled with")
                ||
                lower.contains("getting filled")
                ||
                lower.contains("value provided for")
                ||
                lower.contains("can you");
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

    private void putGenericVariable(

            Map<String, String> variables,

            String key,

            String value

    ) {

        String normalizedKey =
                normalizeGenericVariableKey(key);

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

            case "origin" -> "from";

            case "destination" -> "to";

            case "journey type" -> "journeyType";

            case "first name",
                 "firstname" -> "firstName";

            case "last name",
                 "lastname" -> "lastName";

            case "postal code",
                 "zip code",
                 "postcode" -> "zip";

            case "verify account" -> "account";

            default -> normalized.replace(" ", "");
        };
    }

    private String normalizeGenericVariableKey(
            String key
    ) {

        if (
                key == null
        ) {

            return null;
        }

        String known =
                normalizeVariableKey(key);

        String cleaned =
                key.trim()
                        .replace("-", " ")
                        .replace("_", " ")
                        .replaceAll("[^A-Za-z0-9 ]", "")
                        .trim();

        if (
                cleaned.isBlank()
        ) {

            return null;
        }

        String genericKey =
                toVariableKey(cleaned);

        if (
                isDeniedGenericVariableKey(genericKey)
        ) {

            return null;
        }

        if (
                isKnownRuntimeVariableKey(known)
        ) {

            return known;
        }

        return genericKey;
    }

    private boolean isKnownRuntimeVariableKey(
            String key
    ) {

        if (
                key == null
        ) {

            return false;
        }

        return switch (key) {
            case "username",
                 "password",
                 "confirmPassword",
                 "email",
                 "ssn",
                 "search",
                 "firstName",
                 "lastName",
                 "phone",
                 "payee",
                 "address",
                 "city",
                 "state",
                 "zip",
                 "account",
                 "amount",
                 "product",
                 "quantity",
                 "token",
                 "otp",
                 "from",
                 "to",
                 "journeyType",
                 "journey" -> true;
            default -> false;
        };
    }

    private String toVariableKey(
            String cleaned
    ) {

        String[] parts =
                cleaned.split("\\s+");

        if (
                parts.length == 1
        ) {

            String part =
                    parts[0];

            if (
                    part.length() > 1
                            &&
                            Character.isUpperCase(part.charAt(0))
                            &&
                            Character.isLowerCase(part.charAt(1))
            ) {

                return Character.toLowerCase(part.charAt(0))
                        + part.substring(1);
            }

            return part;
        }

        StringBuilder key =
                new StringBuilder(
                        parts[0].toLowerCase()
                );

        for (
                int i = 1;
                i < parts.length;
                i++
        ) {

            if (
                    parts[i].isBlank()
            ) {

                continue;
            }

            key.append(
                    Character.toUpperCase(parts[i].charAt(0))
            );

            if (
                    parts[i].length() > 1
            ) {

                key.append(
                        parts[i].substring(1)
                                .toLowerCase()
                );
            }
        }

        return key.toString();
    }

    private boolean isDeniedGenericVariableKey(
            String key
    ) {

        if (
                key == null
                        ||
                        key.isBlank()
        ) {

            return true;
        }

        return switch (key.toLowerCase()) {
            case "url",
                 "uri",
                 "link",
                 "website",
                 "site",
                 "report",
                 "framework",
                 "feature",
                 "scenario",
                 "test",
                 "tests",
                 "there",
                 "then",
                 "after",
                 "because",
                 "only",
                 "hence",
                 "field",
                 "element",
                 "option",
                 "dropdown",
                 "select",
                 "label",
                 "entered",
                 "filled",
                 "failed",
                 "incorrect",
                 "wrong",
                 "value",
                 "provided",
                 "please",
                 "tag",
                 "tags",
                 "generated" -> true;
            default -> false;
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

    private MLPrediction predictIntentWithML(
            String message
    ) {

        if (
                aifMLPredictionService == null
        ) {

            return null;
        }

        try {

            return aifMLPredictionService.predictIntent(
                    message
            );

        } catch (RuntimeException ignored) {

            return null;
        }
    }

    private AICommand completeMlIntentPrediction(
            AICommand command,
            MLPrediction prediction,
            boolean openAiFallbackUsed
    ) {

        if (
                aifMLPredictionService != null
                        &&
                        prediction != null
        ) {

            try {

                aifMLPredictionService.completePrediction(
                        prediction,
                        openAiFallbackUsed,
                        toMlIntentLabel(
                                command == null
                                        ? null
                                        : command.getIntent()
                        )
                );

            } catch (RuntimeException ignored) {

            }
        }

        return command;
    }

    private AICommand commandFromMLPrediction(
            MLPrediction prediction,
            String message,
            Map<String, String> variables
    ) {

        if (
                prediction == null
                        ||
                        !prediction.isHighConfidence()
                        ||
                        prediction.getPredictedLabel() == null
        ) {

            return null;
        }

        IntentClassificationLabel label;

        try {

            label =
                    IntentClassificationLabel.valueOf(
                            prediction.getPredictedLabel()
                    );

        } catch (IllegalArgumentException e) {

            return null;
        }

        return switch (label) {
            case GENERATE_FRAMEWORK -> AICommand.builder()
                    .intent("GENERATE_FRAMEWORK")
                    .flowName(
                            firstNonBlank(
                                    extractFeatureName(message),
                                    "generated"
                            )
                    )
                    .url(
                            extractUrl(message)
                    )
                    .variables(variables)
                    .message(message)
                    .build();
            case EXECUTE_FLOW -> AICommand.builder()
                    .intent("EXECUTE_FLOW")
                    .target(
                            extractExecutionTarget(message)
                    )
                    .variables(variables)
                    .message(message)
                    .build();
            case SHOW_REPORT -> AICommand.builder()
                    .intent("SHOW_REPORT")
                    .message(message)
                    .build();
            case SHOW_DB -> AICommand.builder()
                    .intent("SHOW_DB")
                    .message(message)
                    .build();
            case REPAIR_TEST -> AICommand.builder()
                    .intent("REPAIR_GENERATED_TESTS")
                    .message(message)
                    .build();
            case UNKNOWN -> null;
        };
    }

    private String toMlIntentLabel(
            String intent
    ) {

        if (
                intent == null
                        ||
                        intent.isBlank()
        ) {

            return IntentClassificationLabel.UNKNOWN.name();
        }

        return switch (
                intent.trim()
                        .toUpperCase()
        ) {
            case "GENERATE_FRAMEWORK",
                 "GENERATE_FEATURE" ->
                    IntentClassificationLabel.GENERATE_FRAMEWORK.name();
            case "EXECUTE_FLOW",
                 "EXECUTE_FEATURE",
                 "EXECUTE_GENERATED_TESTS",
                 "AI_EXECUTION" ->
                    IntentClassificationLabel.EXECUTE_FLOW.name();
            case "SHOW_REPORT" ->
                    IntentClassificationLabel.SHOW_REPORT.name();
            case "SHOW_DB" ->
                    IntentClassificationLabel.SHOW_DB.name();
            case "REPAIR_GENERATED_TESTS" ->
                    IntentClassificationLabel.REPAIR_TEST.name();
            default ->
                    IntentClassificationLabel.UNKNOWN.name();
        };
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
