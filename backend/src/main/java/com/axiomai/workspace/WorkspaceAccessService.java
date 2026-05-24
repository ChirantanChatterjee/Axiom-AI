package com.axiomai.workspace;

import com.axiomai.auth.entity.AifUserEntity;
import com.axiomai.auth.service.AuthService;
import com.axiomai.workspace.entity.WorkspaceSessionOwnershipEntity;
import com.axiomai.workspace.repository.WorkspaceSessionOwnershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class WorkspaceAccessService {

    private final AuthService
            authService;

    private final WorkspaceSessionOwnershipRepository
            ownershipRepository;

    private final WorkspaceSessionPresenceService
            sessionPresenceService;

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

            return normalizedSessionId;
        }

        assertOwner(
                ownership,
                user
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
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "This workspace is not assigned to the current user."
                                )
                        );

        assertOwner(
                ownership,
                user
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
            AifUserEntity user
    ) {

        if (
                ownership.getUserId() == null
                        ||
                        !ownership.getUserId()
                                .equals(user.getId())
        ) {

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
}
