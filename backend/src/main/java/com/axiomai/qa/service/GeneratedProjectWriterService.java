package com.axiomai.qa.service;

import com.axiomai.qa.models.GeneratedFramework;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class GeneratedProjectWriterService {

    private final HookGeneratorService
            hookGeneratorService;

    private final RunnerGeneratorService
            runnerGeneratorService;

    private final PomGeneratorService
            pomGeneratorService;

    // =====================================================
    // BACKWARD COMPATIBLE WRITE METHOD
    // =====================================================

    public String writeFramework(
            GeneratedFramework framework
    ) {

        return writeFramework(
                framework,
                "default-session",
                "generated"
        );
    }

    // =====================================================
    // SESSION-AWARE WRITE METHOD
    // =====================================================

    public String writeFramework(

            GeneratedFramework framework,

            String sessionId,

            String featureName

    ) {

        try {

            Path root =
                    getFrameworkRoot(sessionId);

            Path featureFolder =
                    root.resolve(
                            "src/test/resources/features"
                    );

            Path stepFolder =
                    root.resolve(
                            "src/test/java/com/axiomai/generated/steps"
                    );

            Path pageFolder =
                    root.resolve(
                            "src/test/java/com/axiomai/generated/pages"
                    );

            Path hooksFolder =
                    root.resolve(
                            "src/test/java/com/axiomai/generated/hooks"
                    );

            Path runnerFolder =
                    root.resolve(
                            "src/test/java/com/axiomai/generated/runner"
                    );

            Files.createDirectories(featureFolder);
            Files.createDirectories(stepFolder);
            Files.createDirectories(pageFolder);
            Files.createDirectories(hooksFolder);
            Files.createDirectories(runnerFolder);

            writeGeneratedFeatureFile(
                    featureFolder.resolve(
                            sanitizeFeatureName(featureName)
                                    + ".feature"
                    ),
                    framework.getFeatureFile()
            );

            Files.writeString(
                    pageFolder.resolve(
                            "GeneratedPage.java"
                    ),
                    framework.getPageObject()
            );

            Files.writeString(
                    stepFolder.resolve(
                            "GeneratedSteps.java"
                    ),
                    framework.getStepDefinition()
            );

            Files.writeString(
                    hooksFolder.resolve(
                            "Hooks.java"
                    ),
                    hookGeneratorService.generateHooks()
            );

            Files.writeString(
                    runnerFolder.resolve(
                            "TestRunner.java"
                    ),
                    runnerGeneratorService.generateRunner()
            );

            Files.writeString(
                    root.resolve("pom.xml"),
                    pomGeneratorService.generatePom()
            );

            Files.writeString(
                    root.resolve("README.md"),
                    generateReadme()
            );

            return root.toAbsolutePath()
                    .normalize()
                    .toString();

        } catch (IOException e) {

            throw new RuntimeException(
                    "Framework generation failed.",
                    e
            );
        }
    }

    // =====================================================
    // WRITE SINGLE FEATURE
    // =====================================================

    public String writeFeatureFile(

            String sessionId,

            String featureName,

            String content

    ) {

        try {

            Path featureFolder =
                    getFrameworkRoot(sessionId)
                            .resolve(
                                    "src/test/resources/features"
                            );

            Files.createDirectories(featureFolder);

            Path featureFile =
                    featureFolder.resolve(
                            sanitizeFeatureName(featureName)
                                    + ".feature"
                    );

            writeGeneratedFeatureFile(
                    featureFile,
                    content
            );

            return featureFile.toAbsolutePath()
                    .normalize()
                    .toString();

        } catch (IOException e) {

            throw new RuntimeException(
                    "Feature generation failed.",
                    e
            );
        }
    }

    // =====================================================
    // ZIP FRAMEWORK
    // =====================================================

    public String zipFramework(
            String sessionId
    ) {

        try {

            Path root =
                    getFrameworkRoot(sessionId);

            if (
                    !Files.exists(root)
            ) {

                throw new RuntimeException(
                        "No generated framework exists for this session."
                );
            }

            Path workspaceRoot =
                    getWorkspaceRoot(sessionId);

            Files.createDirectories(workspaceRoot);

            Path zipPath =
                    workspaceRoot.resolve(
                            "framework.zip"
                    );

            Files.deleteIfExists(zipPath);

            try (
                    OutputStream outputStream =
                            Files.newOutputStream(zipPath);

                    ZipOutputStream zipOutputStream =
                            new ZipOutputStream(outputStream)
            ) {

                try (
                        Stream<Path> paths =
                                Files.walk(root)
                ) {

                    paths.sorted(
                                    Comparator.naturalOrder()
                            )
                            .filter(Files::isRegularFile)
                            .forEach(path -> addZipEntry(
                                    root,
                                    path,
                                    zipOutputStream
                            ));
                }
            }

            return zipPath.toAbsolutePath()
                    .normalize()
                    .toString();

        } catch (IOException e) {

            throw new RuntimeException(
                    "Framework zip generation failed.",
                    e
            );
        }
    }

    private void addZipEntry(

            Path root,

            Path path,

            ZipOutputStream zipOutputStream

    ) {

        try {

            String entryName =
                    root.relativize(path)
                            .toString()
                            .replace("\\", "/");

            zipOutputStream.putNextEntry(
                    new ZipEntry(entryName)
            );

            Files.copy(
                    path,
                    zipOutputStream
            );

            zipOutputStream.closeEntry();

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to add file to framework zip: "
                            + path,
                    e
            );
        }
    }

    private void writeGeneratedFeatureFile(
            Path featureFile,
            String incomingContent
    ) throws IOException {

        String normalizedIncoming =
                normalizeGeneratedFeatureContent(
                        incomingContent
                );

        if (
                scenarioCount(normalizedIncoming) == 0
        ) {

            throw new IllegalStateException(
                    "Generated feature update produced no scenarios. Existing generated scenarios were preserved."
            );
        }

        if (
                !Files.exists(featureFile)
        ) {

            validateGeneratedScenarioTags(normalizedIncoming);

            Files.writeString(
                    featureFile,
                    normalizedIncoming
            );

            return;
        }

        String existingContent =
                Files.readString(featureFile);

        String normalizedExisting =
                normalizeGeneratedFeatureContent(
                        existingContent
                );

        int existingScenarioCount =
                scenarioCount(normalizedExisting);

        int newUniqueScenarioCount =
                uniqueIncomingScenarioCount(
                        normalizedExisting,
                        normalizedIncoming
                );

        String mergedContent =
                existingScenarioCount == 0
                        ? normalizedIncoming
                        : mergeFeatureContent(
                                normalizedExisting,
                                normalizedIncoming
                        );

        mergedContent =
                normalizeGeneratedFeatureContent(
                        mergedContent
                );

        int finalScenarioCount =
                scenarioCount(mergedContent);

        if (
                finalScenarioCount
                        < existingScenarioCount
                        + newUniqueScenarioCount
        ) {

            throw new IllegalStateException(
                    "Generated feature merge failed scenario-count validation. Existing scenarios were preserved."
            );
        }

        validateGeneratedScenarioTags(mergedContent);

        Files.writeString(
                backupPath(featureFile),
                existingContent
        );

        Files.writeString(
                featureFile,
                mergedContent
        );
    }

    private Path backupPath(
            Path featureFile
    ) {

        return featureFile.resolveSibling(
                featureFile.getFileName()
                        + ".bak"
        );
    }

    private String mergeFeatureContent(
            String existingContent,
            String incomingContent
    ) {

        List<ScenarioBlock> incomingScenarios =
                scenarioBlocks(incomingContent);

        Set<String> existingKeys =
                scenarioKeys(existingContent);

        List<String> uniqueIncomingBlocks =
                new ArrayList<>();

        for (
                ScenarioBlock incomingScenario
                : incomingScenarios
        ) {

            if (
                    existingKeys.add(
                            incomingScenario.key()
                    )
            ) {

                uniqueIncomingBlocks.add(
                        incomingScenario.content()
                );
            }
        }

        String merged =
                trimTrailingWhitespace(existingContent);

        if (
                uniqueIncomingBlocks.isEmpty()
        ) {

            return merged
                    + "\n";
        }

        StringBuilder builder =
                new StringBuilder(merged);

        builder.append("\n\n");

        for (
                int index = 0;
                index < uniqueIncomingBlocks.size();
                index++
        ) {

            if (
                    index > 0
            ) {

                builder.append("\n");
            }

            builder.append(
                    trimTrailingWhitespace(
                            uniqueIncomingBlocks.get(index)
                    )
            );
            builder.append("\n");
        }

        return builder.toString();
    }

    private String normalizeGeneratedFeatureContent(
            String content
    ) {

        String normalized =
                content == null
                        ? ""
                        : content.replace("\r\n", "\n")
                                .replace('\r', '\n');

        normalized =
                ensureGeneratedScenarioTags(normalized);

        return trimTrailingWhitespace(normalized)
                + "\n";
    }

    private String ensureGeneratedScenarioTags(
            String content
    ) {

        if (
                content == null
                        ||
                        content.isBlank()
        ) {

            return "";
        }

        String[] lines =
                content.split(
                        "\\R",
                        -1
                );

        List<String> output =
                new ArrayList<>();

        List<String> pendingTagLines =
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

                pendingTagLines.add(line);
                continue;
            }

            if (
                    isScenarioHeader(trimmed)
            ) {

                output.add(
                        scenarioIndent(line)
                                + mergedGeneratedTagLine(
                                        pendingTagLines,
                                        line
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
        );
    }

    private String mergedGeneratedTagLine(
            List<String> pendingTagLines,
            String scenarioLine
    ) {

        Set<String> tags =
                new LinkedHashSet<>();

        tags.add("@generated");

        for (
                String tagLine
                : pendingTagLines
        ) {

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

        if (
                looksLikeNegativeScenario(
                        pendingTagLines,
                        scenarioLine
                )
        ) {

            tags.add("@negative");
        }

        return String.join(
                " ",
                tags
        );
    }

    private boolean looksLikeNegativeScenario(
            List<String> pendingTagLines,
            String scenarioLine
    ) {

        String lower =
                (
                        String.join(
                                " ",
                                pendingTagLines
                        )
                                + " "
                                + scenarioLine
                ).toLowerCase();

        return lower.contains("@negative")
                ||
                lower.contains("negative")
                ||
                lower.contains("invalid")
                ||
                lower.contains("reject")
                ||
                lower.contains("required")
                ||
                lower.contains("validation")
                ||
                lower.contains("cannot ")
                ||
                lower.contains("can't ");
    }

    private void validateGeneratedScenarioTags(
            String content
    ) {

        for (
                ScenarioBlock scenario
                : scenarioBlocks(content)
        ) {

            if (
                    !scenario.tags()
                            .stream()
                            .anyMatch("@generated"::equalsIgnoreCase)
            ) {

                throw new IllegalStateException(
                        "Generated feature validation failed because scenario `"
                                + scenario.name()
                                + "` is missing @generated."
                );
            }
        }
    }

    private int uniqueIncomingScenarioCount(
            String existingContent,
            String incomingContent
    ) {

        Set<String> existingKeys =
                scenarioKeys(existingContent);

        int count =
                0;

        for (
                ScenarioBlock incomingScenario
                : scenarioBlocks(incomingContent)
        ) {

            if (
                    !existingKeys.contains(
                            incomingScenario.key()
                    )
            ) {

                existingKeys.add(
                        incomingScenario.key()
                );

                count++;
            }
        }

        return count;
    }

    private Set<String> scenarioKeys(
            String content
    ) {

        Set<String> keys =
                new LinkedHashSet<>();

        for (
                ScenarioBlock scenario
                : scenarioBlocks(content)
        ) {

            keys.add(
                    scenario.key()
            );
        }

        return keys;
    }

    private List<ScenarioBlock> scenarioBlocks(
            String content
    ) {

        List<ScenarioBlock> scenarios =
                new ArrayList<>();

        if (
                content == null
                        ||
                        content.isBlank()
        ) {

            return scenarios;
        }

        String[] lines =
                content.split(
                        "\\R",
                        -1
                );

        List<String> pendingTags =
                new ArrayList<>();

        List<String> currentLines =
                new ArrayList<>();

        List<String> currentTags =
                new ArrayList<>();

        String currentName =
                null;

        for (
                String line
                : lines
        ) {

            String trimmed =
                    line.trim();

            if (
                    trimmed.startsWith("@")
            ) {

                if (
                        currentName != null
                ) {

                    appendScenarioBlock(
                            scenarios,
                            currentName,
                            currentTags,
                            currentLines
                    );

                    currentName =
                            null;

                    currentTags =
                            new ArrayList<>();

                    currentLines =
                            new ArrayList<>();
                }

                pendingTags.add(line);
                continue;
            }

            if (
                    isScenarioHeader(trimmed)
            ) {

                appendScenarioBlock(
                        scenarios,
                        currentName,
                        currentTags,
                        currentLines
                );

                currentName =
                        scenarioName(trimmed);

                currentTags =
                        tagsFromLines(pendingTags);

                currentLines =
                        new ArrayList<>(pendingTags);

                currentLines.add(line);

                pendingTags.clear();
                continue;
            }

            if (
                    currentName != null
            ) {

                currentLines.add(line);
            } else if (
                    !pendingTags.isEmpty()
            ) {

                pendingTags.clear();
            }
        }

        appendScenarioBlock(
                scenarios,
                currentName,
                currentTags,
                currentLines
        );

        return scenarios;
    }

    private void appendScenarioBlock(
            List<ScenarioBlock> scenarios,
            String scenarioName,
            List<String> scenarioTags,
            List<String> scenarioLines
    ) {

        if (
                scenarioName == null
        ) {

            return;
        }

        String content =
                trimTrailingWhitespace(
                        String.join(
                                "\n",
                                scenarioLines
                        )
                );

        scenarios.add(
                new ScenarioBlock(
                        scenarioName,
                        scenarioTags,
                        content
                )
        );
    }

    private List<String> tagsFromLines(
            List<String> tagLines
    ) {

        List<String> tags =
                new ArrayList<>();

        for (
                String tagLine
                : tagLines
        ) {

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

        return tags;
    }

    private int scenarioCount(
            String content
    ) {

        return scenarioBlocks(content)
                .size();
    }

    private boolean isScenarioHeader(
            String trimmedLine
    ) {

        return trimmedLine.startsWith("Scenario:")
                ||
                trimmedLine.startsWith("Scenario Outline:");
    }

    private String scenarioName(
            String scenarioHeader
    ) {

        return scenarioHeader.substring(
                scenarioHeader.indexOf(':') + 1
        )
                .trim();
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

    private String trimTrailingWhitespace(
            String value
    ) {

        return value == null
                ? ""
                : value.replaceAll(
                        "\\s+$",
                        ""
                );
    }

    private record ScenarioBlock(
            String name,
            List<String> tags,
            String content
    ) {

        private String key() {

            return name == null
                    ? ""
                    : name.trim()
                            .toLowerCase()
                            .replaceAll(
                                    "\\s+",
                                    " "
                            );
        }
    }

    // =====================================================
    // PATHS
    // =====================================================

    public Path getWorkspaceRoot(
            String sessionId
    ) {

        return Paths.get(
                "generated-frameworks",
                sanitizePathPart(sessionId)
        );
    }

    public Path getFrameworkRoot(
            String sessionId
    ) {

        return getWorkspaceRoot(sessionId)
                .resolve("framework");
    }

    public int deleteWorkspace(
            String sessionId
    ) {

        Path workspaceRoot =
                getWorkspaceRoot(sessionId)
                        .toAbsolutePath()
                        .normalize();

        Path generatedRoot =
                Paths.get("generated-frameworks")
                        .toAbsolutePath()
                        .normalize();

        if (
                !workspaceRoot.startsWith(generatedRoot)
                        ||
                        workspaceRoot.equals(generatedRoot)
                        ||
                        !Files.exists(workspaceRoot)
        ) {

            return 0;
        }

        try (
                Stream<Path> paths =
                        Files.walk(workspaceRoot)
        ) {

            java.util.List<Path> targets =
                    paths.sorted(
                                    Comparator.reverseOrder()
                            )
                            .toList();

            int deleted =
                    0;

            for (
                    Path target
                    : targets
            ) {

                Files.deleteIfExists(target);
                deleted++;
            }

            return deleted;

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to delete generated workspace for chat "
                            + sessionId,
                    e
            );
        }
    }

    private String sanitizeFeatureName(
            String featureName
    ) {

        if (
                featureName == null
                        ||
                        featureName.isBlank()
        ) {

            return "generated";
        }

        return sanitizePathPart(featureName)
                .toLowerCase();
    }

    private String sanitizePathPart(
            String value
    ) {

        if (
                value == null
                        ||
                        value.isBlank()
        ) {

            return "default";
        }

        return value.replaceAll(
                "[^A-Za-z0-9._-]",
                "-"
        );
    }

    private String generateReadme() {

        return """
                # Generated Axiom AI Test Framework

                ## Run All Tests

                ```bash
                mvn test
                ```

                ## Run By Tag

                ```bash
                mvn test -Dcucumber.filter.tags="@generated"
                mvn test -Dcucumber.filter.tags="@login"
                mvn test -Dcucumber.filter.tags="@ai_requirement"
                ```

                Runtime data can be passed as Maven properties:

                ```bash
                mvn test -Dusername="user@example.com" -Dpassword="secret" -Dproduct="Sauce Labs Backpack"
                ```

                The Cucumber HTML report is written to `target/cucumber-report.html` and includes screenshots captured after each step.
                """;
    }
}
