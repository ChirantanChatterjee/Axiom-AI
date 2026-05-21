package com.axiomai.qa.service;

import com.axiomai.workspace.SupabaseStorageCleanupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedFrameworkPersistenceServiceTest {

    @Test
    void restoresGeneratedFrameworkFromPersistedArchive() throws Exception {

        String sessionId =
                "persist-test-"
                        + UUID.randomUUID();

        GeneratedProjectWriterService writerService =
                new GeneratedProjectWriterService(
                        new HookGeneratorService(),
                        new RunnerGeneratorService(),
                        new PomGeneratorService()
                );

        Path storedArchive =
                Files.createTempFile(
                        "aif-framework-",
                        ".zip"
                );

        Files.deleteIfExists(storedArchive);

        SupabaseStorageCleanupService storageService =
                new FakeSupabaseStorageService(storedArchive);

        GeneratedFrameworkPersistenceService persistenceService =
                new GeneratedFrameworkPersistenceService(
                        writerService,
                        storageService
                );

        Path frameworkRoot =
                writerService.getFrameworkRoot(sessionId);

        try {

            Files.createDirectories(
                    frameworkRoot.resolve(
                            "src/test/resources/features"
                    )
            );

            Files.writeString(
                    frameworkRoot.resolve("pom.xml"),
                    "<project></project>"
            );

            Files.writeString(
                    frameworkRoot.resolve(
                            "src/test/resources/features/sample.feature"
                    ),
                    "Feature: persisted"
            );

            assertTrue(
                    persistenceService.persistFramework(sessionId)
            );

            assertTrue(
                    Files.exists(storedArchive)
            );

            writerService.deleteWorkspace(sessionId);

            assertFalse(
                    Files.exists(frameworkRoot)
            );

            assertTrue(
                    persistenceService.restoreFramework(sessionId)
            );

            assertTrue(
                    Files.exists(
                            frameworkRoot.resolve("pom.xml")
                    )
            );

            assertTrue(
                    Files.exists(
                            frameworkRoot.resolve(
                                    "src/test/resources/features/sample.feature"
                            )
                    )
            );

        } finally {

            writerService.deleteWorkspace(sessionId);
            Files.deleteIfExists(storedArchive);
        }
    }

    private static class FakeSupabaseStorageService extends SupabaseStorageCleanupService {

        private final Path storedArchive;

        FakeSupabaseStorageService(
                Path storedArchive
        ) {

            super(
                    new ObjectMapper(),
                    "https://example.supabase.co",
                    "service-role",
                    "aif-artifacts",
                    "generated-frameworks/"
            );

            this.storedArchive =
                    storedArchive;
        }

        @Override
        public boolean uploadFile(

                String objectPath,

                Path file

        ) throws Exception {

            Files.copy(
                    file,
                    storedArchive,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

            return true;
        }

        @Override
        public boolean downloadFile(

                String objectPath,

                Path target

        ) throws Exception {

            Files.createDirectories(
                    target.getParent()
            );

            Files.copy(
                    storedArchive,
                    target,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

            return true;
        }
    }
}
