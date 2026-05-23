package com.axiomai.workspace;

import com.axiomai.core.memory.ExecutionMemoryService;
import com.axiomai.qa.execution.repository.GeneratedTestExecutionJobRepository;
import com.axiomai.qa.execution.service.GeneratedTestExecutionQueueService;
import com.axiomai.qa.service.GeneratedFrameworkPersistenceService;
import com.axiomai.qa.service.GeneratedProjectWriterService;
import com.axiomai.qa.service.HookGeneratorService;
import com.axiomai.qa.service.PomGeneratorService;
import com.axiomai.qa.service.RunnerGeneratorService;
import com.axiomai.workspace.entity.GeneratedFrameworkArchiveEntity;
import com.axiomai.workspace.repository.GeneratedFrameworkArchiveRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceCleanupServiceTest {

    @Test
    void cleanupDeletesChatWorkspaceMemoryReportsAndLocalFiles() throws Exception {

        String sessionId =
                "cleanup-test-"
                        + UUID.randomUUID();

        AutomationWorkspaceService automationWorkspaceService =
                new AutomationWorkspaceService();

        ExecutionMemoryService executionMemoryService =
                new ExecutionMemoryService();

        GeneratedProjectWriterService generatedProjectWriterService =
                new GeneratedProjectWriterService(
                        new HookGeneratorService(),
                        new RunnerGeneratorService(),
                        new PomGeneratorService()
                );

        SupabaseStorageCleanupService supabaseStorageCleanupService =
                new SupabaseStorageCleanupService(
                        new ObjectMapper(),
                        "",
                        "",
                        "",
                        "generated-frameworks/"
                );

        Map<String, GeneratedFrameworkArchiveEntity> archiveStore =
                new HashMap<>();

        archiveStore.put(
                sessionId,
                GeneratedFrameworkArchiveEntity.builder()
                        .sessionId(sessionId)
                        .archive(new byte[]{
                                1
                        })
                        .archiveName("framework.zip")
                        .sizeBytes(1)
                        .updatedAt(Instant.now())
                        .build()
        );

        GeneratedFrameworkArchiveRepository archiveRepository =
                inMemoryArchiveRepository(archiveStore);

        GeneratedFrameworkPersistenceService generatedFrameworkPersistenceService =
                new GeneratedFrameworkPersistenceService(
                        generatedProjectWriterService,
                        supabaseStorageCleanupService,
                        archiveRepository
                );

        Set<String> deletedJobSessions =
                new HashSet<>();

        GeneratedTestExecutionQueueService generatedTestExecutionQueueService =
                new GeneratedTestExecutionQueueService(
                        inMemoryJobRepository(deletedJobSessions),
                        new ObjectMapper()
                );

        WorkspaceCleanupService cleanupService =
                new WorkspaceCleanupService(
                        automationWorkspaceService,
                        executionMemoryService,
                        generatedProjectWriterService,
                        supabaseStorageCleanupService,
                        generatedFrameworkPersistenceService,
                        generatedTestExecutionQueueService
                );

        Path workspaceRoot =
                generatedProjectWriterService
                        .getWorkspaceRoot(sessionId);

        Path generatedFile =
                workspaceRoot.resolve(
                        "framework/src/test/resources/features/sample.feature"
                );

        String reportOne =
                sessionId
                        + "-automation.html";

        String reportTwo =
                sessionId
                        + "-execution.html";

        Path reportRoot =
                Paths.get("reports");

        try {

            Files.createDirectories(
                    generatedFile.getParent()
            );

            Files.writeString(
                    generatedFile,
                    "Feature: cleanup"
            );

            Files.createDirectories(reportRoot);

            Files.writeString(
                    reportRoot.resolve(reportOne),
                    "<html></html>"
            );

            Files.writeString(
                    reportRoot.resolve(reportTwo),
                    "<html></html>"
            );

            automationWorkspaceService.setWebsite(
                    sessionId,
                    "https://example.com"
            );

            automationWorkspaceService.setLatestReport(
                    sessionId,
                    "http://localhost:8080/api/reports/" + reportOne
            );

            executionMemoryService.updateIntent(
                    sessionId,
                    "GENERATE_FRAMEWORK"
            );

            executionMemoryService.storeReport(
                    sessionId,
                    "reports/" + reportTwo
            );

            WorkspaceCleanupResult result =
                    cleanupService.cleanup(sessionId);

            assertTrue(
                    result.workspaceSessionDeleted()
            );

            assertTrue(
                    result.executionSessionDeleted()
            );

            assertTrue(
                    result.localFilesDeleted() > 0
            );

            assertTrue(
                    result.reportFilesDeleted() >= 2
            );

            assertFalse(
                    Files.exists(workspaceRoot)
            );

            assertFalse(
                    Files.exists(
                            reportRoot.resolve(reportOne)
                    )
            );

            assertFalse(
                    Files.exists(
                            reportRoot.resolve(reportTwo)
                    )
            );

            assertFalse(
                    result.supabaseStorage()
                            .configured()
            );

            assertFalse(
                    archiveStore.containsKey(sessionId)
            );

            assertTrue(
                    deletedJobSessions.contains(sessionId)
            );

            assertNull(
                    automationWorkspaceService.getSession(sessionId)
                            .getWebsiteUrl()
            );

        } finally {

            generatedProjectWriterService.deleteWorkspace(
                    sessionId
            );

            Files.deleteIfExists(
                    reportRoot.resolve(reportOne)
            );

            Files.deleteIfExists(
                    reportRoot.resolve(reportTwo)
            );
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

    private static GeneratedTestExecutionJobRepository inMemoryJobRepository(
            Set<String> deletedSessions
    ) {

        return (GeneratedTestExecutionJobRepository) Proxy.newProxyInstance(
                GeneratedTestExecutionJobRepository.class.getClassLoader(),
                new Class<?>[]{
                        GeneratedTestExecutionJobRepository.class
                },
                (proxy, method, args) -> {

                    String methodName =
                            method.getName();

                    if (
                            "deleteForWorkspaceSession".equals(methodName)
                    ) {

                        deletedSessions.add(
                                (String) args[0]
                        );

                        return 0;
                    }

                    if (
                            "toString".equals(methodName)
                    ) {

                        return "InMemoryGeneratedTestExecutionJobRepository";
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
