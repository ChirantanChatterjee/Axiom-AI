package com.axiomai.qa.service;

import com.axiomai.ai.service.OpenAIService;
import com.axiomai.ml.AIFRepairMLContext;
import com.axiomai.ml.AIFMLModelNames;
import com.axiomai.ml.FailureClassificationLabel;
import com.axiomai.ml.MLPrediction;
import com.axiomai.ml.RepairRecommendationLabel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAIGeneratedTestRepairServiceTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    @Test
    void appliesOpenAIRepairAndRedactsSensitivePromptData() throws Exception {

        Path featureFile =
                tempDir.resolve(
                        "src/test/resources/features/sample.feature"
                );

        Files.createDirectories(
                featureFile.getParent()
        );

        Files.writeString(
                featureFile,
                """
                        Feature: sample

                        @sample
                        Scenario: stale assertion
                          Given user launches "https://example.test"
                          Then user should see "Old Text"
                        """
        );

        String repairedFeature =
                """
                        Feature: sample

                        @sample
                        Scenario: stale assertion
                          Given user launches "https://example.test"
                          Then user should see "New Text"
                        """;

        StubOpenAIService openAI =
                new StubOpenAIService(
                        objectMapper.writeValueAsString(
                                Map.of(
                                        "canRepair",
                                        true,
                                        "failureSummary",
                                        "The scenario asserted stale page text.",
                                        "failureDetails",
                                        List.of(
                                                "The generated test expected Old Text, but the current page renders New Text."
                                        ),
                                        "changes",
                                        List.of(
                                                "Updated the feature assertion from Old Text to New Text."
                                        ),
                                        "files",
                                        List.of(
                                                Map.of(
                                                        "path",
                                                        "src/test/resources/features/sample.feature",
                                                        "content",
                                                        repairedFeature
                                                )
                                        )
                                )
                        )
                );

        OpenAIGeneratedTestRepairService service =
                new OpenAIGeneratedTestRepairService(
                        openAI
                );

        OpenAIGeneratedTestRepairService.OpenAIRepairAttempt attempt =
                service.repair(
                        tempDir,
                        "Assertion failed. username is alice@example.com and password is secret123. Authorization: Bearer abc.def",
                        "please fix the failed generated test"
                );

        assertTrue(
                attempt.isRepaired()
        );

        assertEquals(
                "OpenAI",
                attempt.getRepairResult()
                        .getRepairSource()
        );

        assertEquals(
                "The scenario asserted stale page text.",
                attempt.getRepairResult()
                        .getFailureSummary()
        );

        assertTrue(
                Files.readString(featureFile)
                        .contains("New Text")
        );

        assertFalse(
                openAI.prompt.contains("secret123")
        );

        assertFalse(
                openAI.prompt.contains("alice@example.com")
        );

        assertFalse(
                openAI.prompt.contains("abc.def")
        );

        assertTrue(
                openAI.prompt.contains("custom combobox/autocomplete/select widgets")
        );

        assertTrue(
                openAI.prompt.contains("selected chips/single-value tokens")
        );
    }

    @Test
    void returnsUnavailableWhenOpenAIResponseCannotBeApplied() throws Exception {

        Path featureFile =
                tempDir.resolve(
                        "src/test/resources/features/sample.feature"
                );

        Files.createDirectories(
                featureFile.getParent()
        );

        Files.writeString(
                featureFile,
                """
                        Feature: sample
                        Scenario: stale assertion
                          Then user should see "Old Text"
                        """
        );

        OpenAIGeneratedTestRepairService service =
                new OpenAIGeneratedTestRepairService(
                        new StubOpenAIService(
                                "OpenAI request failed."
                        )
                );

        OpenAIGeneratedTestRepairService.OpenAIRepairAttempt attempt =
                service.repair(
                        tempDir,
                        "failure output",
                        "fix it"
                );

        assertFalse(
                attempt.isRepaired()
        );

        assertTrue(
                attempt.getFallbackReason()
                        .contains("OpenAI repair was unavailable")
        );
    }

    @Test
    void enrichesOpenAIRepairPromptWithAifMlContext() throws Exception {

        Path featureFile =
                tempDir.resolve(
                        "src/test/resources/features/sample.feature"
                );

        Files.createDirectories(
                featureFile.getParent()
        );

        Files.writeString(
                featureFile,
                """
                        Feature: sample
                        Scenario: stale locator
                          When user clicks "Login"
                        """
        );

        StubOpenAIService openAI =
                new StubOpenAIService(
                        objectMapper.writeValueAsString(
                                Map.of(
                                        "canRepair",
                                        false,
                                        "failureSummary",
                                        "Needs human review."
                                )
                        )
                );

        OpenAIGeneratedTestRepairService service =
                new OpenAIGeneratedTestRepairService(
                        openAI
                );

        service.repair(
                tempDir,
                "Unable to resolve element: Login",
                "fix locator",
                AIFRepairMLContext.builder()
                        .failurePrediction(
                                MLPrediction.builder()
                                        .modelName(
                                                AIFMLModelNames.FAILURE_CLASSIFICATION
                                        )
                                        .predictedLabel(
                                                FailureClassificationLabel.LOCATOR_FAILURE.name()
                                        )
                                        .confidence(0.91)
                                        .build()
                        )
                        .repairPrediction(
                                MLPrediction.builder()
                                        .modelName(
                                                AIFMLModelNames.REPAIR_RECOMMENDATION
                                        )
                                        .predictedLabel(
                                                RepairRecommendationLabel.UPDATE_LOCATOR.name()
                                        )
                                        .confidence(0.88)
                                        .build()
                        )
                        .similarRepairs(List.of())
                        .build()
        );

        assertTrue(
                openAI.prompt.contains("AIF custom ML repair context")
        );

        assertTrue(
                openAI.prompt.contains("LOCATOR_FAILURE")
        );

        assertTrue(
                openAI.prompt.contains("UPDATE_LOCATOR")
        );
    }

    @Test
    void enrichesOpenAIRepairPromptWithLocatorMismatchEvidence() throws Exception {

        Path featureFile =
                tempDir.resolve(
                        "src/test/resources/features/generated.feature"
                );

        Path pageFile =
                tempDir.resolve(
                        "src/test/java/com/axiomai/generated/pages/GeneratedPage.java"
                );

        Path evidenceFile =
                tempDir.resolve(
                        "target/aif-runtime/action-evidence.json"
                );

        Files.createDirectories(
                featureFile.getParent()
        );
        Files.createDirectories(
                pageFile.getParent()
        );
        Files.createDirectories(
                evidenceFile.getParent()
        );

        Files.writeString(
                featureFile,
                """
                        Feature: generated
                        Scenario: User selects multiple auto-complete suggestions
                          When user enters "Blue" into "Single Color Name"
                        """
        );

        Files.writeString(
                pageFile,
                """
                        package com.axiomai.generated.pages;
                        class GeneratedPage {
                          void fill() {
                            page.locator("input").first();
                          }
                        }
                        """
        );

        Files.writeString(
                evidenceFile,
                """
                        [{"intendedFieldName":"Multiple Color Names","finalResolvedSelector":"input:visible","element":{"id":"autoCompleteSingleInput"}}]
                        """
        );

        StubOpenAIService openAI =
                new StubOpenAIService(
                        objectMapper.writeValueAsString(
                                Map.of(
                                        "canRepair",
                                        false,
                                        "failureSummary",
                                        "Needs locator repair."
                                )
                        )
                );

        OpenAIGeneratedTestRepairService service =
                new OpenAIGeneratedTestRepairService(
                        openAI
                );

        service.repair(
                tempDir,
                "The generated test typed into the wrong textbox.",
                "It is using the wrong field."
        );

        assertTrue(
                openAI.prompt.contains("locatorRepairEvidence")
        );

        assertTrue(
                openAI.prompt.contains("REPAIR_LOCATORS_WITH_RUNTIME_EVIDENCE")
        );

        assertTrue(
                openAI.prompt.contains("runtimeActionEvidence")
        );

        assertTrue(
                openAI.prompt.contains("weakLocatorFindings")
        );

        assertTrue(
                openAI.prompt.contains("locatorRepair")
        );
    }

    @Test
    void enrichesOpenAIRepairPromptWithGuidedFieldLocatorInstruction() throws Exception {

        Path pageFile =
                tempDir.resolve(
                        "src/test/java/com/axiomai/generated/pages/GeneratedPage.java"
                );

        Files.createDirectories(
                pageFile.getParent()
        );

        Files.writeString(
                pageFile,
                """
                        package com.axiomai.generated.pages;
                        class GeneratedPage {
                          void fill() {
                            page.locator("#old-email");
                          }
                        }
                        """
        );

        StubOpenAIService openAI =
                new StubOpenAIService(
                        objectMapper.writeValueAsString(
                                Map.of(
                                        "canRepair",
                                        false,
                                        "failureSummary",
                                        "Needs field locator repair."
                                )
                        )
                );

        OpenAIGeneratedTestRepairService service =
                new OpenAIGeneratedTestRepairService(
                        openAI
                );

        service.repair(
                tempDir,
                "Unable to fill Email field.",
                "the field locator used for \"Email\" field is incorrect can you please fix it?"
        );

        assertTrue(
                openAI.prompt.contains("guidedRepairInstruction")
        );

        assertTrue(
                openAI.prompt.contains("FIELD_LOCATOR")
        );

        assertTrue(
                openAI.prompt.contains("Email")
        );
    }

    private static class StubOpenAIService
            extends OpenAIService {

        private final String response;

        private String prompt =
                "";

        private StubOpenAIService(
                String response
        ) {

            this.response =
                    response;
        }

        @Override
        public String ask(
                String prompt
        ) {

            this.prompt =
                    prompt;

            return response;
        }
    }
}
