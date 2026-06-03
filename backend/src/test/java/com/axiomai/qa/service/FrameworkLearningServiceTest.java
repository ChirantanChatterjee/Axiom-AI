package com.axiomai.qa.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameworkLearningServiceTest {

    @Test
    void recordsStructuredRuntimeRepairPatternsAndGuidance() {

        GeneratedProjectWriterService writer =
                new GeneratedProjectWriterService(
                        null,
                        null,
                        null
                );

        FrameworkLearningService service =
                new FrameworkLearningService(writer);

        String sessionId =
                "learning-test-"
                        + System.nanoTime();

        try {

            boolean learned =
                    service.recordRuntimeRepairLearning(
                            sessionId,
                            "The last generated test could not resolve element: A-Z",
                            List.of(
                                    "Changed generated sort dropdown step for \"Name (A to Z)\" to supported value \"az\".",
                                    "Changed username field input from password value to ${username}."
                            )
                    );

            assertTrue(
                    learned
            );

            List<FrameworkLearningService.LearnedRepairPattern> patterns =
                    service.repairPatterns(sessionId);

            assertTrue(
                    patterns.stream()
                            .anyMatch(pattern -> pattern.category()
                                    .equals("sort-dropdown-values"))
            );

            assertTrue(
                    patterns.stream()
                            .anyMatch(pattern -> pattern.category()
                                    .equals("credential-field-value-mismatch"))
            );

            String guidance =
                    service.runtimeRepairGuidance(sessionId);

            assertTrue(
                    guidance.contains("sort-dropdown-values")
            );

            assertTrue(
                    guidance.contains("credential-field-value-mismatch")
            );

        } finally {

            writer.deleteWorkspace(sessionId);
        }
    }
}
