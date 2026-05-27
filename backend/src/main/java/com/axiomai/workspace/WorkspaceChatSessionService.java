package com.axiomai.workspace;

import com.axiomai.auth.entity.AifUserEntity;
import com.axiomai.auth.service.AuthService;
import com.axiomai.workspace.entity.WorkspaceChatSessionEntity;
import com.axiomai.workspace.repository.WorkspaceChatSessionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceChatSessionService {

    private static final TypeReference<List<Map<String, Object>>> MESSAGE_LIST_TYPE =
            new TypeReference<>() {
            };

    private final WorkspaceChatSessionRepository
            repository;

    private final AuthService
            authService;

    private final ObjectMapper
            objectMapper;

    @Transactional(readOnly = true)
    public List<WorkspaceChatSessionDto> listForCurrentUser(
            String token
    ) {

        AifUserEntity user =
                authService.requireUser(token);

        List<WorkspaceChatSessionDto> sessions =
                repository.findByUserIdOrderByUpdatedAtDesc(user.getId())
                .stream()
                .map(this::toDto)
                .toList();

        log.info(
                "Workspace chat sessions listed userId={} email={} count={}",
                user.getId(),
                maskedEmail(user.getEmail()),
                sessions.size()
        );

        return sessions;
    }

    @Transactional
    public WorkspaceChatSessionDto saveForCurrentUser(
            String token,
            String sessionId,
            WorkspaceChatSessionDto request
    ) {

        AifUserEntity user =
                authService.requireUser(token);

        String normalizedSessionId =
                normalizeSessionId(sessionId);

        WorkspaceChatSessionEntity entity =
                repository.findById(normalizedSessionId)
                        .orElseGet(() -> WorkspaceChatSessionEntity.builder()
                                .sessionId(normalizedSessionId)
                                .userId(user.getId())
                                .userEmail(user.getEmail())
                                .createdAt(firstNonNull(
                                        request == null
                                                ? null
                                                : request.getCreatedAt(),
                                        Instant.now()
                                ))
                                .build());

        assertOwner(
                entity,
                user
        );

        Instant now =
                Instant.now();

        entity.setUserEmail(user.getEmail());
        entity.setTitle(
                firstNonBlank(
                        request == null
                                ? null
                                : request.getTitle(),
                        "New chat"
                )
        );
        entity.setWebsiteUrl(
                blankToNull(
                        request == null
                                ? null
                                : request.getWebsiteUrl()
                )
        );
        entity.setDomainName(
                blankToNull(
                        request == null
                                ? null
                                : request.getDomainName()
                )
        );
        entity.setFrameworkLocked(
                request != null
                        &&
                        request.isFrameworkLocked()
        );
        entity.setUpdatedAt(now);
        entity.setMessagesJson(
                serializeMessages(
                        request == null
                                ? null
                                : request.getMessages()
                )
        );

        WorkspaceChatSessionEntity saved =
                repository.save(entity);

        int messageCount =
                deserializeMessages(
                        saved.getMessagesJson()
                )
                        .size();

        log.info(
                "Workspace chat session saved userId={} email={} sessionId={} messageCount={}",
                user.getId(),
                maskedEmail(user.getEmail()),
                saved.getSessionId(),
                messageCount
        );

        return toDto(saved);
    }

    @Transactional
    public WorkspaceChatSessionDto appendMessagesForCurrentUser(
            String token,
            String sessionId,
            WorkspaceChatSessionDto metadata,
            List<Map<String, Object>> newMessages
    ) {

        AifUserEntity user =
                authService.requireUser(token);

        String normalizedSessionId =
                normalizeSessionId(sessionId);

        WorkspaceChatSessionEntity entity =
                findOrCreate(
                        normalizedSessionId,
                        user,
                        metadata
                );

        assertOwner(
                entity,
                user
        );

        entity.setUserEmail(
                user.getEmail()
        );

        List<Map<String, Object>> messages =
                new ArrayList<>(
                        deserializeMessages(
                                entity.getMessagesJson()
                        )
                );

        int messageCountBeforeAppend =
                messages.size();

        if (
                newMessages != null
        ) {

            for (Map<String, Object> message : newMessages) {

                appendIfNotDuplicate(
                        messages,
                        message
                );
            }
        }

        applyMetadata(
                entity,
                metadata,
                messages
        );

        entity.setUpdatedAt(
                Instant.now()
        );
        entity.setMessagesJson(
                serializeMessages(messages)
        );

        WorkspaceChatSessionEntity saved =
                repository.save(entity);

        log.info(
                "Workspace chat session appended userId={} email={} sessionId={} added={} total={}",
                user.getId(),
                maskedEmail(user.getEmail()),
                saved.getSessionId(),
                messages.size()
                        - messageCountBeforeAppend,
                messages.size()
        );

        return toDto(saved);
    }

    @Transactional
    public void delete(
            String sessionId
    ) {

        repository.deleteById(
                normalizeSessionId(sessionId)
        );
    }

    private WorkspaceChatSessionDto toDto(
            WorkspaceChatSessionEntity entity
    ) {

        return WorkspaceChatSessionDto.builder()
                .id(entity.getSessionId())
                .title(entity.getTitle())
                .websiteUrl(entity.getWebsiteUrl())
                .domainName(entity.getDomainName())
                .frameworkLocked(entity.isFrameworkLocked())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .messages(
                        deserializeMessages(
                                entity.getMessagesJson()
                        )
                )
                .build();
    }

    private WorkspaceChatSessionEntity findOrCreate(
            String sessionId,
            AifUserEntity user,
            WorkspaceChatSessionDto metadata
    ) {

        return repository.findById(sessionId)
                .orElseGet(() -> WorkspaceChatSessionEntity.builder()
                        .sessionId(sessionId)
                        .userId(user.getId())
                        .userEmail(user.getEmail())
                        .title("New chat")
                        .frameworkLocked(false)
                        .createdAt(firstNonNull(
                                metadata == null
                                        ? null
                                        : metadata.getCreatedAt(),
                                Instant.now()
                        ))
                        .updatedAt(Instant.now())
                        .messagesJson("[]")
                        .build());
    }

    private void applyMetadata(
            WorkspaceChatSessionEntity entity,
            WorkspaceChatSessionDto metadata,
            List<Map<String, Object>> messages
    ) {

        entity.setTitle(
                firstNonBlank(
                        metadata == null
                                ? null
                                : metadata.getTitle(),
                        titleFromMessages(
                                entity.getTitle(),
                                messages
                        )
                )
        );
        entity.setWebsiteUrl(
                blankToNull(
                        metadata == null
                                ? entity.getWebsiteUrl()
                                : firstNonBlank(
                                        metadata.getWebsiteUrl(),
                                        entity.getWebsiteUrl()
                                )
                )
        );
        entity.setDomainName(
                blankToNull(
                        metadata == null
                                ? entity.getDomainName()
                                : firstNonBlank(
                                        metadata.getDomainName(),
                                        entity.getDomainName()
                                )
                )
        );
        entity.setFrameworkLocked(
                entity.isFrameworkLocked()
                        ||
                        (
                                metadata != null
                                        &&
                                        metadata.isFrameworkLocked()
                        )
        );
    }

    private void appendIfNotDuplicate(
            List<Map<String, Object>> messages,
            Map<String, Object> message
    ) {

        if (
                message == null
                        ||
                        blankToNull(
                                stringValue(
                                        message.get("text")
                                )
                        ) == null
        ) {

            return;
        }

        Map<String, Object> normalized =
                new LinkedHashMap<>(message);

        if (
                !messages.isEmpty()
        ) {

            Map<String, Object> last =
                    messages.get(messages.size() - 1);

            if (
                    stringValue(last.get("sender"))
                            .equals(
                                    stringValue(
                                            normalized.get("sender")
                                    )
                            )
                            &&
                            stringValue(last.get("text"))
                                    .equals(
                                            stringValue(
                                                    normalized.get("text")
                                            )
                                    )
            ) {

                return;
            }
        }

        messages.add(normalized);
    }

    private String serializeMessages(
            List<Map<String, Object>> messages
    ) {

        try {

            return objectMapper.writeValueAsString(
                    messages == null
                            ? List.of()
                            : messages
            );

        } catch (Exception e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unable to save chat messages.",
                    e
            );
        }
    }

    private List<Map<String, Object>> deserializeMessages(
            String messagesJson
    ) {

        if (
                messagesJson == null
                        ||
                        messagesJson.isBlank()
        ) {

            return List.of();
        }

        try {

            return objectMapper.readValue(
                    messagesJson,
                    MESSAGE_LIST_TYPE
            );

        } catch (Exception e) {

            return List.of();
        }
    }

    private String titleFromMessages(
            String currentTitle,
            List<Map<String, Object>> messages
    ) {

        if (
                currentTitle != null
                        &&
                        !currentTitle.isBlank()
                        &&
                        !"New chat".equalsIgnoreCase(currentTitle)
        ) {

            return currentTitle;
        }

        if (
                messages == null
        ) {

            return "New chat";
        }

        for (Map<String, Object> message : messages) {

            if (
                    !"user".equalsIgnoreCase(
                            stringValue(
                                    message.get("sender")
                            )
                    )
            ) {

                continue;
            }

            String text =
                    stringValue(
                            message.get("text")
                    )
                            .replaceAll("\\s+", " ")
                            .trim();

            if (
                    text.isBlank()
            ) {

                continue;
            }

            return text.length() <= 38
                    ? text
                    : text.substring(0, 35)
                    + "...";
        }

        return "New chat";
    }

    private String stringValue(
            Object value
    ) {

        return value == null
                ? ""
                : String.valueOf(value);
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
                                "_"
                        );

        if (
                normalized.isBlank()
                        ||
                        normalized.length() > 128
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chat session id is invalid."
            );
        }

        return normalized;
    }

    private String maskedEmail(
            String email
    ) {

        if (
                email == null
                        ||
                        email.isBlank()
        ) {

            return "";
        }

        int at =
                email.indexOf('@');

        if (
                at <= 1
        ) {

            return "***";
        }

        return email.charAt(0)
                + "***"
                + email.substring(at);
    }

    private void assertOwner(
            WorkspaceChatSessionEntity entity,
            AifUserEntity user
    ) {

        if (
                entity.getUserId() == null
                        ||
                        !entity.getUserId()
                                .equals(user.getId())
        ) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This chat belongs to a different user."
            );
        }
    }

    private String firstNonBlank(
            String value,
            String fallback
    ) {

        if (
                value != null
                        &&
                        !value.isBlank()
        ) {

            return value;
        }

        return fallback;
    }

    private String blankToNull(
            String value
    ) {

        if (
                value == null
                        ||
                        value.isBlank()
        ) {

            return null;
        }

        return value;
    }

    private Instant firstNonNull(
            Instant value,
            Instant fallback
    ) {

        return value == null
                ? fallback
                : value;
    }
}
