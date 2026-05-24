package com.axiomai.workspace;

import com.axiomai.audit.AuditLogService;
import com.axiomai.auth.entity.AifUserEntity;
import com.axiomai.auth.service.AuthService;
import com.axiomai.workspace.entity.WorkspaceSessionOwnershipEntity;
import com.axiomai.workspace.repository.WorkspaceSessionOwnershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkspaceAccessService {

    private final AuthService
            authService;

    private final WorkspaceSessionOwnershipRepository
            ownershipRepository;

    private final WorkspaceSessionPresenceService
            sessionPresenceService;

    private AuditLogService
            auditLogService;

    @Autowired(required = false)
    public void setAuditLogService(
            AuditLogService auditLogService
    ) {

        this.auditLogService =
                auditLogService;
    }

    @Transactional
    public String bindToCurrentUser(
            String token,
            String sessionId
    ) {

        AifUserEntity user =
                authService.requireUser(token);

        String normalizedSessionId =
                normalizeSessionId(sessionId);

        WorkspaceSessionOwnershipEntity ownership =
                ownershipRepository.findById(normalizedSessionId)
                        .orElse(null);

        if (
                ownership == null
        ) {

            if (
                    sessionPresenceService != null
                            &&
                            sessionPresenceService.hasExistingSessionState(
                                    normalizedSessionId
                            )
            ) {

                auditDenied(
                        user,
                        normalizedSessionId,
                        "workspace.session.legacy_claim",
                        Map.of(
                                "reason",
                                "existing_unowned_session_state"
                        )
                );

                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "This existing workspace is not assigned to the current user."
                );
            }

            ownershipRepository.save(
                    createOwnership(
                            normalizedSessionId,
                            user
                    )
            );

            auditSuccess(
                    user,
                    normalizedSessionId,
                    "workspace.session.bound",
                    Map.of("created", true)
            );

            return normalizedSessionId;
        }

        assertOwner(
                ownership,
                user,
                normalizedSessionId
        );

        auditSuccess(
                user,
                normalizedSessionId,
                "workspace.session.bound",
                Map.of("created", false)
        );

        return normalizedSessionId;
    }

    @Transactional
    public String requireOrBindAccess(
            String token,
            String sessionId
    ) {

        return bindToCurrentUser(
                token,
                sessionId
        );
    }

    @Transactional(readOnly = true)
    public String requireAccess(
            String token,
            String sessionId
    ) {

        AifUserEntity user =
                authService.requireUser(token);

        String normalizedSessionId =
                normalizeSessionId(sessionId);

        WorkspaceSessionOwnershipEntity ownership =
                ownershipRepository.findById(normalizedSessionId)
                        .orElse(null);

        if (
                ownership == null
        ) {

            auditDenied(
                    user,
                    normalizedSessionId,
                    "workspace.session.access",
                    Map.of("reason", "unassigned")
            );

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This workspace is not assigned to the current user."
            );
        }

        assertOwner(
                ownership,
                user,
                normalizedSessionId
        );

        return normalizedSessionId;
    }

    @Transactional
    public void deleteOwnership(
            String sessionId
    ) {

        ownershipRepository.deleteById(
                normalizeSessionId(sessionId)
        );
    }

    @Transactional(readOnly = true)
    public String currentUserId(
            String token
    ) {

        AifUserEntity user =
                authService.requireUser(token);

        return user.getId() == null
                ? null
                : String.valueOf(user.getId());
    }

    private WorkspaceSessionOwnershipEntity createOwnership(
            String sessionId,
            AifUserEntity user
    ) {

        Instant now =
                Instant.now();

        return WorkspaceSessionOwnershipEntity.builder()
                .sessionId(sessionId)
                .userId(user.getId())
                .userEmail(user.getEmail())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private void assertOwner(
            WorkspaceSessionOwnershipEntity ownership,
            AifUserEntity user,
            String sessionId
    ) {

        if (
                ownership.getUserId() == null
                        ||
                        !ownership.getUserId()
                                .equals(user.getId())
        ) {

            auditDenied(
                    user,
                    sessionId,
                    "workspace.session.ownership_denied",
                    Map.of("reason", "different_owner")
            );

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This workspace belongs to a different user."
            );
        }
    }

    private String normalizeSessionId(
            String sessionId
    ) {

        if (
                sessionId == null
                        ||
                        sessionId.isBlank()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chat session id is required."
            );
        }

        String normalized =
                sessionId.trim()
                        .replaceAll(
                                "[^A-Za-z0-9._-]",
                                "-"
                        );

        if (
                normalized.isBlank()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chat session id is required."
            );
        }

        return normalized;
    }

    private void auditSuccess(
            AifUserEntity user,
            String sessionId,
            String action,
            Map<String, ?> details
    ) {

        if (
                auditLogService == null
        ) {

            return;
        }

        auditLogService.recordSuccess(
                auditUserId(user),
                sessionId,
                action,
                "WORKSPACE_SESSION",
                sessionId,
                details
        );
    }

    private void auditDenied(
            AifUserEntity user,
            String sessionId,
            String action,
            Map<String, ?> details
    ) {

        if (
                auditLogService == null
        ) {

            return;
        }

        auditLogService.recordDenied(
                auditUserId(user),
                sessionId,
                action,
                "WORKSPACE_SESSION",
                sessionId,
                details
        );
    }

    private String auditUserId(
            AifUserEntity user
    ) {

        if (
                user == null
                        ||
                        user.getId() == null
        ) {

            return null;
        }

        return String.valueOf(
                user.getId()
        );
    }
}
