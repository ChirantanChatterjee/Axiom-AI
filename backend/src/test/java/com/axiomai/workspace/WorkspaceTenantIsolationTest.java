package com.axiomai.workspace;

import com.axiomai.ai.controller.AIChatController;
import com.axiomai.ai.dto.AIResponse;
import com.axiomai.ai.dto.ChatRequest;
import com.axiomai.ai.service.AIOrchestratorService;
import com.axiomai.auth.entity.AifUserEntity;
import com.axiomai.auth.service.AuthService;
import com.axiomai.qa.execution.controller.GeneratedTestExecutionJobController;
import com.axiomai.qa.execution.entity.GeneratedTestExecutionJobEntity;
import com.axiomai.qa.execution.service.GeneratedTestExecutionJobDto;
import com.axiomai.qa.execution.service.GeneratedTestExecutionQueueService;
import com.axiomai.workspace.entity.WorkspaceSessionOwnershipEntity;
import com.axiomai.workspace.repository.WorkspaceSessionOwnershipRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkspaceTenantIsolationTest {

    @Test
    void userACannotDeleteUserBWorkspaceSession() {

        StubWorkspaceCleanupService cleanupService =
                new StubWorkspaceCleanupService();

        StubWorkspaceAccessService accessService =
                new StubWorkspaceAccessService();

        accessService.requireAccessException =
                forbidden();

        WorkspaceSessionController controller =
                new WorkspaceSessionController(
                        cleanupService,
                        accessService,
                        new StubWorkspaceChatSessionService()
                );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> controller.deleteSession(
                                "session-b",
                                "token-a"
                        )
                );

        assertForbidden(exception);

        assertEquals(
                0,
                cleanupService.cleanupCalls
        );

        assertEquals(
                0,
                accessService.deleteOwnershipCalls
        );
    }

    @Test
    void savingChatHistoryDoesNotClaimLegacyWorkspaceState() {

        StubWorkspaceCleanupService cleanupService =
                new StubWorkspaceCleanupService();

        StubWorkspaceAccessService accessService =
                new StubWorkspaceAccessService();

        StubWorkspaceChatSessionService chatSessionService =
                new StubWorkspaceChatSessionService();

        WorkspaceSessionController controller =
                new WorkspaceSessionController(
                        cleanupService,
                        accessService,
                        chatSessionService
                );

        controller.saveSession(
                "legacy-session",
                "token-a",
                WorkspaceChatSessionDto.builder()
                        .title("Legacy chat")
                        .messages(List.of(
                                Map.of(
                                        "sender",
                                        "user",
                                        "text",
                                        "generate framework"
                                )
                        ))
                        .build()
        );

        assertEquals(
                0,
                accessService.bindCalls
        );

        assertEquals(
                1,
                chatSessionService.saveCalls
        );

        assertEquals(
                "legacy-session",
                chatSessionService.lastSessionId
        );
    }

    @Test
    void userACannotInspectUserBGeneratedExecutionJob() {

        StubGeneratedTestExecutionQueueService queueService =
                new StubGeneratedTestExecutionQueueService();

        StubWorkspaceAccessService accessService =
                new StubWorkspaceAccessService();

        accessService.requireAccessException =
                forbidden();

        queueService.job =
                GeneratedTestExecutionJobEntity.builder()
                        .id("job-b")
                        .sessionId("session-b")
                        .userId("session-b")
                        .status("QUEUED")
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();

        GeneratedTestExecutionJobController controller =
                new GeneratedTestExecutionJobController(
                        queueService,
                        accessService
                );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> controller.getJob(
                                "job-b",
                                "token-a"
                        )
                );

        assertForbidden(exception);

        assertEquals(
                0,
                queueService.toDtoCalls
        );

        assertEquals(
                "session-b",
                accessService.lastRequiredSessionId
        );
    }

    @Test
    void userACannotExecuteTestsAgainstUserBWorkspace() {

        StubAIOrchestratorService orchestratorService =
                new StubAIOrchestratorService();

        StubWorkspaceAccessService accessService =
                new StubWorkspaceAccessService();

        accessService.bindException =
                forbidden();

        AIChatController controller =
                new AIChatController(
                        orchestratorService,
                        accessService,
                        new StubWorkspaceChatSessionService()
                );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> controller.chat(
                                new ChatRequest(
                                        "run generated tests",
                                        "session-b",
                                        null,
                                        null,
                                        true
                                ),
                                "token-a"
                        )
                );

        assertForbidden(exception);

        assertEquals(
                0,
                orchestratorService.processCalls
        );
    }

    @Test
    void chatControllerPersistsConversationTurnAfterSuccessfulResponse() {

        StubAIOrchestratorService orchestratorService =
                new StubAIOrchestratorService();

        StubWorkspaceAccessService accessService =
                new StubWorkspaceAccessService();

        StubWorkspaceChatSessionService chatSessionService =
                new StubWorkspaceChatSessionService();

        AIChatController controller =
                new AIChatController(
                        orchestratorService,
                        accessService,
                        chatSessionService
                );

        AIResponse response =
                controller.chat(
                        new ChatRequest(
                                "generate tests",
                                "session-a",
                                "https://example.test",
                                "example.test",
                                false
                        ),
                        "token-a"
                );

        assertEquals(
                1,
                orchestratorService.processCalls
        );

        assertEquals(
                true,
                response.isSuccess()
        );

        assertEquals(
                1,
                chatSessionService.appendCalls
        );

        assertEquals(
                "session-a",
                chatSessionService.lastSessionId
        );

        assertEquals(
                2,
                chatSessionService.lastMessages.size()
        );
    }

    @Test
    void chatControllerPersistsStructuredVariablesWithoutClearTextValues() {

        StubAIOrchestratorService orchestratorService =
                new StubAIOrchestratorService();

        StubWorkspaceAccessService accessService =
                new StubWorkspaceAccessService();

        StubWorkspaceChatSessionService chatSessionService =
                new StubWorkspaceChatSessionService();

        AIChatController controller =
                new AIChatController(
                        orchestratorService,
                        accessService,
                        chatSessionService
                );

        ChatRequest request =
                new ChatRequest(
                        "password is secret_sauce",
                        "session-a",
                        "https://example.test",
                        "example.test",
                        false
                );

        request.setIntent("UPDATE_TEST_DATA");
        request.setVariables(
                Map.of(
                        "password",
                        "secret_sauce"
                )
        );

        controller.chat(
                request,
                "token-a"
        );

        Map<String, Object> persistedUserMessage =
                chatSessionService.lastMessages.get(0);

        assertEquals(
                "Submitted runtime values for password.",
                persistedUserMessage.get("text")
        );

        assertEquals(
                "variables",
                persistedUserMessage.get("type")
        );

        assertEquals(
                Map.of(
                        "password",
                        "Saved"
                ),
                persistedUserMessage.get("data")
        );

        assertEquals(
                "UPDATE_TEST_DATA",
                orchestratorService.lastExplicitIntent
        );

        assertEquals(
                Map.of(
                        "password",
                        "secret_sauce"
                ),
                orchestratorService.lastExplicitVariables
        );
    }

    @Test
    void legacyUnownedSessionsCannotBeArbitrarilyClaimed() {

        StubAuthService authService =
                new StubAuthService();

        StubPresenceService presenceService =
                new StubPresenceService();

        AtomicReference<WorkspaceSessionOwnershipEntity> savedOwnership =
                new AtomicReference<>();

        authService.users.put(
                "token-a",
                user(1L)
        );

        presenceService.hasExistingState =
                true;

        WorkspaceAccessService accessService =
                new WorkspaceAccessService(
                        authService,
                        ownershipRepository(
                                Map.of(),
                                savedOwnership
                        ),
                        presenceService
                );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> accessService.bindToCurrentUser(
                                "token-a",
                                "legacy-session"
                        )
                );

        assertForbidden(exception);

        assertNull(
                savedOwnership.get()
        );
    }

    @Test
    void freshUnownedSessionCanBeBoundForCurrentUser() {

        StubAuthService authService =
                new StubAuthService();

        StubPresenceService presenceService =
                new StubPresenceService();

        AtomicReference<WorkspaceSessionOwnershipEntity> savedOwnership =
                new AtomicReference<>();

        authService.users.put(
                "token-a",
                user(1L)
        );

        WorkspaceAccessService accessService =
                new WorkspaceAccessService(
                        authService,
                        ownershipRepository(
                                Map.of(),
                                savedOwnership
                        ),
                        presenceService
                );

        assertEquals(
                "new-session",
                accessService.bindToCurrentUser(
                        "token-a",
                        "new-session"
                )
        );

        assertEquals(
                1L,
                savedOwnership.get()
                        .getUserId()
        );
    }

    @Test
    void ownershipCheckRejectsDifferentUser() {

        StubAuthService authService =
                new StubAuthService();

        authService.users.put(
                "token-a",
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
                                ),
                                new AtomicReference<>()
                        ),
                        new StubPresenceService()
                );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> accessService.requireAccess(
                                "token-a",
                                "session-b"
                        )
                );

        assertForbidden(exception);
    }

    private static void assertForbidden(
            ResponseStatusException exception
    ) {

        assertEquals(
                HttpStatus.FORBIDDEN.value(),
                exception.getStatusCode()
                        .value()
        );
    }

    private ResponseStatusException forbidden() {

        return new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "This workspace belongs to a different user."
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

    private static WorkspaceSessionOwnershipRepository ownershipRepository(
            Map<String, WorkspaceSessionOwnershipEntity> ownerships,
            AtomicReference<WorkspaceSessionOwnershipEntity> savedOwnership
    ) {

        Map<String, WorkspaceSessionOwnershipEntity> storedOwnerships =
                new HashMap<>(ownerships);

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
                                storedOwnerships.get(
                                        String.valueOf(args[0])
                                )
                        );
                    }

                    if (
                            "save".equals(method.getName())
                    ) {

                        WorkspaceSessionOwnershipEntity ownership =
                                (WorkspaceSessionOwnershipEntity) args[0];

                        savedOwnership.set(ownership);
                        storedOwnerships.put(
                                ownership.getSessionId(),
                                ownership
                        );

                        return ownership;
                    }

                    if (
                            "deleteById".equals(method.getName())
                    ) {

                        storedOwnerships.remove(
                                String.valueOf(args[0])
                        );

                        return null;
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

    private static class StubAuthService
            extends AuthService {

        private final Map<String, AifUserEntity> users =
                new HashMap<>();

        private StubAuthService() {

            super(
                    null,
                    null,
                    null
            );
        }

        @Override
        public AifUserEntity requireUser(
                String token
        ) {

            AifUserEntity user =
                    users.get(token);

            if (
                    user == null
            ) {

                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Missing session token."
                );
            }

            return user;
        }
    }

    private static class StubPresenceService
            extends WorkspaceSessionPresenceService {

        private boolean hasExistingState;

        private StubPresenceService() {

            super(
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        @Override
        public boolean hasExistingSessionState(
                String sessionId
        ) {

            return hasExistingState;
        }
    }

    private static class StubWorkspaceAccessService
            extends WorkspaceAccessService {

        private ResponseStatusException requireAccessException;

        private ResponseStatusException bindException;

        private String lastRequiredSessionId;

        private int bindCalls;

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

            lastRequiredSessionId =
                    sessionId;

            if (
                    requireAccessException != null
            ) {

                throw requireAccessException;
            }

            return sessionId;
        }

        @Override
        public String bindToCurrentUser(
                String token,
                String sessionId
        ) {

            bindCalls++;

            if (
                    bindException != null
            ) {

                throw bindException;
            }

            return sessionId;
        }

        @Override
        public void deleteOwnership(
                String sessionId
        ) {

            deleteOwnershipCalls++;
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

            return null;
        }
    }

    private static class StubWorkspaceChatSessionService
            extends WorkspaceChatSessionService {

        private int appendCalls;

        private int saveCalls;

        private String lastSessionId;

        private List<Map<String, Object>> lastMessages =
                List.of();

        private StubWorkspaceChatSessionService() {

            super(
                    null,
                    null,
                    null
            );
        }

        @Override
        public void delete(
                String sessionId
        ) {
        }

        @Override
        public WorkspaceChatSessionDto saveForCurrentUser(
                String token,
                String sessionId,
                WorkspaceChatSessionDto request
        ) {

            saveCalls++;
            lastSessionId =
                    sessionId;

            return request;
        }

        @Override
        public WorkspaceChatSessionDto appendMessagesForCurrentUser(
                String token,
                String sessionId,
                WorkspaceChatSessionDto metadata,
                List<Map<String, Object>> newMessages
        ) {

            appendCalls++;
            lastSessionId =
                    sessionId;
            lastMessages =
                    newMessages;

            return WorkspaceChatSessionDto.builder()
                    .id(sessionId)
                    .messages(newMessages)
                    .build();
        }
    }

    private static class StubGeneratedTestExecutionQueueService
            extends GeneratedTestExecutionQueueService {

        private GeneratedTestExecutionJobEntity job;

        private int toDtoCalls;

        private StubGeneratedTestExecutionQueueService() {

            super(
                    null,
                    null
            );
        }

        @Override
        public Optional<GeneratedTestExecutionJobEntity> find(
                String jobId
        ) {

            return Optional.ofNullable(job);
        }

        @Override
        public GeneratedTestExecutionJobDto toDto(
                GeneratedTestExecutionJobEntity job
        ) {

            toDtoCalls++;

            return null;
        }
    }

    private static class StubAIOrchestratorService
            extends AIOrchestratorService {

        private int processCalls;

        private String lastExplicitIntent;

        private Map<String, String> lastExplicitVariables;

        private StubAIOrchestratorService() {

            super(
                    null,
                    null,
                    null,
                    null
            );
        }

        @Override
        public AIResponse processMessage(
                String message,
                String sessionId,
                String websiteUrl,
                String domainName,
                Boolean frameworkLocked,
                String explicitIntent,
                Map<String, String> explicitVariables
        ) {

            processCalls++;
            lastExplicitIntent =
                    explicitIntent;
            lastExplicitVariables =
                    explicitVariables;

            return AIResponse.builder()
                    .success(true)
                    .build();
        }
    }
}
