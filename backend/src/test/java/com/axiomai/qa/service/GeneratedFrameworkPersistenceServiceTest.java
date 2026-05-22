package com.axiomai.qa.service;

import com.axiomai.workspace.SupabaseStorageCleanupService;
import com.axiomai.workspace.entity.GeneratedFrameworkArchiveEntity;
import com.axiomai.workspace.repository.GeneratedFrameworkArchiveRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedFrameworkPersistenceServiceTest {

    @Test
    void restoresGeneratedFrameworkFromDatabaseArchive() throws Exception {

        String sessionId =
                "persist-test-"
                        + UUID.randomUUID();

        GeneratedProjectWriterService writerService =
                new GeneratedProjectWriterService(
                        new HookGeneratorService(),
                        new RunnerGeneratorService(),
                        new PomGeneratorService()
                );

        Map<String, GeneratedFrameworkArchiveEntity> archiveStore =
                new HashMap<>();

        GeneratedFrameworkArchiveRepository archiveRepository =
                inMemoryArchiveRepository(archiveStore);

        SupabaseStorageCleanupService storageService =
                new SupabaseStorageCleanupService(
                        new ObjectMapper(),
                        "",
                        "",
                        "",
                        "generated-frameworks/"
                );

        GeneratedFrameworkPersistenceService persistenceService =
                new GeneratedFrameworkPersistenceService(
                        writerService,
                        storageService,
                        archiveRepository
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

            GeneratedFrameworkArchiveEntity storedArchive =
                    archiveStore.get(sessionId);

            assertEquals(
                    sessionId,
                    storedArchive.getSessionId()
            );

            assertNotNull(
                    storedArchive.getArchive()
            );

            assertTrue(
                    storedArchive.getArchive().length > 0
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
        }
    }

    private static GeneratedFrameworkArchiveRepository inMemoryArchiveRepository(
            Map<String, GeneratedFrameworkArchiveEntity> archiveStore
    ) {

        return (GeneratedFrameworkArchiveRepository) Proxy.newProxyInstance(
                GeneratedFrameworkArchiveRepository.class.getClassLoader(),
                new Class<?>[]{
                        GeneratedFrameworkArchiveRepository.class
                },
                (proxy, method, args) -> {

                    String methodName =
                            method.getName();

                    if (
                            "save".equals(methodName)
                    ) {

                        GeneratedFrameworkArchiveEntity entity =
                                (GeneratedFrameworkArchiveEntity) args[0];

                        archiveStore.put(
                                entity.getSessionId(),
                                entity
                        );

                        return entity;
                    }

                    if (
                            "findById".equals(methodName)
                    ) {

                        return java.util.Optional.ofNullable(
                                archiveStore.get(args[0])
                        );
                    }

                    if (
                            "deleteById".equals(methodName)
                    ) {

                        archiveStore.remove(args[0]);
                        return null;
                    }

                    if (
                            "toString".equals(methodName)
                    ) {

                        return "InMemoryGeneratedFrameworkArchiveRepository";
                    }

                    if (
                            "hashCode".equals(methodName)
                    ) {

                        return System.identityHashCode(proxy);
                    }

                    if (
                            "equals".equals(methodName)
                    ) {

                        return proxy == args[0];
                    }

                    throw new UnsupportedOperationException(
                            "Unexpected repository method: "
                                    + methodName
                    );
                }
        );
    }
}
