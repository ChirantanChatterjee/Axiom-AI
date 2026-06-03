package com.axiomai.qa.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
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

    private static final String LEARNING_RULES_FILE =
            "framework-learning-rules.tsv";

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

    public String runtimeRepairGuidance(
            String sessionId
    ) {

        List<LearnedRepairPattern> patterns =
                repairPatterns(sessionId);

        if (
                patterns.isEmpty()
        ) {

            return "";
        }

        StringBuilder guidance =
                new StringBuilder(
                        "Learned local repair patterns for this session:"
                );

        for (
                LearnedRepairPattern pattern
                : patterns
        ) {

            guidance.append(System.lineSeparator())
                    .append("- ")
                    .append(pattern.category())
                    .append(": ")
                    .append(pattern.guidance());
        }

        return guidance.toString();
    }

    public List<LearnedRepairPattern> repairPatterns(
            String sessionId
    ) {

        if (
                sessionId == null
                        ||
                        sessionId.isBlank()
        ) {

            return List.of();
        }

        Path rulesFile =
                generatedProjectWriterService
                        .getWorkspaceRoot(sessionId)
                        .resolve(LEARNING_RULES_FILE);

        if (
                !Files.exists(rulesFile)
        ) {

            return List.of();
        }

        try {

            return Files.readAllLines(rulesFile)
                    .stream()
                    .map(this::parsePattern)
                    .filter(pattern -> pattern != null)
                    .distinct()
                    .toList();

        } catch (IOException e) {

            return List.of();
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
                - For authenticated flows, distinguish runtime credential/app-state failures from locator failures before changing generated feature files.
                - For multi-step form flows, verify navigation, duplicate/confirmation fields, and post-submit page state before changing locators.
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

            appendRepairPatterns(
                    workspaceRoot,
                    failureSummary,
                    repairChanges
            );

            return true;
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to record runtime repair learning.",
                    e
            );
        }
    }

    private void appendRepairPatterns(
            Path workspaceRoot,
            String failureSummary,
            List<String> repairChanges
    ) throws IOException {

        List<LearnedRepairPattern> patterns =
                classifyRepairPatterns(
                        failureSummary,
                        repairChanges
                );

        if (
                patterns.isEmpty()
        ) {

            return;
        }

        Set<String> existing =
                new LinkedHashSet<>();

        Path rulesFile =
                workspaceRoot.resolve(LEARNING_RULES_FILE);

        if (
                Files.exists(rulesFile)
        ) {

            existing.addAll(
                    Files.readAllLines(rulesFile)
            );
        }

        List<String> nextLines =
                new ArrayList<>();

        for (
                LearnedRepairPattern pattern
                : patterns
        ) {

            String line =
                    patternLine(pattern);

            if (
                    existing.add(line)
            ) {

                nextLines.add(line);
            }
        }

        if (
                nextLines.isEmpty()
        ) {

            return;
        }

        Files.writeString(
                rulesFile,
                String.join(
                        System.lineSeparator(),
                        nextLines
                )
                        + System.lineSeparator(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

    private List<LearnedRepairPattern> classifyRepairPatterns(
            String failureSummary,
            List<String> repairChanges
    ) {

        if (
                repairChanges == null
                        ||
                        repairChanges.isEmpty()
        ) {

            return List.of();
        }

        List<LearnedRepairPattern> patterns =
                new ArrayList<>();

        for (
                String change
                : repairChanges
        ) {

            String lower =
                    change == null
                            ? ""
                            : change.toLowerCase();

            if (
                    lower.contains("sort")
                            &&
                            (
                                    lower.contains("supported value")
                                            ||
                                            lower.contains("select value")
                                            ||
                                            lower.contains("dropdown")
                            )
            ) {

                patterns.add(
                        learnedPattern(
                                "sort-dropdown-values",
                                failureSummary,
                                "For generated sorting tests, repair option clicks/selects into dropdown value entries. SauceDemo values are az, za, lohi, and hilo.",
                                change
                        )
                );
            }

            if (
                    lower.contains("generated click target")
                            ||
                            lower.contains("generated sort action from click")
                            ||
                            lower.contains("nonexistent")
            ) {

                patterns.add(
                        learnedPattern(
                                "bad-generated-action-target",
                                failureSummary,
                                "When runtime evidence shows a generated action target does not exist, update the feature step to the observed visible action label instead of adding fake aliases.",
                                change
                        )
                );
            }

            if (
                    lower.contains("assertion text")
                            ||
                            lower.contains("expected text")
            ) {

                patterns.add(
                        learnedPattern(
                                "assertion-text-mismatch",
                                failureSummary,
                                "For assertion mismatches, prefer exact user/runtime-provided UI text. Do not guess one actual sentence for multiple failed assertions.",
                                change
                        )
                );
            }

            if (
                    lower.contains("username field")
                            ||
                            lower.contains("password field")
                            ||
                            lower.contains("${username}")
                            ||
                            lower.contains("${password}")
            ) {

                patterns.add(
                        learnedPattern(
                                "credential-field-value-mismatch",
                                failureSummary,
                                "For login steps, keep username-like targets filled with ${username} and password-like targets filled with ${password}.",
                                change
                        )
                );
            }

            if (
                    lower.contains("passenger")
                            ||
                            lower.contains("first name")
                            ||
                            lower.contains("last name")
            ) {

                patterns.add(
                        learnedPattern(
                                "travel-passenger-details",
                                failureSummary,
                                "For Agile Travel return journeys, after flight-search Continue, assert First Name and Last Name, enter passenger names, then click next.",
                                change
                        )
                );
            }

            if (
                    lower.contains("bill pay")
                            ||
                            lower.contains("verify account")
                            ||
                            lower.contains("send payment")
            ) {

                patterns.add(
                        learnedPattern(
                                "parabank-bill-pay",
                                failureSummary,
                                "For ParaBank Bill Pay, launch the home page, log in, open Bill Pay, fill verify account with the account value, and click send payment button.",
                                change
                        )
                );
            }

            if (
                    lower.contains("intermediate assertion")
                            ||
                            lower.contains("removed invalid")
            ) {

                patterns.add(
                        learnedPattern(
                                "brittle-intermediate-assertion",
                                failureSummary,
                                "Remove a failing intermediate assertion only when later generated steps show the test should continue past that state.",
                                change
                        )
                );
            }
        }

        return patterns.stream()
                .distinct()
                .toList();
    }

    private LearnedRepairPattern learnedPattern(
            String category,
            String failureSummary,
            String guidance,
            String exampleChange
    ) {

        return new LearnedRepairPattern(
                category,
                safeField(failureSummary),
                guidance,
                safeField(exampleChange)
        );
    }

    private LearnedRepairPattern parsePattern(
            String line
    ) {

        if (
                line == null
                        ||
                        line.isBlank()
        ) {

            return null;
        }

        String[] parts =
                line.split(
                        "\\t",
                        -1
                );

        if (
                parts.length < 4
        ) {

            return null;
        }

        return new LearnedRepairPattern(
                parts[0],
                parts[1],
                parts[2],
                parts[3]
        );
    }

    private String patternLine(
            LearnedRepairPattern pattern
    ) {

        return safeField(pattern.category())
                + "\t"
                + safeField(pattern.failureSignature())
                + "\t"
                + safeField(pattern.guidance())
                + "\t"
                + safeField(pattern.exampleChange());
    }

    private String safeField(
            String value
    ) {

        if (
                value == null
        ) {

            return "";
        }

        return value.trim()
                .replace('\t', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replaceAll(
                        "\\s+",
                        " "
                );
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

    public record LearnedRepairPattern(
            String category,
            String failureSignature,
            String guidance,
            String exampleChange
    ) {
    }
}
