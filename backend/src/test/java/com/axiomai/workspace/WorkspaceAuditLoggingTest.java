package com.axiomai.workspace;

import com.axiomai.audit.AuditLogService;
import com.axiomai.audit.entity.AuditLogEntity;
import com.axiomai.audit.repository.AuditLogRepository;
import com.axiomai.auth.entity.AifUserEntity;
import com.axiomai.auth.service.AuthService;
import com.axiomai.workspace.entity.WorkspaceSessionOwnershipEntity;
import com.axiomai.workspace.repository.WorkspaceSessionOwnershipRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceAuditLoggingTest {

    @Test
    void workspaceDeleteCreatesAuditEntry() {

        AtomicReference<AuditLogEntity> savedAudit =
                new AtomicReference<>();

        StubWorkspaceCleanupService cleanupService =
                new StubWorkspaceCleanupService();

        StubWorkspaceAccessService accessService =
                new StubWorkspaceAccessService();

        WorkspaceSessionController controller =
                new WorkspaceSessionController(
                        cleanupService,
                        accessService
                );

        controller.setAuditLogService(
                auditService(
                        auditRepository(savedAudit)
                )
        );

        controller.deleteSession(
                "session-1",
                "token-1"
        );

        AuditLogEntity audit =
                savedAudit.get();

        assertEquals(
                "workspace.session.deleted",
                audit.getAction()
        );
        assertEquals(
                AuditLogService.OUTCOME_SUCCESS,
                audit.getOutcome()
        );
        assertEquals(
                "user-1",
                audit.getUserId()
        );
        assertEquals(
                "session-1",
                audit.getSessionId()
        );
        assertEquals(
                1,
                cleanupService.cleanupCalls
        );
        assertEquals(
                1,
                accessService.deleteOwnershipCalls
        );
    }

    @Test
    void ownershipDeniedCreatesAuditEntry() {

        AtomicReference<AuditLogEntity> savedAudit =
                new AtomicReference<>();

        StubAuthService authService =
                new StubAuthService(
                        user(1L)
                );

        WorkspaceAccessService accessService =
                new WorkspaceAccessService(
                        authService,
                        ownershipRepository(
                                Map.of(
                                        "session-b",
                                        ownership(
                                                "session-b",
                                                user(2L)
                                        )
                                )
                        ),
                        null
                );

        accessService.setAuditLogService(
                auditService(
                        auditRepository(savedAudit)
                )
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> accessService.requireAccess(
                                "token-a",
                                "session-b"
                        )
                );

        assertEquals(
                HttpStatus.FORBIDDEN.value(),
                exception.getStatusCode()
                        .value()
        );

        AuditLogEntity audit =
                savedAudit.get();

        assertEquals(
                "workspace.session.ownership_denied",
                audit.getAction()
        );
        assertEquals(
                AuditLogService.OUTCOME_DENIED,
                audit.getOutcome()
        );
        assertEquals(
                "1",
                audit.getUserId()
        );
        assertEquals(
                "session-b",
                audit.getSessionId()
        );
        assertTrue(
                audit.getDetailsJson()
                        .contains("different_owner")
        );
    }

    private AuditLogService auditService(
            AuditLogRepository repository
    ) {

        return new AuditLogService(
                repository,
                new ObjectMapper()
        );
    }

    private AuditLogRepository auditRepository(
            AtomicReference<AuditLogEntity> savedAudit
    ) {

        return (AuditLogRepository) Proxy.newProxyInstance(
                AuditLogRepository.class.getClassLoader(),
                new Class<?>[]{
                        AuditLogRepository.class
                },
                (proxy, method, args) -> {

                    if (
                            "save".equals(method.getName())
                    ) {

                        AuditLogEntity audit =
                                (AuditLogEntity) args[0];

                        savedAudit.set(audit);

                        return audit;
                    }

                    if (
                            "toString".equals(method.getName())
                    ) {

                        return "StubAuditLogRepository";
                    }

                    throw new UnsupportedOperationException(
                            method.getName()
                    );
                }
        );
    }

    private WorkspaceSessionOwnershipRepository ownershipRepository(
            Map<String, WorkspaceSessionOwnershipEntity> ownerships
    ) {

        return (WorkspaceSessionOwnershipRepository) Proxy.newProxyInstance(
                WorkspaceSessionOwnershipRepository.class.getClassLoader(),
                new Class<?>[]{
                        WorkspaceSessionOwnershipRepository.class
                },
                (proxy, method, args) -> {

                    if (
                            "findById".equals(method.getName())
                    ) {

                        return Optional.ofNullable(
                                ownerships.get(
                                        String.valueOf(args[0])
                                )
                        );
                    }

                    if (
                            "toString".equals(method.getName())
                    ) {

                        return "StubWorkspaceSessionOwnershipRepository";
                    }

                    throw new UnsupportedOperationException(
                            method.getName()
                    );
                }
        );
    }

    private static AifUserEntity user(
            Long id
    ) {

        return AifUserEntity.builder()
                .id(id)
                .email("user-" + id + "@example.com")
                .role("USER")
                .build();
    }

    private static WorkspaceSessionOwnershipEntity ownership(
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

    private static class StubAuthService
            extends AuthService {

        private final AifUserEntity user;

        private StubAuthService(
                AifUserEntity user
        ) {

            super(
                    null,
                    null,
                    null
            );

            this.user =
                    user;
        }

        @Override
        public AifUserEntity requireUser(
                String token
        ) {

            return user;
        }
    }

    private static class StubWorkspaceAccessService
            extends WorkspaceAccessService {

        private int deleteOwnershipCalls;

        private StubWorkspaceAccessService() {

            super(
                    null,
                    null,
                    null
            );
        }

        @Override
        public String requireAccess(
                String token,
                String sessionId
        ) {

            return sessionId;
        }

        @Override
        public void deleteOwnership(
                String sessionId
        ) {

            deleteOwnershipCalls++;
        }

        @Override
        public String currentUserId(
                String token
        ) {

            return "user-1";
        }
    }

    private static class StubWorkspaceCleanupService
            extends WorkspaceCleanupService {

        private int cleanupCalls;

        private StubWorkspaceCleanupService() {

            super(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        @Override
        public WorkspaceCleanupResult cleanup(
                String sessionId
        ) {

            cleanupCalls++;

            return new WorkspaceCleanupResult(
                    sessionId,
                    true,
                    true,
                    0,
                    0,
                    null
            );
        }
    }
}
