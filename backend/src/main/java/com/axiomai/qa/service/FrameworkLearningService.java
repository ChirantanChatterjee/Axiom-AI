package com.axiomai.qa.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class FrameworkLearningService {

    private static final String LEARNING_FILE =
            "framework-learning.md";

    private static final String UPLOAD_MARKER =
            "user-uploaded-framework.marker";

    private final GeneratedProjectWriterService generatedProjectWriterService;

    public boolean hasUserUploadedFramework(
            String sessionId
    ) {

        return Files.exists(
                generatedProjectWriterService
                        .getWorkspaceRoot(sessionId)
                        .resolve(UPLOAD_MARKER)
        );
    }

    public String learningSummary(
            String sessionId
    ) {

        Path learningFile =
                generatedProjectWriterService
                        .getWorkspaceRoot(sessionId)
                        .resolve(LEARNING_FILE);

        if (
                !Files.exists(learningFile)
        ) {

            return "";
        }

        try {
            return Files.readString(learningFile);
        } catch (IOException e) {
            return "";
        }
    }

    public FrameworkSnapshot snapshot(
            Path frameworkRoot
    ) {

        return new FrameworkSnapshot(
                featureFiles(frameworkRoot),
                tags(frameworkRoot)
        );
    }

    public String recordUploadLearning(

            String sessionId,
            String uploadedFileName,
            FrameworkSnapshot before,
            FrameworkSnapshot after

    ) {

        Set<String> addedTags =
                new LinkedHashSet<>(after.tags());

        addedTags.removeAll(
                before.tags()
        );

        Set<String> addedFeatures =
                new LinkedHashSet<>(after.featureFiles());

        addedFeatures.removeAll(
                before.featureFiles()
        );

        String summary =
                """
                # User Framework Learning

                Uploaded at: %s
                Uploaded file: %s

                Observed user modifications:
                - Feature files now present: %s
                - New feature files added by user: %s
                - Tags now present: %s
                - New tags added by user: %s

                Generation guidance:
                - Preserve user-added tags when creating related tests.
                - Prefer the existing uploaded framework support code when executing tests.
                - When new tests are generated in this session, keep tag names concise and consistent with the user-added tag style.
                - If a user-added feature demonstrates domain-specific wording, mirror that wording in future Gherkin scenarios for this session.
                """.formatted(
                        Instant.now(),
                        uploadedFileName == null
                                ? "uploaded framework"
                                : uploadedFileName,
                        String.join(", ", after.featureFiles()),
                        joinOrNone(addedFeatures),
                        String.join(", ", after.tags()),
                        joinOrNone(addedTags)
                );

        try {
            Path workspaceRoot =
                    generatedProjectWriterService
                            .getWorkspaceRoot(sessionId);

            Files.createDirectories(workspaceRoot);

            Files.writeString(
                    workspaceRoot.resolve(LEARNING_FILE),
                    summary
            );

            Files.writeString(
                    workspaceRoot.resolve(UPLOAD_MARKER),
                    Instant.now()
                            .toString()
            );
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to record uploaded framework learning.",
                    e
            );
        }

        return summary;
    }

    public boolean recordRuntimeRepairLearning(

            String sessionId,
            String failureSummary,
            List<String> repairChanges

    ) {

        if (
                sessionId == null
                        ||
                        sessionId.isBlank()
                        ||
                        failureSummary == null
                        ||
                        failureSummary.isBlank()
                        ||
                        failureSummary.contains(
                                "did not contain a recognized failure signature"
                        )
                        ||
                        failureSummary.startsWith(
                                "No previous generated-test output"
                        )
        ) {

            return false;
        }

        String actions =
                repairChanges == null
                        ||
                        repairChanges.isEmpty()
                        ? "No generated feature file was changed. Refresh support files and use this failure pattern for future repair decisions."
                        : String.join(
                        "; ",
                        repairChanges
                );

        String summary =
                """

                # Runtime Repair Learning

                Recorded at: %s

                Observed failure:
                - %s

                Repair action:
                - %s

                Generation guidance:
                - Recognize this failure signature when the user asks AIF to inspect the last failed generated test.
                - Prefer dynamic assertion waits and app-observable success or validation text over brittle single-read assertions.
                - For ParaBank bill pay, verify login, Bill Pay navigation, account confirmation, and post-submit page state before changing locators.
                - Keep generating negative and boundary scenarios, but express validation assertions as observable outcomes rather than guessed exact text.
                """.formatted(
                        Instant.now(),
                        failureSummary,
                        actions
                );

        try {
            Path workspaceRoot =
                    generatedProjectWriterService
                            .getWorkspaceRoot(sessionId);

            Files.createDirectories(workspaceRoot);

            Files.writeString(
                    workspaceRoot.resolve(LEARNING_FILE),
                    summary,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

            return true;
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to record runtime repair learning.",
                    e
            );
        }
    }

    private Set<String> featureFiles(
            Path frameworkRoot
    ) {

        Set<String> features =
                new LinkedHashSet<>();

        Path featureRoot =
                frameworkRoot.resolve(
                        "src/test/resources/features"
                );

        if (
                !Files.exists(featureRoot)
        ) {

            return features;
        }

        try (
                Stream<Path> paths =
                        Files.walk(featureRoot)
        ) {

            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName()
                            .toString()
                            .endsWith(".feature"))
                    .sorted()
                    .forEach(path -> features.add(
                            featureRoot.relativize(path)
                                    .toString()
                                    .replace("\\", "/")
                    ));

        } catch (IOException ignored) {
        }

        return features;
    }

    private Set<String> tags(
            Path frameworkRoot
    ) {

        Set<String> tags =
                new LinkedHashSet<>();

        Pattern pattern =
                Pattern.compile("@[A-Za-z0-9_\\-]+");

        Path featureRoot =
                frameworkRoot.resolve(
                        "src/test/resources/features"
                );

        if (
                !Files.exists(featureRoot)
        ) {

            return tags;
        }

        try (
                Stream<Path> paths =
                        Files.walk(featureRoot)
        ) {

            List<Path> featureFiles =
                    paths.filter(Files::isRegularFile)
                            .filter(path -> path.getFileName()
                                    .toString()
                                    .endsWith(".feature"))
                            .sorted()
                            .toList();

            for (
                    Path featureFile
                    : featureFiles
            ) {

                Matcher matcher =
                        pattern.matcher(
                                Files.readString(featureFile)
                        );

                while (
                        matcher.find()
                ) {

                    tags.add(
                            matcher.group()
                    );
                }
            }

        } catch (IOException ignored) {
        }

        return tags;
    }

    private String joinOrNone(
            Set<String> values
    ) {

        if (
                values == null
                        ||
                        values.isEmpty()
        ) {

            return "none";
        }

        return String.join(", ", values);
    }

    public record FrameworkSnapshot(
            Set<String> featureFiles,
            Set<String> tags
    ) {
    }
}
