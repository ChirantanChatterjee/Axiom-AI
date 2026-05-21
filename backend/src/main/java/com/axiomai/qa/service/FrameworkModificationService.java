package com.axiomai.qa.service;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class FrameworkModificationService {

    private final GeneratedProjectWriterService generatedProjectWriterService;

    private final GeneratedTestExecutionService generatedTestExecutionService;

    private final FrameworkLearningService frameworkLearningService;

    private final GeneratedFrameworkPersistenceService generatedFrameworkPersistenceService;

    public UploadedFrameworkResult uploadModifiedFramework(

            String sessionId,
            MultipartFile file

    ) {

        if (
                file == null
                        ||
                        file.isEmpty()
        ) {

            throw new RuntimeException(
                    "Upload a non-empty framework zip file."
            );
        }

        String fileName =
                file.getOriginalFilename() == null
                        ? "framework.zip"
                        : file.getOriginalFilename();

        if (
                !fileName.toLowerCase()
                        .endsWith(".zip")
        ) {

            throw new RuntimeException(
                    "Only .zip framework uploads are supported."
            );
        }

        Path frameworkRoot =
                generatedProjectWriterService
                        .getFrameworkRoot(sessionId)
                        .toAbsolutePath()
                        .normalize();

        Path generatedRoot =
                Path.of("generated-frameworks")
                        .toAbsolutePath()
                        .normalize();

        Path workspaceRoot =
                generatedProjectWriterService
                        .getWorkspaceRoot(sessionId)
                        .toAbsolutePath()
                        .normalize();

        if (
                !frameworkRoot.startsWith(generatedRoot)
                        ||
                        !workspaceRoot.startsWith(generatedRoot)
        ) {

            throw new RuntimeException(
                    "Invalid framework session path."
            );
        }

        Path stagingRoot =
                workspaceRoot.resolve("upload-staging")
                        .toAbsolutePath()
                        .normalize();

        FrameworkLearningService.FrameworkSnapshot before =
                Files.exists(frameworkRoot)
                        ? frameworkLearningService.snapshot(frameworkRoot)
                        : new FrameworkLearningService.FrameworkSnapshot(
                                java.util.Set.of(),
                                java.util.Set.of()
                        );

        try {
            deleteDirectory(stagingRoot);
            Files.createDirectories(stagingRoot);

            try (
                    InputStream inputStream =
                            file.getInputStream();

                    ZipInputStream zipInputStream =
                            new ZipInputStream(inputStream)
            ) {

                unzipInto(
                        zipInputStream,
                        stagingRoot
                );
            }

            normalizeNestedFrameworkRoot(stagingRoot);

            if (
                    !Files.exists(
                            stagingRoot.resolve("pom.xml")
                    )
            ) {

                throw new RuntimeException(
                        "Uploaded zip does not contain a runnable Maven framework at its root."
                );
            }

            FrameworkLearningService.FrameworkSnapshot after =
                    frameworkLearningService.snapshot(stagingRoot);

            deleteDirectory(frameworkRoot);

            Files.createDirectories(
                    frameworkRoot.getParent()
            );

            Files.move(
                    stagingRoot,
                    frameworkRoot,
                    StandardCopyOption.REPLACE_EXISTING
            );

            String learningSummary =
                    frameworkLearningService.recordUploadLearning(
                            sessionId,
                            fileName,
                            before,
                            after
                    );

            generatedFrameworkPersistenceService
                    .persistFramework(sessionId);

            GeneratedTestExecutionService.GeneratedTestCatalog catalog =
                    generatedTestExecutionService.listTags(sessionId);

            return UploadedFrameworkResult.builder()
                    .sessionId(sessionId)
                    .frameworkRoot(
                            frameworkRoot.toString()
                    )
                    .fileName(fileName)
                    .tags(catalog.getTags())
                    .learningSummary(learningSummary)
                    .message(
                            "Uploaded framework applied to this chat session. I found "
                                    + catalog.getTags()
                                            .size()
                                    + " executable tag(s), and future generated tests will use the uploaded framework learning profile."
                    )
                    .build();

        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to upload modified framework.",
                    e
            );
        }
    }

    private void unzipInto(

            ZipInputStream zipInputStream,
            Path frameworkRoot

    ) throws IOException {

        ZipEntry entry;

        while (
                (entry = zipInputStream.getNextEntry()) != null
        ) {

            Path target =
                    frameworkRoot.resolve(
                                    entry.getName()
                            )
                            .normalize();

            if (
                    !target.startsWith(frameworkRoot)
            ) {

                throw new RuntimeException(
                        "Uploaded zip contains an unsafe path: "
                                + entry.getName()
                );
            }

            if (
                    entry.isDirectory()
            ) {

                Files.createDirectories(target);
            } else {
                Files.createDirectories(
                        target.getParent()
                );

                Files.copy(
                        zipInputStream,
                        target,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            zipInputStream.closeEntry();
        }
    }

    private void normalizeNestedFrameworkRoot(
            Path frameworkRoot
    ) throws IOException {

        if (
                Files.exists(
                        frameworkRoot.resolve("pom.xml")
                )
        ) {

            return;
        }

        try (
                Stream<Path> children =
                        Files.list(frameworkRoot)
        ) {

            Path nested =
                    children.filter(Files::isDirectory)
                            .filter(path -> Files.exists(
                                    path.resolve("pom.xml")
                            ))
                            .findFirst()
                            .orElse(null);

            if (
                    nested == null
            ) {

                return;
            }

            try (
                    Stream<Path> nestedChildren =
                            Files.list(nested)
            ) {

                for (
                        Path child
                        : nestedChildren.toList()
                ) {

                    Files.move(
                            child,
                            frameworkRoot.resolve(
                                    child.getFileName()
                            ),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }
            }

            deleteDirectory(nested);
        }
    }

    private void deleteDirectory(
            Path path
    ) throws IOException {

        if (
                !Files.exists(path)
        ) {

            return;
        }

        try (
                Stream<Path> paths =
                        Files.walk(path)
        ) {

            for (
                    Path target
                    : paths.sorted(
                            Comparator.reverseOrder()
                    )
                    .toList()
            ) {

                Files.deleteIfExists(target);
            }
        }
    }

    @Getter
    @Builder
    public static class UploadedFrameworkResult {

        private String sessionId;

        private String frameworkRoot;

        private String fileName;

        private java.util.List<GeneratedTestExecutionService.GeneratedTestTag> tags;

        private String learningSummary;

        private String message;
    }
}
