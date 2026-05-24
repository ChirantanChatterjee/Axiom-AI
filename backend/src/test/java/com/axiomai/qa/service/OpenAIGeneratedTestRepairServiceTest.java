package com.axiomai.qa.service;

import com.axiomai.ai.service.OpenAIService;
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
