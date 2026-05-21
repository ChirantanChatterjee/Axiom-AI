package com.axiomai.qa.service;

import com.axiomai.workspace.SupabaseStorageCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class GeneratedFrameworkPersistenceService {

    private static final String FRAMEWORK_ARCHIVE =
            "framework.zip";

    private final GeneratedProjectWriterService
            generatedProjectWriterService;

    private final SupabaseStorageCleanupService
            supabaseStorageCleanupService;

    public boolean persistFramework(
            String sessionId
    ) {

        if (
                !supabaseStorageCleanupService.isConfigured()
        ) {

            return false;
        }

        Path frameworkRoot =
                generatedProjectWriterService
                        .getFrameworkRoot(sessionId);

        if (
                !Files.exists(
                        frameworkRoot.resolve("pom.xml")
                )
        ) {

            return false;
        }

        Path archive =
                Path.of(
                        generatedProjectWriterService
                                .zipFramework(sessionId)
                );

        return persistFrameworkArchive(
                sessionId,
                archive
        );
    }

    public boolean persistFrameworkArchive(

            String sessionId,

            Path archive

    ) {

        if (
                !supabaseStorageCleanupService.isConfigured()
        ) {

            return false;
        }

        try {

            return supabaseStorageCleanupService.uploadFile(
                    archiveObjectPath(sessionId),
                    archive
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to persist generated framework for chat "
                            + sessionId
                            + ". Check SUPABASE_SERVICE_ROLE_KEY and AIF_SUPABASE_STORAGE_BUCKET.",
                    e
            );
        }
    }

    public boolean restoreFramework(
            String sessionId
    ) {

        Path frameworkRoot =
                generatedProjectWriterService
                        .getFrameworkRoot(sessionId)
                        .toAbsolutePath()
                        .normalize();

        if (
                Files.exists(
                        frameworkRoot.resolve("pom.xml")
                )
        ) {

            return true;
        }

        if (
                !supabaseStorageCleanupService.isConfigured()
        ) {

            return false;
        }

        Path workspaceRoot =
                generatedProjectWriterService
                        .getWorkspaceRoot(sessionId)
                        .toAbsolutePath()
                        .normalize();

        Path archive =
                workspaceRoot.resolve("restored-framework.zip");

        try {

            boolean downloaded =
                    supabaseStorageCleanupService.downloadFile(
                            archiveObjectPath(sessionId),
                            archive
                    );

            if (
                    !downloaded
            ) {

                return false;
            }

            deleteDirectory(frameworkRoot);

            Files.createDirectories(frameworkRoot);

            unzipInto(
                    archive,
                    frameworkRoot
            );

            Files.deleteIfExists(archive);

            return Files.exists(
                    frameworkRoot.resolve("pom.xml")
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to restore generated framework for chat "
                            + sessionId
                            + " from Supabase Storage.",
                    e
            );
        }
    }

    public boolean isPersistenceConfigured() {

        return supabaseStorageCleanupService.isConfigured();
    }

    private String archiveObjectPath(
            String sessionId
    ) {

        return supabaseStorageCleanupService.sessionObjectPath(
                sessionId,
                FRAMEWORK_ARCHIVE
        );
    }

    private void unzipInto(

            Path archive,

            Path frameworkRoot

    ) throws IOException {

        try (
                InputStream inputStream =
                        Files.newInputStream(archive);

                ZipInputStream zipInputStream =
                        new ZipInputStream(inputStream)
        ) {

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
                            "Persisted framework archive contains an unsafe path: "
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
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    );
                }

                zipInputStream.closeEntry();
            }
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
}
