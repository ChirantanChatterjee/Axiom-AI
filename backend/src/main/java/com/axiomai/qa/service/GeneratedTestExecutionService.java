package com.axiomai.qa.service;

import com.axiomai.qa.execution.entity.GeneratedTestExecutionJobEntity;
import com.axiomai.qa.execution.service.GeneratedTestExecutionQueueService;
import com.axiomai.qa.generator.flow.FlowPageObjectGenerator;
import com.axiomai.qa.generator.flow.FlowStepDefinitionGenerator;
import com.axiomai.reporting.service.ReportArtifactService;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeneratedTestExecutionService {

    private static final long DEFAULT_TEST_TIMEOUT_MINUTES =
            6;

    private static final int MAX_COMMAND_OUTPUT_CHARS =
            200_000;

    @Value("${aif.generated-tests.timeout-minutes:${AIF_GENERATED_TEST_TIMEOUT_MINUTES:6}}")
    private long generatedTestTimeoutMinutes;

    @Value("${aif.generated-tests.maven-opts}")
    private String generatedTestMavenOpts;

    @Value("${aif.generated-tests.maven-offline:${AIF_GENERATED_TEST_MAVEN_OFFLINE:true}}")
    private boolean generatedTestMavenOffline;

    private final GeneratedProjectWriterService
            generatedProjectWriterService;

    private final GeneratedFrameworkPersistenceService
            generatedFrameworkPersistenceService;

    private final ReportArtifactService
            reportArtifactService;

    private final FlowPageObjectGenerator
            flowPageObjectGenerator;

    private final HookGeneratorService
            hookGeneratorService;

    private final RunnerGeneratorService
            runnerGeneratorService;

    private final PomGeneratorService
            pomGeneratorService;

    private final FrameworkLearningService
            frameworkLearningService;

    private final GeneratedFeatureRepairService
            generatedFeatureRepairService;

    private final OpenAIGeneratedTestRepairService
            openAIGeneratedTestRepairService;

    private final GeneratedTestExecutionQueueService
            generatedTestExecutionQueueService;

    public GeneratedTestCatalog listTags(
            String sessionId
    ) {

        Path frameworkRoot =
                resolveFrameworkRoot(sessionId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        missingFrameworkMessage()
                                )
                        );

        boolean featureFilesChanged =
                normalizeFeatureFiles(frameworkRoot);

        if (
                featureFilesChanged
                        &&
                        generatedFrameworkPersistenceService != null
        ) {

            generatedFrameworkPersistenceService
                    .persistFramework(sessionId);
        }

        List<GeneratedTestTag> tags =
                parseTags(frameworkRoot);

        return GeneratedTestCatalog.builder()
                .frameworkRoot(
                        frameworkRoot.toAbsolutePath()
                                .normalize()
                                .toString()
                )
                .tags(tags)
                .message(
                        buildTagMessage(tags)
                )
                .build();
    }

    public GeneratedTestRunResult runTests(

            String sessionId,
            String tagExpression,
            Map<String, String> variables

    ) {

        Path frameworkRoot =
                resolveFrameworkRoot(sessionId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        missingFrameworkMessage()
                                )
                        );

        String normalizedExpression =
                normalizeTagExpression(tagExpression);

        boolean featureFilesChanged =
                normalizeFeatureFiles(frameworkRoot);

        requireMatchingScenarios(
                frameworkRoot,
                normalizedExpression
        );

        List<String> missingVariables =
                missingVariables(
                        frameworkRoot,
                        normalizedExpression,
                        variables
                );

        if (
                !missingVariables.isEmpty()
        ) {

            throw new RuntimeException(
                    missingRuntimeDataMessage(
                            missingVariables
                    )
            );
        }

        List<String> command =
                generatedTestCommand(
                        frameworkRoot,
                        normalizedExpression,
                        shouldUseOfflineMaven()
                );

        try {

            boolean supportFilesChanged =
                    false;

            if (
                    !frameworkLearningService
                            .hasUserUploadedFramework(sessionId)
            ) {

                supportFilesChanged =
                        refreshSupportFiles(frameworkRoot);
            }

            if (
                    (
                            supportFilesChanged
                                    ||
                                    featureFilesChanged
                    )
                            &&
                            generatedFrameworkPersistenceService != null
            ) {

                generatedFrameworkPersistenceService
                        .persistFramework(sessionId);
            }

            CommandResult testResult =
                    runCommand(
                            command,
                            frameworkRoot,
                            variables,
                            false
                    );

            int exitCode =
                    testResult.exitCode();

            String output =
                    testResult.output();

            if (
                    shouldUseOfflineMaven()
                            &&
                            exitCode != 0
                            &&
                            isMavenOfflineDependencyFailure(output)
            ) {

                CommandResult onlineRetryResult =
                        runCommand(
                                generatedTestCommand(
                                        frameworkRoot,
                                        normalizedExpression,
                                        false
                                ),
                                frameworkRoot,
                                variables,
                                false
                        );

                exitCode =
                        onlineRetryResult.exitCode();

                output =
                        combineOfflineRetryOutputs(
                                output,
                                onlineRetryResult.output()
                        );

                command =
                        generatedTestCommand(
                                frameworkRoot,
                                normalizedExpression,
                                false
                        );
            }

            if (
                    exitCode != 0
                            &&
                            isMissingPlaywrightBrowser(output)
            ) {

                CommandResult installResult =
                        installPlaywrightBrowsers(frameworkRoot);

                if (
                        installResult.exitCode() == 0
                ) {

                    CommandResult retryResult =
                            runCommand(
                                    command,
                                    frameworkRoot,
                                    variables,
                                    false
                            );

                    exitCode =
                            retryResult.exitCode();

                    output =
                            combineOutputs(
                                    output,
                                    installResult.output(),
                                    retryResult.output()
                            );

                } else {

                    output =
                            combineOutputs(
                                    output,
                                    installResult.output(),
                                    null
                            );
                }
            }

            String reportUrl =
                    publishReportFromOutput(
                            frameworkRoot,
                            output
                    );

            return GeneratedTestRunResult.builder()
                    .success(exitCode == 0)
                    .tagExpression(
                            normalizedExpression == null
                                    ? "ALL"
                                    : normalizedExpression
                    )
                    .reportUrl(reportUrl)
                    .exitCode(exitCode)
                    .output(tail(output, 12000))
                    .message(
                            buildExecutionMessage(
                                    exitCode,
                                    normalizedExpression,
                                    reportUrl
                            )
                    )
                    .build();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Generated test execution failed: "
                            + e.getMessage(),
                    e
            );
        }
    }

    private String publishReportFromOutput(
            Path frameworkRoot,
            String output
    ) throws IOException {

        persistExecutionOutput(
                frameworkRoot,
                output
        );

        return publishCucumberReport(
                frameworkRoot,
                output
        );
    }

    private void persistExecutionOutput(
            Path frameworkRoot,
            String output
    ) {

        try {

            Path targetRoot =
                    frameworkRoot.resolve("target");

            Files.createDirectories(targetRoot);

            Files.writeString(
                    targetRoot.resolve("aif-generated-test-output.txt"),
                    output == null
                            ? ""
                            : output
            );

        } catch (IOException e) {

            log.warn(
                    "Unable to persist generated test output for repair diagnostics at {}",
                    frameworkRoot,
                    e
            );
        }
    }

    public GeneratedTestRepairResult repairLatestFailure(
            String sessionId
    ) {

        return repairLatestFailure(
                sessionId,
                ""
        );
    }

    public GeneratedTestRepairResult repairLatestFailure(
            String sessionId,
            String userInstruction
    ) {

        Path frameworkRoot =
                resolveFrameworkRoot(sessionId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        missingFrameworkMessage()
                                )
                        );

        try {

            String latestOutput =
                    latestQueuedExecutionOutput(sessionId);

            GeneratedFeatureRepairService.RepairResult deterministicRepair =
                    generatedFeatureRepairService.repair(
                            frameworkRoot,
                            latestOutput,
                            userInstruction
                    );

            GeneratedFeatureRepairService.RepairResult repair;

            if (
                    deterministicRepair.isChanged()
            ) {

                repair =
                        withFallbackRepairMetadata(
                                deterministicRepair,
                                "Applied deterministic feature-file repair before OpenAI because generated test steps can be wrong even when support code is valid."
                        );

            } else {

                OpenAIGeneratedTestRepairService.OpenAIRepairAttempt openAIRepair =
                        openAIGeneratedTestRepairService
                                .repair(
                                        frameworkRoot,
                                        latestOutput,
                                        userInstruction
                                );

                if (
                        openAIRepair.isRepaired()
                ) {

                    repair =
                            openAIRepair.getRepairResult();

                } else {

                    repair =
                            withFallbackRepairMetadata(
                                    deterministicRepair,
                                    openAIRepair.getFallbackReason()
                            );
                }
            }

            boolean learned =
                    frameworkLearningService
                            .recordRuntimeRepairLearning(
                                    sessionId,
                                    repair.getFailureSummary(),
                                    repair.getChanges()
                            );

            boolean supportFilesChanged =
                    false;

            if (
                    !frameworkLearningService
                            .hasUserUploadedFramework(sessionId)
            ) {

                supportFilesChanged =
                        refreshSupportFiles(frameworkRoot);
            }

            if (
                    supportFilesChanged
                            ||
                            repair.isChanged()
            ) {

                generatedFrameworkPersistenceService
                        .persistFramework(sessionId);
            }

            return GeneratedTestRepairResult.builder()
                    .changed(
                            repair.isChanged()
                    )
                    .frameworkRoot(
                            frameworkRoot.toAbsolutePath()
                                    .normalize()
                                    .toString()
                    )
                    .changedFiles(
                            repair.getChangedFiles()
                    )
                    .changes(
                            repair.getChanges()
                    )
                    .failureSummary(
                            repair.getFailureSummary()
                    )
                    .failureDetails(
                            repair.getFailureDetails()
                    )
                    .repairSource(
                            repair.getRepairSource()
                    )
                    .fallbackReason(
                            repair.getFallbackReason()
                    )
                    .learned(learned)
                    .message(
                            buildRepairMessage(
                                    repair,
                                    learned
                            )
                    )
                    .build();

        } catch (IOException e) {

            throw new RuntimeException(
                    "Generated test repair failed: "
                            + e.getMessage(),
                    e
            );
        }
    }

    private GeneratedFeatureRepairService.RepairResult withFallbackRepairMetadata(
            GeneratedFeatureRepairService.RepairResult repair,
            String fallbackReason
    ) {

        return GeneratedFeatureRepairService.RepairResult.builder()
                .changed(
                        repair.isChanged()
                )
                .changedFiles(
                        repair.getChangedFiles()
                )
                .changes(
                        repair.getChanges()
                )
                .failureSummary(
                        repair.getFailureSummary()
                )
                .failureDetails(
                        repair.getFailureDetails()
                )
                .repairGuidance(
                        repair.getRepairGuidance()
                )
                .repairSource("deterministic fallback")
                .fallbackReason(fallbackReason)
                .build();
    }

    private String latestQueuedExecutionOutput(
            String sessionId
    ) {

        return generatedTestExecutionQueueService
                .findLatestForSession(sessionId)
                .map(this::executionOutputForRepair)
                .orElse("");
    }

    private String executionOutputForRepair(
            GeneratedTestExecutionJobEntity job
    ) {

        StringBuilder output =
                new StringBuilder();

        appendRepairLine(
                output,
                "Status",
                job.getStatus()
        );
        appendRepairLine(
                output,
                "Message",
                job.getMessage()
        );
        appendRepairLine(
                output,
                "Error",
                job.getErrorMessage()
        );

        if (
                job.getOutput() != null
                        &&
                        !job.getOutput()
                                .isBlank()
        ) {

            if (
                    output.length() > 0
            ) {

                output.append(System.lineSeparator());
            }

            output.append(job.getOutput());
        }

        return output.toString();
    }

    private void appendRepairLine(
            StringBuilder output,
            String label,
            String value
    ) {

        if (
                value == null
                        ||
                        value.isBlank()
        ) {

            return;
        }

        output.append(label)
                .append(": ")
                .append(value)
                .append(System.lineSeparator());
    }

    private Optional<Path> resolveFrameworkRoot(
            String sessionId
    ) {

        Path sessionRoot =
                generatedProjectWriterService
                        .getFrameworkRoot(sessionId);

        if (
                isRunnableFramework(sessionRoot)
        ) {

            return Optional.of(sessionRoot);
        }

        if (
                sessionId != null
                        &&
                        !sessionId.isBlank()
        ) {

            if (
                    generatedFrameworkPersistenceService
                            .restoreFramework(sessionId)
                            &&
                            isRunnableFramework(sessionRoot)
            ) {

                return Optional.of(sessionRoot);
            }

            return Optional.empty();
        }

        return latestRunnableFramework();
    }

    private String missingFrameworkMessage() {

        if (
                generatedFrameworkPersistenceService
                        .isPersistenceConfigured()
        ) {

            return "No generated framework found for this chat. Generate a framework or tests first.";
        }

        return "No generated framework found for this chat. Generate a framework or tests first. If this happened after a Render restart, configure SUPABASE_SERVICE_ROLE_KEY and AIF_SUPABASE_STORAGE_BUCKET so AIF can restore generated frameworks from Supabase Storage.";
    }

    private Optional<Path> latestRunnableFramework() {

        Path generatedRoot =
                Paths.get("generated-frameworks");

        if (
                !Files.exists(generatedRoot)
        ) {

            return Optional.empty();
        }

        try (
                Stream<Path> paths =
                        Files.list(generatedRoot)
        ) {

            return paths
                    .map(path -> path.resolve("framework"))
                    .filter(this::isRunnableFramework)
                    .max(
                            Comparator.comparingLong(
                                    this::lastModified
                            )
                    );

        } catch (IOException e) {

            return Optional.empty();
        }
    }

    private boolean isRunnableFramework(
            Path root
    ) {

        return root != null
                &&
                Files.exists(
                        root.resolve("pom.xml")
                )
                &&
                hasFeatureFiles(root);
    }

    private boolean refreshSupportFiles(
            Path frameworkRoot
    ) throws IOException {

        Path pageFolder =
                frameworkRoot.resolve(
                        "src/test/java/com/axiomai/generated/pages"
                );

        Path stepFolder =
                frameworkRoot.resolve(
                        "src/test/java/com/axiomai/generated/steps"
                );

        Path hooksFolder =
                frameworkRoot.resolve(
                        "src/test/java/com/axiomai/generated/hooks"
                );

        Path runnerFolder =
                frameworkRoot.resolve(
                        "src/test/java/com/axiomai/generated/runner"
                );

        Files.createDirectories(pageFolder);
        Files.createDirectories(stepFolder);
        Files.createDirectories(hooksFolder);
        Files.createDirectories(runnerFolder);

        boolean changed =
                false;

        changed |= writeStringIfChanged(
                pageFolder.resolve("GeneratedPage.java"),
                flowPageObjectGenerator.generate(List.of())
        );

        changed |= writeStringIfChanged(
                stepFolder.resolve("GeneratedSteps.java"),
                FlowStepDefinitionGenerator.generate(List.of())
        );

        changed |= writeStringIfChanged(
                hooksFolder.resolve("Hooks.java"),
                hookGeneratorService.generateHooks()
        );

        changed |= writeStringIfChanged(
                runnerFolder.resolve("TestRunner.java"),
                runnerGeneratorService.generateRunner()
        );

        changed |= writeStringIfChanged(
                frameworkRoot.resolve("pom.xml"),
                pomGeneratorService.generatePom()
        );

        return changed;
    }

    private boolean writeStringIfChanged(

            Path path,
            String content

    ) throws IOException {

        if (
                Files.exists(path)
                        &&
                        Files.readString(path)
                                .equals(content)
        ) {

            return false;
        }

        Files.writeString(
                path,
                content
        );

        return true;
    }

    private boolean hasFeatureFiles(
            Path root
    ) {

        try (
                Stream<Path> paths =
                        Files.walk(
                                root.resolve(
                                        "src/test/resources/features"
                                )
                        )
        ) {

            return paths.anyMatch(
                    path -> Files.isRegularFile(path)
                            &&
                            path.getFileName()
                                    .toString()
                                    .endsWith(".feature")
            );

        } catch (IOException e) {

            return false;
        }
    }

    private boolean normalizeFeatureFiles(
            Path frameworkRoot
    ) {

        Path featureRoot =
                frameworkRoot.resolve(
                        "src/test/resources/features"
                );

        if (
                !Files.exists(featureRoot)
        ) {

            return false;
        }

        boolean changed =
                false;

        try (
                Stream<Path> paths =
                        Files.walk(featureRoot)
        ) {

            List<Path> featureFiles =
                    paths.filter(Files::isRegularFile)
                            .filter(path -> path.getFileName()
                                    .toString()
                                    .endsWith(".feature"))
                            .toList();

            for (
                    Path featureFile
                    : featureFiles
            ) {

                String content =
                        Files.readString(featureFile);

                String normalized =
                        normalizeFeatureContent(
                                content,
                                featureFile
                        );

                if (
                        !content.equals(normalized)
                ) {

                    Files.writeString(
                            featureFile,
                            normalized
                    );

                    changed =
                            true;
                }
            }

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to normalize generated feature files.",
                    e
            );
        }

        return changed;
    }

    private String normalizeFeatureContent(
            String content,
            Path featureFile
    ) {

        String normalized =
                content.replaceAll(
                        "(?i)\\bYYYY\\b",
                        String.valueOf(
                                Year.now()
                                        .getValue()
                        )
                );

        return ensureScenarioTags(
                normalized,
                featureFileTag(featureFile)
        );
    }

    private String ensureScenarioTags(
            String feature,
            String featureTag
    ) {

        if (
                feature == null
                        ||
                        feature.isBlank()
        ) {

            return "";
        }

        Set<String> requiredTags =
                new LinkedHashSet<>();

        requiredTags.add("@generated");
        requiredTags.add("@ai_requirement");

        if (
                featureTag != null
                        &&
                        !featureTag.isBlank()
        ) {

            requiredTags.add("@"
                    + featureTag);
        }

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
                    trimmed.startsWith("Scenario:")
                            ||
                            trimmed.startsWith("Scenario Outline:")
            ) {

                output.add(
                        scenarioIndent(line)
                                + mergedScenarioTags(
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

    private String mergedScenarioTags(
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

    private String featureFileTag(
            Path featureFile
    ) {

        String fileName =
                featureFile.getFileName()
                        .toString();

        if (
                fileName.endsWith(".feature")
        ) {

            fileName =
                    fileName.substring(
                            0,
                            fileName.length()
                                    - ".feature".length()
                    );
        }

        return fileName.toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private long lastModified(
            Path path
    ) {

        try {

            return Files.getLastModifiedTime(path)
                    .toMillis();

        } catch (IOException e) {

            return 0;
        }
    }

    private List<GeneratedTestTag> parseTags(
            Path frameworkRoot
    ) {

        Map<String, GeneratedTestTagBuilder> tags =
                new LinkedHashMap<>();

        Path featureRoot =
                frameworkRoot.resolve(
                        "src/test/resources/features"
                );

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

                parseFeatureFile(
                        featureFile,
                        tags
                );
            }

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to parse generated feature files.",
                    e
            );
        }

        return tags.values()
                .stream()
                .map(GeneratedTestTagBuilder::build)
                .toList();
    }

    private void parseFeatureFile(

            Path featureFile,
            Map<String, GeneratedTestTagBuilder> tags

    ) throws IOException {

        List<String> lines =
                Files.readAllLines(featureFile);

        String featureName =
                featureFile.getFileName()
                        .toString();

        List<String> pendingTags =
                new ArrayList<>();

        for (
                String line
                : lines
        ) {

            String trimmed =
                    line.trim();

            if (
                    trimmed.startsWith("Feature:")
            ) {

                featureName =
                        trimmed.substring(
                                "Feature:".length()
                        )
                                .trim();

                continue;
            }

            if (
                    trimmed.startsWith("@")
            ) {

                pendingTags.addAll(
                        List.of(
                                trimmed.split("\\s+")
                        )
                );

                continue;
            }

            if (
                    trimmed.startsWith("Scenario:")
                            ||
                            trimmed.startsWith("Scenario Outline:")
            ) {

                String scenario =
                        trimmed.substring(
                                trimmed.indexOf(':') + 1
                        )
                                .trim();

                for (
                        String tag
                        : pendingTags
                ) {

                    if (
                            !tag.startsWith("@")
                    ) {

                        continue;
                    }

                    GeneratedTestTagBuilder builder =
                            tags.computeIfAbsent(
                                    tag,
                                    GeneratedTestTagBuilder::new
                            );

                    builder.addScenario(
                            scenario
                    );

                    builder.addFeature(
                            featureName
                    );
                }

                pendingTags.clear();
            }
        }
    }

    private String buildTagMessage(
            List<GeneratedTestTag> tags
    ) {

        if (
                tags.isEmpty()
        ) {

            return "I could not find any tags in the generated feature files yet. Generate tests first, then ask me for the tags again.";
        }

        StringBuilder message =
                new StringBuilder();

        message.append(
                "Here are the tags available in the generated test framework:\n\n"
        );

        for (
                GeneratedTestTag tag
                : tags
        ) {

            message.append(tag.getTag())
                    .append("\n")
                    .append(tag.getDescription())
                    .append("\n\n");
        }

        message.append(
                "You can ask me to run one of them, for example: `run tests with tag "
        );

        message.append(
                tags.get(0)
                        .getTag()
        );

        message.append(
                "`, or say `run all the generated tests`."
        );

        return message.toString();
    }

    private String buildRepairMessage(
            GeneratedFeatureRepairService.RepairResult repair,
            boolean learned
    ) {

        StringBuilder message =
                new StringBuilder();

        message.append(
                "I inspected the latest generated test failure. "
        );

        message.append(
                repair.getFailureSummary()
        );

        if (
                repair.getRepairSource() != null
                        &&
                        !repair.getRepairSource()
                                .isBlank()
        ) {

            message.append(
                    "\n\nRepair source: "
            );

            message.append(
                    repair.getRepairSource()
            );

            message.append(".");
        }

        if (
                repair.getFallbackReason() != null
                        &&
                        !repair.getFallbackReason()
                                .isBlank()
        ) {

            message.append(
                    "\nOpenAI repair fallback reason: "
            );

            message.append(
                    repair.getFallbackReason()
            );
        }

        if (
                repair.getFailureDetails() != null
                        &&
                        !repair.getFailureDetails()
                                .isEmpty()
        ) {

            message.append(
                    "\n\nWhat was failing:\n"
            );

            for (
                    String detail
                    : repair.getFailureDetails()
            ) {

                message.append("- ")
                        .append(detail)
                        .append("\n");
            }
        }

        if (
                !repair.isChanged()
        ) {

            if (
                    isRuntimeDataFailure(
                            repair.getFailureSummary()
                    )
            ) {

                message.append(
                        "\n\nNo generated feature-file change is needed for this failure. Update the current workspace test data with valid credentials, for example: `username is john and password is demo`, then rerun `@bill_pay`."
                );

                return message.toString();
            }

            if (
                    isBrowserRuntimeFailure(
                            repair.getFailureSummary()
                    )
            ) {

                message.append(
                        "\n\nNo generated feature-file change is needed for this failure. I refreshed the generated support files so future runs use installed Chrome and skip Playwright browser downloads. Rerun the same tag to verify."
                );

                return message.toString();
            }

            if (
                    isAssertionTextMismatchFailure(
                            repair.getFailureSummary()
                    )
            ) {

                message.append(
                        repair.getRepairGuidance() == null
                                ||
                                repair.getRepairGuidance()
                                        .isBlank()
                                ? "\n\nThis looks like an assertion-text mismatch, not a locator or URL problem. I did not change the feature file because I could not infer a safe assertion replacement from the execution output. Tell me the specific mapping, for example: `replace assertion \"old expected text\" with \"actual UI text\"`."
                                : repair.getRepairGuidance()
                );

                return message.toString();
            }

            if (
                    learned
            ) {

                message.append(
                        "\n\nI recognized this failure signature and recorded it as session learning. I did not change generated feature files for this run. The generated support files were refreshed with the latest waits and diagnostics, so rerun the same tag to verify the repaired support behavior."
                );

            } else {

                message.append(
                        "\n\nI did not find a safe automatic feature-file repair for this failure. The generated support files were refreshed, so rerun the test if the failure was caused by stale page objects or step definitions."
                );
            }

            return message.toString();
        }

        message.append(
                "\n\nI updated the generated framework with these repairs:\n"
        );

        for (
                String change
                : repair.getChanges()
        ) {

            message.append("- ")
                    .append(change)
                    .append("\n");
        }

        if (
                repair.getChangedFiles() != null
                        &&
                        !repair.getChangedFiles()
                                .isEmpty()
        ) {

            message.append(
                    "\nChanged files:\n"
            );

            for (
                    String changedFile
                    : repair.getChangedFiles()
            ) {

                message.append("- ")
                        .append(changedFile)
                        .append("\n");
            }
        }

        message.append(
                "\nYou can rerun the same tag or run all generated tests now."
        );

        return message.toString();
    }

    private boolean isRuntimeDataFailure(
            String summary
    ) {

        if (
                summary == null
        ) {

            return false;
        }

        String lower =
                summary.toLowerCase();

        return lower.contains("runtime test-data")
                ||
                lower.contains("missing runtime data")
                ||
                lower.contains("credentials");
    }

    private boolean isBrowserRuntimeFailure(
            String summary
    ) {

        if (
                summary == null
        ) {

            return false;
        }

        String lower =
                summary.toLowerCase();

        return lower.contains("playwright browser")
                ||
                lower.contains("browser-runtime")
                ||
                lower.contains("browser downloads");
    }

    private boolean isAssertionTextMismatchFailure(
            String summary
    ) {

        if (
                summary == null
        ) {

            return false;
        }

        return summary.toLowerCase()
                .contains("page did not show expected text");
    }

    private String buildExecutionMessage(

            int exitCode,
            String tagExpression,
            String reportUrl

    ) {

        String target =
                tagExpression == null
                        ||
                        tagExpression.isBlank()
                        ? "all generated tests"
                        : "tests matching `" + tagExpression + "`";

        String status =
                exitCode == 0
                        ? "completed successfully"
                        : "finished with failures";

        String reportMessage =
                reportUrl == null
                        ? " No Cucumber HTML report was produced."
                        : " You can open the generated Cucumber report from the link below.";

        return "I ran "
                + target
                + ". The execution "
                + status
                + "."
                + reportMessage;
    }

    private String normalizeTagExpression(
            String tagExpression
    ) {

        if (
                tagExpression == null
                        ||
                        tagExpression.isBlank()
                        ||
                        "ALL".equalsIgnoreCase(
                                tagExpression.trim()
                        )
        ) {

            return null;
        }

        return tagExpression.trim()
                .replaceAll(
                        "@\\s+([A-Za-z0-9_\\-]+)",
                        "@$1"
                );
    }

    private List<String> missingVariables(

            Path frameworkRoot,
            String tagExpression,
            Map<String, String> variables

    ) {

        List<String> required =
                requiredVariables(
                        frameworkRoot,
                        tagExpression
                );

        if (
                required.isEmpty()
        ) {

            return List.of();
        }

        Map<String, String> normalizedVariables =
                new LinkedHashMap<>();

        if (
                variables != null
        ) {

            for (
                    Map.Entry<String, String> entry
                    : variables.entrySet()
            ) {

                if (
                        entry.getKey() != null
                ) {

                    normalizedVariables.put(
                            entry.getKey()
                                    .toLowerCase(),
                            entry.getValue()
                    );
                }
            }
        }

        List<String> missing =
                new ArrayList<>();

        for (
                String key
                : required
        ) {

            String value =
                    normalizedVariables.get(
                            key.toLowerCase()
                    );

            if (
                    value == null
                            ||
                            value.isBlank()
            ) {

                missing.add(key);
            }
        }

        return missing;
    }

    private List<String> requiredVariables(

            Path frameworkRoot,
            String tagExpression

    ) {

        List<String> tagFilters =
                extractTags(tagExpression);

        List<String> required =
                new ArrayList<>();

        Path featureRoot =
                frameworkRoot.resolve(
                        "src/test/resources/features"
                );

        Pattern placeholder =
                Pattern.compile(
                        "\\$\\{([A-Za-z0-9_]+)}"
                );

        try (
                Stream<Path> paths =
                        Files.walk(featureRoot)
        ) {

            List<Path> featureFiles =
                    paths.filter(Files::isRegularFile)
                            .filter(path -> path.getFileName()
                                    .toString()
                                    .endsWith(".feature"))
                            .toList();

            for (
                    Path featureFile
                    : featureFiles
            ) {

                String content =
                        Files.readString(featureFile);

                if (
                        !tagFilters.isEmpty()
                                &&
                                tagFilters.stream()
                                        .noneMatch(content::contains)
                ) {

                    continue;
                }

                Matcher matcher =
                        placeholder.matcher(content);

                while (
                        matcher.find()
                ) {

                    String key =
                            matcher.group(1);

                    if (
                            !required.contains(key)
                    ) {

                        required.add(key);
                    }
                }
            }

        } catch (IOException ignored) {
        }

        return required;
    }

    private List<RuntimeVariableContext> runtimeVariableContexts(

            Path frameworkRoot,
            String tagExpression,
            List<String> missingVariables

    ) {

        List<String> normalizedMissing =
                missingVariables.stream()
                        .map(String::toLowerCase)
                        .toList();

        List<RuntimeVariableContext> contexts =
                new ArrayList<>();

        Path featureRoot =
                frameworkRoot.resolve(
                        "src/test/resources/features"
                );

        Pattern placeholder =
                Pattern.compile(
                        "\\$\\{([A-Za-z0-9_]+)}"
                );

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

                collectRuntimeVariableContexts(
                        featureFile,
                        tagExpression,
                        normalizedMissing,
                        placeholder,
                        contexts
                );
            }

        } catch (IOException ignored) {
        }

        return contexts;
    }

    private void collectRuntimeVariableContexts(

            Path featureFile,
            String tagExpression,
            List<String> normalizedMissing,
            Pattern placeholder,
            List<RuntimeVariableContext> contexts

    ) throws IOException {

        List<String> lines =
                Files.readAllLines(featureFile);

        String featureName =
                featureFile.getFileName()
                        .toString();

        List<String> featureTags =
                new ArrayList<>();

        List<String> pendingTags =
                new ArrayList<>();

        String scenarioName =
                null;

        boolean scenarioMatches =
                false;

        for (
                String line
                : lines
        ) {

            String trimmed =
                    line.trim();

            if (
                    trimmed.startsWith("@")
            ) {

                pendingTags.addAll(
                        List.of(
                                trimmed.split("\\s+")
                        )
                );

                continue;
            }

            if (
                    trimmed.startsWith("Feature:")
            ) {

                featureName =
                        trimmed.substring(
                                "Feature:".length()
                        )
                                .trim();

                featureTags.addAll(pendingTags);
                pendingTags.clear();

                continue;
            }

            if (
                    trimmed.startsWith("Scenario:")
                            ||
                            trimmed.startsWith("Scenario Outline:")
            ) {

                scenarioName =
                        trimmed.substring(
                                trimmed.indexOf(':') + 1
                        )
                                .trim();

                List<String> scenarioTags =
                        new ArrayList<>(featureTags);

                scenarioTags.addAll(pendingTags);

                scenarioMatches =
                        scenarioMatchesTagExpression(
                                scenarioTags,
                                tagExpression
                        );

                pendingTags.clear();

                continue;
            }

            if (
                    !scenarioMatches
                            ||
                            scenarioName == null
            ) {

                continue;
            }

            Matcher matcher =
                    placeholder.matcher(trimmed);

            while (
                    matcher.find()
            ) {

                String variable =
                        matcher.group(1);

                if (
                        !normalizedMissing.contains(
                                variable.toLowerCase()
                        )
                ) {

                    continue;
                }

                RuntimeVariableContext context =
                        RuntimeVariableContext.builder()
                                .variable(variable)
                                .feature(featureName)
                                .scenario(scenarioName)
                                .step(trimmed)
                                .hint(
                                        runtimeVariableHint(
                                                variable,
                                                trimmed
                                        )
                                )
                                .build();

                if (
                        contexts.stream()
                                .noneMatch(existing ->
                                        sameRuntimeContext(
                                                existing,
                                                context
                                        )
                                )
                ) {

                    contexts.add(context);
                }
            }
        }
    }

    private boolean sameRuntimeContext(
            RuntimeVariableContext left,
            RuntimeVariableContext right
    ) {

        return left.getVariable()
                .equalsIgnoreCase(
                        right.getVariable()
                )
                &&
                left.getScenario()
                        .equals(right.getScenario())
                &&
                left.getStep()
                        .equals(right.getStep());
    }

    private String runtimeVariableHint(
            String variable,
            String step
    ) {

        String lower =
                variable == null
                        ? ""
                        : variable.toLowerCase();

        if (
                "search".equals(lower)
        ) {

            return "Value used by this generated search/input step.";
        }

        if (
                "from".equals(lower)
                        ||
                        "origin".equals(lower)
        ) {

            return "Departure or origin value for this scenario.";
        }

        if (
                "to".equals(lower)
                        ||
                        "destination".equals(lower)
        ) {

            return "Arrival or destination value for this scenario.";
        }

        if (
                step != null
                        &&
                        step.toLowerCase()
                                .contains(" into ")
        ) {

            return "Value typed by this step.";
        }

        return "Runtime value required by this scenario.";
    }

    private void requireMatchingScenarios(
            Path frameworkRoot,
            String tagExpression
    ) {

        if (
                hasMatchingScenario(
                        frameworkRoot,
                        tagExpression
                )
        ) {

            return;
        }

        throw new RuntimeException(
                noMatchingGeneratedTestsMessage(tagExpression)
        );
    }

    private boolean hasMatchingScenario(
            Path frameworkRoot,
            String tagExpression
    ) {

        List<String> tagFilters =
                extractTags(tagExpression);

        if (
                !tagFilters.isEmpty()
        ) {

            return tagExpressionCanMatchCatalog(
                    parseTags(frameworkRoot),
                    tagExpression
            );
        }

        Path featureRoot =
                frameworkRoot.resolve(
                        "src/test/resources/features"
                );

        try (
                Stream<Path> paths =
                        Files.walk(featureRoot)
        ) {

            List<Path> featureFiles =
                    paths.filter(Files::isRegularFile)
                            .filter(path -> path.getFileName()
                                    .toString()
                                    .endsWith(".feature"))
                            .toList();

            for (
                    Path featureFile
                    : featureFiles
            ) {

                if (
                        hasMatchingScenarioInFile(
                                featureFile,
                                tagExpression
                        )
                ) {

                    return true;
                }
            }

        } catch (IOException ignored) {
        }

        return false;
    }

    private boolean tagExpressionCanMatchCatalog(
            List<GeneratedTestTag> tags,
            String tagExpression
    ) {

        if (
                tags == null
                        ||
                        tags.isEmpty()
        ) {

            return false;
        }

        List<String> availableTags =
                tags.stream()
                        .map(GeneratedTestTag::getTag)
                        .filter(tag -> tag != null && !tag.isBlank())
                        .map(String::toLowerCase)
                        .toList();

        if (
                availableTags.isEmpty()
        ) {

            return false;
        }

        String[] andClauses =
                tagExpression.split(
                        "(?i)\\s+and\\s+"
                );

        for (
                String clause
                : andClauses
        ) {

            if (
                    clause.toLowerCase()
                            .contains("not ")
            ) {

                continue;
            }

            List<String> clauseTags =
                    extractTags(clause);

            if (
                    clauseTags.isEmpty()
            ) {

                continue;
            }

            boolean clauseCanMatch =
                    clauseTags.stream()
                            .map(String::toLowerCase)
                            .anyMatch(availableTags::contains);

            if (
                    !clauseCanMatch
            ) {

                return false;
            }
        }

        return true;
    }

    private boolean hasMatchingScenarioInFile(
            Path featureFile,
            String tagExpression
    ) throws IOException {

        List<String> lines =
                Files.readAllLines(featureFile);

        List<String> featureTags =
                new ArrayList<>();

        List<String> pendingTags =
                new ArrayList<>();

        for (
                String line
                : lines
        ) {

            String trimmed =
                    line.trim();

            if (
                    trimmed.startsWith("@")
            ) {

                pendingTags.addAll(
                        List.of(
                                trimmed.split("\\s+")
                        )
                );

                continue;
            }

            if (
                    trimmed.startsWith("Feature:")
            ) {

                featureTags.addAll(pendingTags);
                pendingTags.clear();
                continue;
            }

            if (
                    trimmed.startsWith("Scenario:")
                            ||
                            trimmed.startsWith("Scenario Outline:")
            ) {

                List<String> scenarioTags =
                        new ArrayList<>(featureTags);

                scenarioTags.addAll(pendingTags);

                if (
                        scenarioMatchesTagExpression(
                                scenarioTags,
                                tagExpression
                        )
                ) {

                    return true;
                }

                pendingTags.clear();
            }
        }

        return false;
    }

    private boolean scenarioMatchesTagExpression(
            List<String> scenarioTags,
            String tagExpression
    ) {

        List<String> tagFilters =
                extractTags(tagExpression);

        if (
                tagFilters.isEmpty()
        ) {

            return true;
        }

        List<String> normalizedScenarioTags =
                scenarioTags.stream()
                        .map(String::toLowerCase)
                        .toList();

        String[] andClauses =
                tagExpression.split(
                        "(?i)\\s+and\\s+"
                );

        for (
                String clause
                : andClauses
        ) {

            List<String> clauseTags =
                    extractTags(clause);

            if (
                    clauseTags.isEmpty()
            ) {

                continue;
            }

            boolean clauseMatched =
                    clauseTags.stream()
                            .map(String::toLowerCase)
                            .anyMatch(normalizedScenarioTags::contains);

            if (
                    !clauseMatched
            ) {

                return false;
            }
        }

        return true;
    }

    private String noMatchingGeneratedTestsMessage(
            String tagExpression
    ) {

        if (
                tagExpression == null
                        ||
                        tagExpression.isBlank()
        ) {

            return "No generated Cucumber scenarios were found in this framework. Generate tests first, then run them again.";
        }

        return "No generated Cucumber scenarios match tag filter `"
                + tagExpression
                + "`. Ask for the generated test tags, then run one of the listed tags.";
    }

    private List<String> extractTags(
            String tagExpression
    ) {

        if (
                tagExpression == null
                        ||
                        tagExpression.isBlank()
        ) {

            return List.of();
        }

        List<String> tags =
                new ArrayList<>();

        Matcher matcher =
                Pattern.compile(
                        "@[A-Za-z0-9_\\-]+"
                )
                        .matcher(tagExpression);

        while (
                matcher.find()
        ) {

            tags.add(
                    matcher.group()
            );
        }

        return tags;
    }

    private String publishCucumberReport(

            Path frameworkRoot,

            String output

    ) throws IOException {

        Path cucumberReport =
                frameworkRoot.resolve(
                        "target/cucumber-report.html"
                );

        Files.createDirectories(
                Paths.get("reports")
        );

        String fileName =
                "generated-tests-"
                        + LocalDateTime.now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyyMMdd-HHmmss"
                                )
                        )
                        + ".html";

        Path copiedReport =
                Paths.get("reports")
                        .resolve(fileName);

        String reportHtml;

        if (
                isCompleteCucumberReport(cucumberReport)
        ) {

            reportHtml =
                    Files.readString(
                            cucumberReport
                    );

        } else {

            reportHtml =
                    fallbackReportHtml(
                            frameworkRoot,
                            output
                    );
        }

        Files.writeString(
                copiedReport,
                reportHtml
        );

        reportArtifactService.saveHtmlReport(
                fileName,
                reportHtml
        );

        return "/api/reports/"
                + fileName;
    }

    private boolean isCompleteCucumberReport(
            Path cucumberReport
    ) {

        if (
                !Files.exists(cucumberReport)
        ) {

            return false;
        }

        try {

            String content =
                    Files.readString(cucumberReport);

            boolean hasFinishedEvent =
                    content.contains("testRunFinished")
                            ||
                            content.contains("\"testRunFinished\"");

            boolean hasExecutedScenario =
                    content.contains("testCaseStarted")
                            ||
                            content.contains("\"testCaseStarted\"")
                            ||
                            content.contains("testCaseFinished")
                            ||
                            content.contains("\"testCaseFinished\"");

            return hasFinishedEvent
                    &&
                    hasExecutedScenario;

        } catch (IOException e) {

            return false;
        }
    }

    private String fallbackReportHtml(

            Path frameworkRoot,

            String output

    ) {

        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <title>Generated Cucumber Test Report</title>
                  <style>
                    body { font-family: Arial, sans-serif; margin: 0; color: #202124; background: #f8f9fb; }
                    header { background: #1f2937; color: white; padding: 24px 32px; }
                    main { padding: 24px 32px; }
                    section { background: white; border: 1px solid #dfe3ea; border-radius: 6px; margin-bottom: 18px; padding: 18px; }
                    h1, h2 { margin: 0 0 12px; }
                    code, pre { font-family: Menlo, Consolas, monospace; }
                    pre { white-space: pre-wrap; background: #111827; color: #f9fafb; padding: 16px; border-radius: 6px; overflow-x: auto; }
                    .muted { color: #5f6b7a; }
                  </style>
                </head>
                <body>
                  <header>
                    <h1>Generated Cucumber Test Report</h1>
                    <div>Cucumber did not produce <code>target/cucumber-report.html</code>, so AIF preserved the Maven/Cucumber output for diagnosis.</div>
                  </header>
                  <main>
                    <section>
                      <h2>Framework</h2>
                      <div class="muted">%s</div>
                    </section>
                    <section>
                      <h2>Execution Output</h2>
                      <pre>%s</pre>
                    </section>
                  </main>
                </body>
                </html>
                """.formatted(
                escapeHtml(
                        frameworkRoot.toAbsolutePath()
                                .normalize()
                                .toString()
                ),
                escapeHtml(
                        output == null
                                ? ""
                                : output
                )
        );
    }

    private String escapeHtml(
            String value
    ) {

        if (
                value == null
        ) {

            return "";
        }

        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private CommandResult installPlaywrightBrowsers(
            Path frameworkRoot
    ) throws Exception {

        List<String> command =
                new ArrayList<>();

        command.add(
                mavenCommand()
        );

        command.add("-B");

        command.add("-ntp");

        command.add("-f");

        command.add(
                frameworkRoot.resolve("pom.xml")
                        .toAbsolutePath()
                        .normalize()
                        .toString()
        );

        command.add("exec:java");

        command.add(
                "-Dexec.mainClass=com.microsoft.playwright.CLI"
        );

        command.add(
                "-Dexec.args=install chromium"
        );

        return runCommand(
                command,
                frameworkRoot,
                Map.of(),
                true
        );
    }

    private CommandResult runCommand(

            List<String> command,

            Path workingDirectory,

            Map<String, String> variables,

            boolean allowBrowserDownload

    ) throws Exception {

        ProcessBuilder processBuilder =
                new ProcessBuilder(command);

        processBuilder.directory(
                workingDirectory.toFile()
        );

        processBuilder.redirectErrorStream(true);

        applyVariables(
                processBuilder,
                variables,
                allowBrowserDownload
        );

        Process process =
                processBuilder.start();

        long timeoutMinutes =
                executionTimeoutMinutes();

        long startedAt =
                System.nanoTime();

        String commandSummary =
                commandSummary(command);

        log.info(
                "Starting generated test command: {} (timeout {} minute(s))",
                commandSummary,
                timeoutMinutes
        );

        CompletableFuture<String> outputFuture =
                CompletableFuture.supplyAsync(
                        () -> readOutput(
                                process,
                                commandSummary
                        )
                );

        boolean completed =
                process.waitFor(
                        timeoutMinutes,
                        TimeUnit.MINUTES
                );

        if (
                !completed
        ) {

            process.destroyForcibly();

            outputFuture.cancel(true);

            log.warn(
                    "Generated test command timed out after {} minute(s): {}",
                    timeoutMinutes,
                    commandSummary
            );

            throw new RuntimeException(
                    "Generated test execution timed out after "
                            + timeoutMinutes
                            + " minutes."
            );
        }

        String output =
                outputFuture.get(
                        5,
                        TimeUnit.SECONDS
                );

        long durationSeconds =
                TimeUnit.NANOSECONDS.toSeconds(
                        System.nanoTime()
                                - startedAt
                );

        log.info(
                "Generated test command finished with exit code {} in {} second(s): {}",
                process.exitValue(),
                durationSeconds,
                commandSummary
        );

        return new CommandResult(
                process.exitValue(),
                output
        );
    }

    private long executionTimeoutMinutes() {

        if (
                generatedTestTimeoutMinutes <= 0
        ) {

            return DEFAULT_TEST_TIMEOUT_MINUTES;
        }

        return generatedTestTimeoutMinutes;
    }

    private String commandSummary(
            List<String> command
    ) {

        return String.join(
                " ",
                command
        );
    }

    private List<String> generatedTestCommand(

            Path frameworkRoot,
            String normalizedExpression,
            boolean offline

    ) {

        List<String> command =
                new ArrayList<>();

        command.add(
                mavenCommand()
        );

        command.add("-B");

        command.add("-ntp");

        if (
                offline
        ) {

            command.add("-o");
        }

        command.add("-Dstyle.color=never");

        command.add("-f");

        command.add(
                frameworkRoot.resolve("pom.xml")
                        .toAbsolutePath()
                        .normalize()
                        .toString()
        );

        command.add("test");

        if (
                normalizedExpression != null
                        &&
                        !normalizedExpression.isBlank()
        ) {

            command.add(
                    "-Dcucumber.filter.tags="
                            + normalizedExpression
            );
        }

        return command;
    }

    private boolean shouldUseOfflineMaven() {

        return generatedTestMavenOffline;
    }

    private String generatedTestMavenOpts() {

        if (
                generatedTestMavenOpts == null
                        ||
                        generatedTestMavenOpts.isBlank()
        ) {

            return "-Xmx192m -XX:MaxMetaspaceSize=128m -XX:+ExitOnOutOfMemoryError -Djava.awt.headless=true";
        }

        return generatedTestMavenOpts.trim();
    }

    private boolean isMavenOfflineDependencyFailure(
            String output
    ) {

        if (
                output == null
        ) {

            return false;
        }

        String lower =
                output.toLowerCase();

        return lower.contains("cannot access central")
                ||
                lower.contains("offline mode")
                ||
                lower.contains("has not been downloaded from it before")
                ||
                lower.contains("missing artifact");
    }

    private String combineOfflineRetryOutputs(

            String offlineOutput,
            String onlineOutput

    ) {

        return (offlineOutput == null ? "" : offlineOutput)
                + System.lineSeparator()
                + "AIF retried generated test execution without Maven offline mode because a dependency was missing from the local image cache."
                + System.lineSeparator()
                + (onlineOutput == null ? "" : onlineOutput);
    }

    private boolean isMissingPlaywrightBrowser(
            String output
    ) {

        if (
                output == null
        ) {

            return false;
        }

        String lower =
                output.toLowerCase();

        return lower.contains("playwright")
                &&
                (
                        lower.contains("executable doesn't exist")
                                ||
                                lower.contains("please run the following command to download new browsers")
                                ||
                                lower.contains("looks like playwright was just installed or updated")
                );
    }

    private String combineOutputs(

            String firstRunOutput,

            String installOutput,

            String retryOutput

    ) {

        StringBuilder combined =
                new StringBuilder();

        combined.append(
                firstRunOutput == null
                        ? ""
                        : firstRunOutput
        );

        combined.append(
                System.lineSeparator()
        );

        combined.append(
                "AIF detected a missing Playwright browser and ran `mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args=\"install chromium\"`."
        );

        combined.append(
                System.lineSeparator()
        );

        if (
                installOutput != null
                        &&
                        !installOutput.isBlank()
        ) {

            combined.append(installOutput);
        }

        if (
                retryOutput != null
        ) {

            combined.append(
                    System.lineSeparator()
            );

            combined.append(
                    "AIF retried the generated test execution after browser installation."
            );

            combined.append(
                    System.lineSeparator()
            );

            combined.append(retryOutput);
        }

        return combined.toString();
    }

    private void applyVariables(

            ProcessBuilder processBuilder,
            Map<String, String> variables

    ) {

        applyVariables(
                processBuilder,
                variables,
                false
        );
    }

    private void applyVariables(

            ProcessBuilder processBuilder,
            Map<String, String> variables,
            boolean allowBrowserDownload

    ) {

        Map<String, String> environment =
                processBuilder.environment();

        environment.putIfAbsent(
                "MAVEN_OPTS",
                generatedTestMavenOpts()
        );

        environment.putIfAbsent(
                "AIF_HEADLESS",
                defaultHeadless()
                        ? "true"
                        : "false"
        );

        environment.putIfAbsent(
                "AIF_BROWSER_LAUNCH_TIMEOUT_MS",
                "60000"
        );

        environment.putIfAbsent(
                "AIF_BROWSER_LAUNCH_RETRY_TIMEOUT_MS",
                "90000"
        );

        environment.putIfAbsent(
                "AIF_STEP_TIMEOUT_MS",
                "8000"
        );

        environment.putIfAbsent(
                "AIF_NAVIGATION_TIMEOUT_MS",
                "15000"
        );

        environment.putIfAbsent(
                "NO_AT_BRIDGE",
                "1"
        );

        if (
                allowBrowserDownload
        ) {

            environment.remove(
                    "PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD"
            );

        } else if (
                defaultHeadless()
        ) {

            environment.putIfAbsent(
                    "PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD",
                    "1"
            );
        }

        if (
                variables == null
                        ||
                        variables.isEmpty()
        ) {

            return;
        }

        for (
                Map.Entry<String, String> entry
                : variables.entrySet()
        ) {

            if (
                    entry.getKey() == null
                            ||
                            entry.getValue() == null
            ) {

                continue;
            }

            environment.put(
                    environmentKey(entry.getKey()),
                    entry.getValue()
            );
        }
    }

    private record CommandResult(
            int exitCode,
            String output
    ) {
    }

    private boolean defaultHeadless() {

        String configured =
                normalizeEnv(
                        System.getenv("AIF_HEADLESS")
                );

        if (
                configured != null
        ) {

            return Boolean.parseBoolean(configured);
        }

        String osName =
                System.getProperty("os.name", "")
                        .toLowerCase();

        return osName.contains("linux")
                &&
                (
                        normalizeEnv(System.getenv("DISPLAY")) == null
                                ||
                                isTruthy(System.getenv("RENDER"))
                                ||
                                isTruthy(System.getenv("CI"))
                );
    }

    private boolean isTruthy(
            String value
    ) {

        String normalized =
                normalizeEnv(value);

        return normalized != null
                &&
                !"false".equalsIgnoreCase(normalized)
                &&
                !"0".equals(normalized);
    }

    private String normalizeEnv(
            String value
    ) {

        if (
                value == null
                        ||
                        value.isBlank()
        ) {

            return null;
        }

        return value.trim();
    }

    private String environmentKey(
            String key
    ) {

        return key.toUpperCase()
                .replaceAll(
                        "[^A-Z0-9]+",
                        "_"
                );
    }

    private String readOutput(
            Process process,
            String commandSummary
    ) {

        StringBuilder output =
                new StringBuilder();

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        process.getInputStream()
                                )
                        )
        ) {

            String line;

            while (
                    (line = reader.readLine()) != null
            ) {

                log.debug(
                        "[generated-test-command] {}",
                        line
                );

                appendBoundedOutput(
                        output,
                        line
                );
            }

        } catch (IOException e) {

            log.warn(
                    "Unable to read generated test output for {}",
                    commandSummary,
                    e
            );

            appendBoundedOutput(
                    output,
                    "Unable to read generated test output: "
                            + e.getMessage()
            );
        }

        return output.toString();
    }

    private void appendBoundedOutput(

            StringBuilder output,

            String line

    ) {

        output.append(line)
                .append(System.lineSeparator());

        if (
                output.length() > MAX_COMMAND_OUTPUT_CHARS
        ) {

            output.delete(
                    0,
                    output.length()
                            - MAX_COMMAND_OUTPUT_CHARS
            );
        }
    }

    private String tail(

            String value,
            int maxLength

    ) {

        if (
                value == null
                        ||
                        value.length() <= maxLength
        ) {

            return value;
        }

        return value.substring(
                value.length() - maxLength
        );
    }

    private String mavenCommand() {

        String os =
                System.getProperty(
                        "os.name",
                        ""
                )
                        .toLowerCase();

        Path wrapper =
                Paths.get(
                        os.contains("win")
                                ? "mvnw.cmd"
                                : "mvnw"
                )
                        .toAbsolutePath()
                        .normalize();

        if (
                Files.exists(wrapper)
        ) {

            return wrapper.toString();
        }

        return os.contains("win")
                ? "mvn.cmd"
                : "mvn";
    }

    private String descriptionFor(
            GeneratedTestTagBuilder builder
    ) {

        String tag =
                builder.tag;

        if (
                "@generated".equalsIgnoreCase(tag)
        ) {

            return "Runs every test that AIF generated into the framework.";
        }

        if (
                "@ai_requirement".equalsIgnoreCase(tag)
        ) {

            return "Runs tests derived from plain-English requirements by the AI requirement agent.";
        }

        if (
                tag.toLowerCase()
                        .startsWith("@flow_")
        ) {

            return "Runs the detected application flow scenarios: "
                    + String.join(
                    ", ",
                    builder.scenarios
            )
                    + ".";
        }

        return "Runs: "
                + String.join(
                ", ",
                builder.scenarios
        )
                + ".";
    }

    public List<String> missingRuntimeVariables(

            String sessionId,
            String tagExpression,
            Map<String, String> variables

    ) {

        Path frameworkRoot =
                resolveFrameworkRoot(sessionId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        missingFrameworkMessage()
                                )
                        );

        String normalizedExpression =
                normalizeTagExpression(tagExpression);

        normalizeFeatureFiles(frameworkRoot);

        requireMatchingScenarios(
                frameworkRoot,
                normalizedExpression
        );

        return missingVariables(
                frameworkRoot,
                normalizedExpression,
                variables
        );
    }

    public List<RuntimeVariableContext> missingRuntimeVariableContexts(

            String sessionId,
            String tagExpression,
            Map<String, String> variables

    ) {

        Path frameworkRoot =
                resolveFrameworkRoot(sessionId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        missingFrameworkMessage()
                                )
                        );

        String normalizedExpression =
                normalizeTagExpression(tagExpression);

        normalizeFeatureFiles(frameworkRoot);

        requireMatchingScenarios(
                frameworkRoot,
                normalizedExpression
        );

        List<String> missingVariables =
                missingVariables(
                        frameworkRoot,
                        normalizedExpression,
                        variables
                );

        if (
                missingVariables.isEmpty()
        ) {

            return List.of();
        }

        List<RuntimeVariableContext> contexts =
                runtimeVariableContexts(
                frameworkRoot,
                normalizedExpression,
                missingVariables
        );

        if (
                !contexts.isEmpty()
        ) {

            return contexts;
        }

        return missingVariables.stream()
                .map(variable ->
                        RuntimeVariableContext.builder()
                                .variable(variable)
                                .hint("Runtime value required by matching generated scenarios.")
                                .build()
                )
                .toList();
    }

    public String missingRuntimeDataMessage(
            List<String> missingVariables
    ) {

        return "Missing runtime data for generated tests: "
                + String.join(
                ", ",
                missingVariables
        )
                + ". Provide it in chat first, for example: username is standard_user and password is secret_sauce.";
    }

    private class GeneratedTestTagBuilder {

        private final String tag;

        private final List<String> scenarios =
                new ArrayList<>();

        private final List<String> features =
                new ArrayList<>();

        private GeneratedTestTagBuilder(
                String tag
        ) {

            this.tag = tag;
        }

        private void addScenario(
                String scenario
        ) {

            if (
                    scenario != null
                            &&
                            !scenario.isBlank()
                            &&
                            !scenarios.contains(scenario)
            ) {

                scenarios.add(scenario);
            }
        }

        private void addFeature(
                String feature
        ) {

            if (
                    feature != null
                            &&
                            !feature.isBlank()
                            &&
                            !features.contains(feature)
            ) {

                features.add(feature);
            }
        }

        private GeneratedTestTag build() {

            return GeneratedTestTag.builder()
                    .tag(tag)
                    .description(
                            descriptionFor(this)
                    )
                    .scenarios(scenarios)
                    .features(features)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class GeneratedTestCatalog {

        private String frameworkRoot;

        private List<GeneratedTestTag> tags;

        private String message;
    }

    @Getter
    @Builder
    public static class GeneratedTestTag {

        private String tag;

        private String description;

        private List<String> scenarios;

        private List<String> features;
    }

    @Getter
    @Builder
    public static class RuntimeVariableContext {

        private String variable;

        private String feature;

        private String scenario;

        private String step;

        private String hint;
    }

    @Getter
    @Builder
    public static class GeneratedTestRunResult {

        private boolean success;

        private String tagExpression;

        private String reportUrl;

        private int exitCode;

        private String output;

        private String message;
    }

    @Getter
    @Builder
    public static class GeneratedTestRepairResult {

        private boolean changed;

        private String frameworkRoot;

        private List<String> changedFiles;

        private List<String> changes;

        private String failureSummary;

        private List<String> failureDetails;

        private String repairSource;

        private String fallbackReason;

        private boolean learned;

        private String message;
    }
}
