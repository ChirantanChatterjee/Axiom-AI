package com.axiomai.workspace;

import com.axiomai.core.memory.ExecutionMemoryService;
import com.axiomai.qa.service.GeneratedProjectWriterService;
import com.axiomai.qa.service.HookGeneratorService;
import com.axiomai.qa.service.PomGeneratorService;
import com.axiomai.qa.service.RunnerGeneratorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        WorkspaceCleanupService cleanupService =
                new WorkspaceCleanupService(
                        automationWorkspaceService,
                        executionMemoryService,
                        generatedProjectWriterService,
                        supabaseStorageCleanupService
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
}
