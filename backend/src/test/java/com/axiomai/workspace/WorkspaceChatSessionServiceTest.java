package com.axiomai.workspace;

import com.axiomai.auth.entity.AifUserEntity;
import com.axiomai.auth.service.AuthService;
import com.axiomai.workspace.entity.WorkspaceChatSessionEntity;
import com.axiomai.workspace.repository.WorkspaceChatSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkspaceChatSessionServiceTest {

    @Test
    void appendMessagesPersistsConversationForAuthenticatedUser() {

        Map<String, WorkspaceChatSessionEntity> store =
                new HashMap<>();

        WorkspaceChatSessionService service =
                new WorkspaceChatSessionService(
                        repository(store),
                        new StubAuthService(),
                        new ObjectMapper()
                );

        service.appendMessagesForCurrentUser(
                "token-a",
                "session-a",
                WorkspaceChatSessionDto.builder()
                        .websiteUrl("https://example.test")
                        .domainName("example.test")
                        .build(),
                List.of(
                        Map.of(
                                "sender",
                                "user",
                                "text",
                                "Generate checkout tests"
                        ),
                        Map.of(
                                "sender",
                                "ai",
                                "text",
                                "Generated checkout tests."
                        )
                )
        );

        List<WorkspaceChatSessionDto> sessions =
                service.listForCurrentUser("token-a");

        assertEquals(
                1,
                sessions.size()
        );

        WorkspaceChatSessionDto session =
                sessions.get(0);

        assertEquals(
                "session-a",
                session.getId()
        );

        assertEquals(
                "Generate checkout tests",
                session.getTitle()
        );

        assertEquals(
                "https://example.test",
                session.getWebsiteUrl()
        );

        assertEquals(
                2,
                session.getMessages()
                        .size()
        );
    }

    @Test
    void appendMessagesDoesNotDuplicateAlreadySyncedLastUserMessage() {

        Map<String, WorkspaceChatSessionEntity> store =
                new HashMap<>();

        WorkspaceChatSessionService service =
                new WorkspaceChatSessionService(
                        repository(store),
                        new StubAuthService(),
                        new ObjectMapper()
                );

        List<Map<String, Object>> userMessage =
                List.of(
                        Map.of(
                                "sender",
                                "user",
                                "text",
                                "Run checkout"
                        )
                );

        service.appendMessagesForCurrentUser(
                "token-a",
                "session-a",
                WorkspaceChatSessionDto.builder()
                        .build(),
                userMessage
        );

        service.appendMessagesForCurrentUser(
                "token-a",
                "session-a",
                WorkspaceChatSessionDto.builder()
                        .build(),
                List.of(
                        Map.of(
                                "sender",
                                "user",
                                "text",
                                "Run checkout"
                        ),
                        Map.of(
                                "sender",
                                "ai",
                                "text",
                                "Started checkout run."
                        )
                )
        );

        WorkspaceChatSessionDto session =
                service.listForCurrentUser("token-a")
                        .get(0);

        assertEquals(
                2,
                session.getMessages()
                        .size()
        );
    }

    private WorkspaceChatSessionRepository repository(
            Map<String, WorkspaceChatSessionEntity> store
    ) {

        return (WorkspaceChatSessionRepository) Proxy.newProxyInstance(
                WorkspaceChatSessionRepository.class.getClassLoader(),
                new Class<?>[]{
                        WorkspaceChatSessionRepository.class
                },
                (proxy, method, args) -> {

                    return switch (method.getName()) {
                        case "findById" -> Optional.ofNullable(
                                store.get(args[0])
                        );
                        case "save" -> {
                            WorkspaceChatSessionEntity entity =
                                    (WorkspaceChatSessionEntity) args[0];
                            store.put(
                                    entity.getSessionId(),
                                    entity
                            );
                            yield entity;
                        }
                        case "findByUserIdOrderByUpdatedAtDesc" ->
                                store.values()
                                        .stream()
                                        .filter(entity -> entity.getUserId()
                                                .equals(args[0]))
                                        .sorted((left, right) -> right.getUpdatedAt()
                                                .compareTo(left.getUpdatedAt()))
                                        .toList();
                        case "deleteById" -> store.remove(args[0]);
                        default -> null;
                    };
                }
        );
    }

    private static class StubAuthService
            extends AuthService {

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

            return AifUserEntity.builder()
                    .id(1L)
                    .email("user@example.test")
                    .displayName("User")
                    .passwordHash("hash")
                    .provider("email")
                    .role("USER")
                    .createdAt(Instant.now())
                    .lastLoginAt(Instant.now())
                    .build();
        }
    }
}
