package com.axiomai.qa.service;

import com.axiomai.qa.models.GeneratedFramework;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
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

            Files.writeString(
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

            Files.writeString(
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
