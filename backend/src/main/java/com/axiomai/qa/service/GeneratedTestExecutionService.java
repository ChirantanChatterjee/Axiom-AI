package com.axiomai.qa.service;

import com.axiomai.qa.generator.flow.FlowPageObjectGenerator;
import com.axiomai.qa.generator.flow.FlowStepDefinitionGenerator;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @Value("${aif.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    @Value("${aif.generated-tests.timeout-minutes:${AIF_GENERATED_TEST_TIMEOUT_MINUTES:6}}")
    private long generatedTestTimeoutMinutes;

    private final GeneratedProjectWriterService
            generatedProjectWriterService;

    private final GeneratedFrameworkPersistenceService
            generatedFrameworkPersistenceService;

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

        normalizeFeatureFiles(frameworkRoot);

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
                    "Missing runtime data for generated tests: "
                            + String.join(
                            ", ",
                            missingVariables
                    )
                            + ". Provide it in chat first, for example: username is standard_user and password is secret_sauce."
            );
        }

        List<String> command =
                new ArrayList<>();

        command.add(
                mavenCommand()
        );

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

        try {

            if (
                    !frameworkLearningService
                            .hasUserUploadedFramework(sessionId)
            ) {

                refreshSupportFiles(frameworkRoot);
            }

            generatedFrameworkPersistenceService
                    .persistFramework(sessionId);

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
                    publishCucumberReport(
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

    public GeneratedTestRepairResult repairLatestFailure(
            String sessionId
    ) {

        Path frameworkRoot =
                resolveFrameworkRoot(sessionId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        missingFrameworkMessage()
                                )
                        );

        try {

            GeneratedFeatureRepairService.RepairResult repair =
                    generatedFeatureRepairService.repair(frameworkRoot);

            boolean learned =
                    frameworkLearningService
                            .recordRuntimeRepairLearning(
                                    sessionId,
                                    repair.getFailureSummary(),
                                    repair.getChanges()
                            );

            if (
                    !frameworkLearningService
                            .hasUserUploadedFramework(sessionId)
            ) {

                refreshSupportFiles(frameworkRoot);
            }

            generatedFrameworkPersistenceService
                    .persistFramework(sessionId);

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

    private void refreshSupportFiles(
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

        Files.writeString(
                pageFolder.resolve("GeneratedPage.java"),
                flowPageObjectGenerator.generate(List.of())
        );

        Files.writeString(
                stepFolder.resolve("GeneratedSteps.java"),
                FlowStepDefinitionGenerator.generate(List.of())
        );

        Files.writeString(
                hooksFolder.resolve("Hooks.java"),
                hookGeneratorService.generateHooks()
        );

        Files.writeString(
                runnerFolder.resolve("TestRunner.java"),
                runnerGeneratorService.generateRunner()
        );

        Files.writeString(
                frameworkRoot.resolve("pom.xml"),
                pomGeneratorService.generatePom()
        );
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

    private void normalizeFeatureFiles(
            Path frameworkRoot
    ) {

        Path featureRoot =
                frameworkRoot.resolve(
                        "src/test/resources/features"
                );

        if (
                !Files.exists(featureRoot)
        ) {

            return;
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
                            .toList();

            for (
                    Path featureFile
                    : featureFiles
            ) {

                String content =
                        Files.readString(featureFile);

                String normalized =
                        content.replaceAll(
                                "(?i)\\bYYYY\\b",
                                String.valueOf(
                                        Year.now()
                                                .getValue()
                                )
                        );

                if (
                        !content.equals(normalized)
                ) {

                    Files.writeString(
                            featureFile,
                            normalized
                    );
                }
            }

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to normalize generated feature files.",
                    e
            );
        }
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

        if (
                isCompleteCucumberReport(cucumberReport)
        ) {

            Files.copy(
                    cucumberReport,
                    copiedReport,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

        } else {

            Files.writeString(
                    copiedReport,
                    fallbackReportHtml(
                            frameworkRoot,
                            output
                    )
            );
        }

        return publicBaseUrl
                + "/api/reports/"
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

            return content.contains("testRunFinished")
                    ||
                    content.contains("\"testRunFinished\"");

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
                "-Xmx384m -XX:MaxMetaspaceSize=192m -Djava.awt.headless=true"
        );

        environment.putIfAbsent(
                "AIF_HEADLESS",
                defaultHeadless()
                        ? "true"
                        : "false"
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

        private boolean learned;

        private String message;
    }
}
