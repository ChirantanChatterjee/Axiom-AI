package com.axiomai.workspace;

import com.axiomai.auth.entity.AifUserEntity;
import com.axiomai.auth.service.AuthService;
import com.axiomai.workspace.entity.WorkspaceChatSessionEntity;
import com.axiomai.workspace.repository.WorkspaceChatSessionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
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

        return repository.findByUserIdOrderByUpdatedAtDesc(user.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public WorkspaceChatSessionDto saveForCurrentUser(
            String token,
            String sessionId,
            WorkspaceChatSessionDto request
    ) {

        AifUserEntity user =
                authService.requireUser(token);

        WorkspaceChatSessionEntity entity =
                repository.findById(sessionId)
                        .orElseGet(() -> WorkspaceChatSessionEntity.builder()
                                .sessionId(sessionId)
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

        return toDto(
                repository.save(entity)
        );
    }

    @Transactional
    public void delete(
            String sessionId
    ) {

        repository.deleteById(sessionId);
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
