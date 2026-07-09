package com.axiomai.qa.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedProjectWriterServiceTest {

    @Test
    void writingAdditionalNegativeTestsMergesWithExistingGeneratedFeature() throws Exception {

        GeneratedProjectWriterService writerService =
                writerService();

        String sessionId =
                "writer-merge-"
                        + UUID.randomUUID();

        Path featureFile =
                writerService.getFrameworkRoot(sessionId)
                        .resolve("src/test/resources/features/generated.feature");

        try {

            writerService.writeFeatureFile(
                    sessionId,
                    "generated",
                    existingFeature()
            );

            writerService.writeFeatureFile(
                    sessionId,
                    "generated",
                    additionalNegativeScenarios()
            );

            String finalFeature =
                    Files.readString(featureFile);

            assertEquals(
                    8,
                    scenarioCount(finalFeature)
            );

            assertTrue(
                    finalFeature.contains("Scenario: Existing generated scenario 5")
            );

            assertTrue(
                    finalFeature.contains("Scenario: Reject invalid amount 3")
            );

            assertTrue(
                    finalFeature.contains("@generated @form_submission @negative")
            );

            Path backup =
                    featureFile.resolveSibling("generated.feature.bak");

            assertTrue(
                    Files.exists(backup)
            );

            assertEquals(
                    5,
                    scenarioCount(
                            Files.readString(backup)
                    )
            );

        } finally {

            writerService.deleteWorkspace(sessionId);
        }
    }

    @Test
    void emptyGeneratedFeatureUpdateDoesNotOverwriteExistingFeature() throws Exception {

        GeneratedProjectWriterService writerService =
                writerService();

        String sessionId =
                "writer-preserve-"
                        + UUID.randomUUID();

        Path featureFile =
                writerService.getFrameworkRoot(sessionId)
                        .resolve("src/test/resources/features/generated.feature");

        try {

            writerService.writeFeatureFile(
                    sessionId,
                    "generated",
                    existingFeature()
            );

            String before =
                    Files.readString(featureFile);

            assertThrows(
                    IllegalStateException.class,
                    () -> writerService.writeFeatureFile(
                            sessionId,
                            "generated",
                            "Feature: generated\n"
                    )
            );

            String after =
                    Files.readString(featureFile);

            assertEquals(
                    before,
                    after
            );

            assertEquals(
                    5,
                    scenarioCount(after)
            );

        } finally {

            writerService.deleteWorkspace(sessionId);
        }
    }

    private GeneratedProjectWriterService writerService() {

        return new GeneratedProjectWriterService(
                new HookGeneratorService(),
                new RunnerGeneratorService(),
                new PomGeneratorService()
        );
    }

    private String existingFeature() {

        StringBuilder feature =
                new StringBuilder(
                        "Feature: generated\n\n"
                );

        for (
                int index = 1;
                index <= 5;
                index++
        ) {

            feature.append("@generated @flow_")
                    .append(index)
                    .append("\n")
                    .append("Scenario: Existing generated scenario ")
                    .append(index)
                    .append("\n")
                    .append("  Given user launches \"https://example.test\"\n")
                    .append("  Then flow should complete successfully\n\n");
        }

        return feature.toString();
    }

    private String additionalNegativeScenarios() {

        StringBuilder feature =
                new StringBuilder(
                        "Feature: generated\n\n"
                );

        for (
                int index = 1;
                index <= 3;
                index++
        ) {

            feature.append("@form_submission\n")
                    .append("Scenario: Reject invalid amount ")
                    .append(index)
                    .append("\n")
                    .append("  Given user launches \"https://example.test\"\n")
                    .append("  When user enters \"invalid\" into \"amount\"\n")
                    .append("  Then user should see \"validation error\"\n\n");
        }

        return feature.toString();
    }

    private int scenarioCount(
            String feature
    ) {

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
}
