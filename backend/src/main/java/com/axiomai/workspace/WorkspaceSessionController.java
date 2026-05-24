package com.axiomai.workspace;

import com.axiomai.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/workspace/sessions")
@RequiredArgsConstructor
public class WorkspaceSessionController {

    private final WorkspaceCleanupService
            workspaceCleanupService;

    private final WorkspaceAccessService
            workspaceAccessService;

    private AuditLogService
            auditLogService;

    @Autowired(required = false)
    public void setAuditLogService(
            AuditLogService auditLogService
    ) {

        this.auditLogService =
                auditLogService;
    }

    @DeleteMapping("/{sessionId}")
    public WorkspaceCleanupResult deleteSession(
            @PathVariable String sessionId,
            @RequestHeader("X-AIF-Session") String token
    ) {

        String normalizedSessionId =
                workspaceAccessService.requireAccess(
                        token,
                        sessionId
                );

        WorkspaceCleanupResult result =
                workspaceCleanupService.cleanup(
                        normalizedSessionId
                );

        workspaceAccessService.deleteOwnership(
                normalizedSessionId
        );

        auditWorkspaceDeleted(
                token,
                normalizedSessionId
        );

        return result;
    }

    private void auditWorkspaceDeleted(
            String token,
            String sessionId
    ) {

        if (
                auditLogService == null
        ) {

            return;
        }

        auditLogService.recordSuccess(
                safeCurrentUserId(token),
                sessionId,
                "workspace.session.deleted",
                "WORKSPACE_SESSION",
                sessionId,
                Map.of("cleanup", "completed")
        );
    }

    private String safeCurrentUserId(
            String token
    ) {

        try {

            return workspaceAccessService.currentUserId(token);

        } catch (RuntimeException e) {

            return null;
        }
    }
}
