package com.axiomai.qa.service;

import com.axiomai.workspace.SupabaseStorageCleanupService;
import com.axiomai.workspace.entity.GeneratedFrameworkArchiveEntity;
import com.axiomai.workspace.repository.GeneratedFrameworkArchiveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
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

    private final GeneratedFrameworkArchiveRepository
            generatedFrameworkArchiveRepository;

    public boolean persistFramework(
            String sessionId
    ) {

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

        boolean persisted =
                persistFrameworkArchiveToDatabase(
                        sessionId,
                        archive
                );

        if (
                supabaseStorageCleanupService.isConfigured()
        ) {

            try {

                persisted =
                        supabaseStorageCleanupService.uploadFile(
                                archiveObjectPath(sessionId),
                                archive
                        )
                                || persisted;

            } catch (Exception e) {

                if (
                        !persisted
                ) {

                    throw new RuntimeException(
                            "Unable to persist generated framework for chat "
                                    + sessionId
                                    + ". Check SUPABASE_SERVICE_ROLE_KEY and AIF_SUPABASE_STORAGE_BUCKET.",
                            e
                    );
                }
            }
        }

        return persisted;
    }

    private boolean persistFrameworkArchiveToDatabase(

            String sessionId,

            Path archive

    ) {

        try {

            if (
                    archive == null
                            ||
                            !Files.exists(archive)
            ) {

                return false;
            }

            byte[] archiveBytes =
                    Files.readAllBytes(archive);

            generatedFrameworkArchiveRepository.save(
                    GeneratedFrameworkArchiveEntity.builder()
                            .sessionId(
                                    normalizeSessionId(sessionId)
                            )
                            .archive(archiveBytes)
                            .archiveName(FRAMEWORK_ARCHIVE)
                            .sizeBytes(archiveBytes.length)
                            .updatedAt(Instant.now())
                            .build()
            );

            return true;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to persist generated framework archive for chat "
                            + sessionId
                            + " in the database.",
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

        Path workspaceRoot =
                generatedProjectWriterService
                        .getWorkspaceRoot(sessionId)
                        .toAbsolutePath()
                        .normalize();

        if (
                Files.exists(
                        frameworkRoot.resolve("pom.xml")
                )
        ) {

            restoreDatabaseArchiveIfNewer(
                    sessionId,
                    workspaceRoot,
                    frameworkRoot
            );

            return true;
        }

        Exception storageRestoreFailure =
                null;

        if (
                supabaseStorageCleanupService.isConfigured()
        ) {

            Path archive =
                    workspaceRoot.resolve(
                            "restored-framework.zip"
                    );

            try {

                boolean downloaded =
                        supabaseStorageCleanupService.downloadFile(
                                archiveObjectPath(sessionId),
                                archive
                        );

                if (
                        downloaded
                ) {

                    restoreFromArchive(
                            archive,
                            frameworkRoot
                    );

                    Files.deleteIfExists(archive);

                    return Files.exists(
                            frameworkRoot.resolve("pom.xml")
                    );
                }

            } catch (Exception e) {

                storageRestoreFailure =
                        e;
            }
        }

        try {

            boolean restored =
                    restoreFrameworkFromDatabase(
                            sessionId,
                            workspaceRoot,
                            frameworkRoot
                    );

            if (
                    restored
            ) {

                return true;
            }

            if (
                    storageRestoreFailure != null
            ) {

                throw new RuntimeException(
                        "Unable to restore generated framework for chat "
                                + sessionId
                                + " from Supabase Storage, and no database archive was available.",
                        storageRestoreFailure
                );
            }

            return false;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to restore generated framework for chat "
                            + sessionId
                            + " from the database.",
                    e
            );
        }
    }

    private boolean restoreFrameworkFromDatabase(

            String sessionId,

            Path workspaceRoot,

            Path frameworkRoot

    ) throws IOException {

        return generatedFrameworkArchiveRepository
                .findById(
                        normalizeSessionId(sessionId)
                )
                .map(archiveEntity -> {

                    Path archive =
                            workspaceRoot.resolve(
                                    "restored-framework-db.zip"
                            );

                    try {

                        Files.createDirectories(workspaceRoot);

                        Files.write(
                                archive,
                                archiveEntity.getArchive()
                        );

                        restoreFromArchive(
                                archive,
                                frameworkRoot
                        );

                        Files.deleteIfExists(archive);

                        return Files.exists(
                                frameworkRoot.resolve("pom.xml")
                        );

                    } catch (IOException e) {

                        return false;
                    }
                })
                .orElse(false);
    }

    private boolean restoreDatabaseArchiveIfNewer(

            String sessionId,

            Path workspaceRoot,

            Path frameworkRoot

    ) {

        return generatedFrameworkArchiveRepository
                .findById(
                        normalizeSessionId(sessionId)
                )
                .map(archiveEntity -> {

                    Instant localUpdatedAt =
                            latestModifiedTime(frameworkRoot);

                    if (
                            archiveEntity.getUpdatedAt() == null
                                    ||
                                    !archiveEntity.getUpdatedAt()
                                            .isAfter(localUpdatedAt)
                    ) {

                        return false;
                    }

                    try {

                        Path archive =
                                workspaceRoot.resolve(
                                        "restored-framework-db.zip"
                                );

                        Files.createDirectories(workspaceRoot);

                        Files.write(
                                archive,
                                archiveEntity.getArchive()
                        );

                        restoreFromArchive(
                                archive,
                                frameworkRoot
                        );

                        Files.deleteIfExists(archive);

                        return true;

                    } catch (IOException e) {

                        throw new RuntimeException(e);
                    }
                })
                .orElse(false);
    }

    private Instant latestModifiedTime(
            Path frameworkRoot
    ) {

        if (
                frameworkRoot == null
                        ||
                        !Files.exists(frameworkRoot)
        ) {

            return Instant.EPOCH;
        }

        try (
                Stream<Path> paths =
                        Files.walk(frameworkRoot)
        ) {

            return paths.filter(Files::isRegularFile)
                    .map(this::lastModifiedTime)
                    .max(Comparator.naturalOrder())
                    .orElse(Instant.EPOCH);

        } catch (IOException e) {

            return Instant.EPOCH;
        }
    }

    private Instant lastModifiedTime(
            Path path
    ) {

        try {

            FileTime time =
                    Files.getLastModifiedTime(path);

            return time.toInstant();

        } catch (IOException e) {

            return Instant.EPOCH;
        }
    }

    public void deletePersistedFramework(
            String sessionId
    ) {

        generatedFrameworkArchiveRepository.deleteById(
                normalizeSessionId(sessionId)
        );
    }

    private void restoreFromArchive(

            Path archive,

            Path frameworkRoot

    ) throws IOException {

        deleteDirectory(frameworkRoot);

        Files.createDirectories(frameworkRoot);

        unzipInto(
                archive,
                frameworkRoot
        );
    }

    public boolean isPersistenceConfigured() {

        return true;
    }

    private String normalizeSessionId(
            String sessionId
    ) {

        if (
                sessionId == null
                        ||
                        sessionId.isBlank()
        ) {

            return "default-session";
        }

        return sessionId.trim()
                .replaceAll(
                        "[^A-Za-z0-9._-]",
                        "-"
                );
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
