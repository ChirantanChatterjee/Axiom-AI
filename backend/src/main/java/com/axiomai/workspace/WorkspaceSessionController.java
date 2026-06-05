package com.axiomai.workspace;

import com.axiomai.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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

        String normalizedSessionId;

        try {

            normalizedSessionId =
                    workspaceAccessService.requireAccess(
                            token,
                            sessionId
                    );

        } catch (org.springframework.web.server.ResponseStatusException e) {

            if (
                    !isUnassignedWorkspaceAccessFailure(e)
                            ||
                            !workspaceChatSessionService.deleteForCurrentUser(
                                    token,
                                    sessionId
                            )
            ) {

                throw e;
            }

            normalizedSessionId =
                    normalizeSessionId(sessionId);

            auditWorkspaceDeleted(
                    token,
                    normalizedSessionId,
                    Map.of("cleanup", "chat-only")
            );

            return new WorkspaceCleanupResult(
                    normalizedSessionId,
                    false,
                    false,
                    0,
                    0,
                    null
            );
        }

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
                normalizedSessionId,
                Map.of("cleanup", "completed")
        );

        return result;
    }

    @GetMapping
    public List<WorkspaceChatSessionDto> listSessions(
            @RequestHeader("X-AIF-Session") String token
    ) {

        log.info(
                "Workspace chat sessions list requested hasSessionHeader={}",
                hasSessionHeader(token)
        );

        return workspaceChatSessionService
                .listForCurrentUser(token);
    }

    @PutMapping("/{sessionId}")
    public WorkspaceChatSessionDto saveSession(
            @PathVariable String sessionId,
            @RequestHeader("X-AIF-Session") String token,
            @RequestBody WorkspaceChatSessionDto request
    ) {

        log.info(
                "Workspace chat session save requested sessionId={} hasSessionHeader={} messageCount={}",
                sessionId,
                hasSessionHeader(token),
                messageCount(request)
        );

        return workspaceChatSessionService
                .saveForCurrentUser(
                        token,
                        sessionId,
                        request
                );
    }

    private boolean hasSessionHeader(
            String token
    ) {

        return token != null
                &&
                !token.isBlank();
    }

    private int messageCount(
            WorkspaceChatSessionDto request
    ) {

        if (
                request == null
                        ||
                        request.getMessages() == null
        ) {

            return 0;
        }

        return request.getMessages()
                .size();
    }

    private void auditWorkspaceDeleted(
            String token,
            String sessionId,
            Map<String, ?> details
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
                details
        );
    }

    private boolean isUnassignedWorkspaceAccessFailure(
            org.springframework.web.server.ResponseStatusException exception
    ) {

        if (
                exception == null
                        ||
                        exception.getStatusCode()
                                .value() != 403
        ) {

            return false;
        }

        String reason =
                exception.getReason();

        return reason != null
                &&
                (
                        reason.contains("not assigned")
                                ||
                                reason.contains("unassigned")
                );
    }

    private String normalizeSessionId(
            String sessionId
    ) {

        if (
                sessionId == null
                        ||
                        sessionId.isBlank()
        ) {

            return "";
        }

        return sessionId.trim()
                .replaceAll(
                        "[^A-Za-z0-9._-]",
                        "-"
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
