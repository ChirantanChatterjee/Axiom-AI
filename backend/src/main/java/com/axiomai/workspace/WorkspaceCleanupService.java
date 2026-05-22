package com.axiomai.workspace;

import com.axiomai.core.memory.ExecutionMemoryService;
import com.axiomai.core.session.ExecutionSession;
import com.axiomai.qa.service.GeneratedFrameworkPersistenceService;
import com.axiomai.qa.service.GeneratedProjectWriterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class WorkspaceCleanupService {

    private final AutomationWorkspaceService
            automationWorkspaceService;

    private final ExecutionMemoryService
            executionMemoryService;

    private final GeneratedProjectWriterService
            generatedProjectWriterService;

    private final SupabaseStorageCleanupService
            supabaseStorageCleanupService;

    private final GeneratedFrameworkPersistenceService
            generatedFrameworkPersistenceService;

    public WorkspaceCleanupResult cleanup(
            String sessionId
    ) {

        String normalizedSessionId =
                normalizeSessionId(sessionId);

        AutomationSession automationSession =
                automationWorkspaceService.removeSession(
                        normalizedSessionId
                );

        ExecutionSession executionSession =
                executionMemoryService.removeSession(
                        normalizedSessionId
                );

        String workspaceId =
                automationSession == null
                        ||
                        automationSession.getSessionId() == null
                        ||
                        automationSession.getSessionId()
                                .isBlank()
                        ? normalizedSessionId
                        : automationSession.getSessionId();

        int reportFilesDeleted =
                deleteReportIfPresent(
                        automationSession == null
                                ? null
                                : automationSession.getLatestReportPath()
                )
                        + deleteReportIfPresent(
                                executionSession == null
                                        ? null
                                        : executionSession.getLastReportPath()
                        );

        int localFilesDeleted =
                generatedProjectWriterService.deleteWorkspace(
                        workspaceId
                );

        SupabaseStorageCleanupResult supabaseCleanup =
                supabaseStorageCleanupService.cleanupSession(
                        workspaceId
                );

        generatedFrameworkPersistenceService.deletePersistedFramework(
                workspaceId
        );

        return new WorkspaceCleanupResult(
                normalizedSessionId,
                automationSession != null,
                executionSession != null,
                localFilesDeleted,
                reportFilesDeleted,
                supabaseCleanup
        );
    }

    private int deleteReportIfPresent(
            String reportPath
    ) {

        String fileName =
                extractReportFileName(reportPath);

        if (
                fileName == null
        ) {

            return 0;
        }

        try {

            Path root =
                    Paths.get("reports")
                            .toAbsolutePath()
                            .normalize();

            Path target =
                    root.resolve(fileName)
                            .normalize();

            if (
                    !target.startsWith(root)
            ) {

                return 0;
            }

            return Files.deleteIfExists(target)
                    ? 1
                    : 0;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to delete report for chat "
                            + reportPath,
                    e
            );
        }
    }

    private String extractReportFileName(
            String reportPath
    ) {

        if (
                reportPath == null
                        ||
                        reportPath.isBlank()
        ) {

            return null;
        }

        String trimmed =
                reportPath.trim();

        int reportsIndex =
                trimmed.lastIndexOf("/api/reports/");

        if (
                reportsIndex >= 0
        ) {

            return trimmed.substring(
                    reportsIndex
                            + "/api/reports/".length()
            );
        }

        Path path =
                Paths.get(trimmed);

        Path fileName =
                path.getFileName();

        return fileName == null
                ? null
                : fileName.toString();
    }

    private String normalizeSessionId(
            String sessionId
    ) {

        if (
                sessionId == null
                        ||
                        sessionId.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Chat session id is required."
            );
        }

        return sessionId.trim()
                .replaceAll(
                        "[^A-Za-z0-9._-]",
                        "-"
                );
    }
}
