package com.axiomai.workspace;

import com.axiomai.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workspace/sessions")
@RequiredArgsConstructor
public class WorkspaceSessionController {

    private final WorkspaceCleanupService
            workspaceCleanupService;

    private final WorkspaceAccessService
            workspaceAccessService;

    private final WorkspaceChatSessionService
            workspaceChatSessionService;

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

        workspaceChatSessionService.delete(
                normalizedSessionId
        );

        auditWorkspaceDeleted(
                token,
                normalizedSessionId
        );

        return result;
    }

    @GetMapping
    public List<WorkspaceChatSessionDto> listSessions(
            @RequestHeader("X-AIF-Session") String token
    ) {

        return workspaceChatSessionService
                .listForCurrentUser(token);
    }

    @PutMapping("/{sessionId}")
    public WorkspaceChatSessionDto saveSession(
            @PathVariable String sessionId,
            @RequestHeader("X-AIF-Session") String token,
            @RequestBody WorkspaceChatSessionDto request
    ) {

        return workspaceChatSessionService
                .saveForCurrentUser(
                        token,
                        sessionId,
                        request
                );
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
