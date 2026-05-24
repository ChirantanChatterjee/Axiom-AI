package com.axiomai.audit;

import com.axiomai.audit.entity.AuditLogEntity;
import com.axiomai.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    public static final String OUTCOME_SUCCESS =
            "SUCCESS";

    public static final String OUTCOME_FAILURE =
            "FAILURE";

    public static final String OUTCOME_DENIED =
            "DENIED";

    private static final String REDACTED =
            "<redacted>";

    private static final int MAX_DETAIL_VALUE_LENGTH =
            2048;

    private static final int MAX_DETAILS_JSON_LENGTH =
            12000;

    private final AuditLogRepository repository;

    private final ObjectMapper objectMapper;

    public void recordSuccess(
            String userId,
            String sessionId,
            String action,
            String resourceType,
            String resourceId,
            Map<String, ?> details
    ) {

        record(
                userId,
                sessionId,
                action,
                resourceType,
                resourceId,
                OUTCOME_SUCCESS,
                details
        );
    }

    public void recordFailure(
            String userId,
            String sessionId,
            String action,
            String resourceType,
            String resourceId,
            Map<String, ?> details
    ) {

        record(
                userId,
                sessionId,
                action,
                resourceType,
                resourceId,
                OUTCOME_FAILURE,
                details
        );
    }

    public void recordDenied(
            String userId,
            String sessionId,
            String action,
            String resourceType,
            String resourceId,
            Map<String, ?> details
    ) {

        record(
                userId,
                sessionId,
                action,
                resourceType,
                resourceId,
                OUTCOME_DENIED,
                details
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            String userId,
            String sessionId,
            String action,
            String resourceType,
            String resourceId,
            String outcome,
            Map<String, ?> details
    ) {

        try {

            HttpServletRequest request =
                    currentRequest();

            repository.save(
                    AuditLogEntity.builder()
                            .timestamp(Instant.now())
                            .userId(
                                    limit(
                                            safe(userId),
                                            128
                                    )
                            )
                            .sessionId(
                                    limit(
                                            safe(sessionId),
                                            128
                                    )
                            )
                            .action(
                                    required(
                                            action,
                                            "unknown"
                                    )
                            )
                            .resourceType(
                                    required(
                                            resourceType,
                                            "UNKNOWN"
                                    )
                            )
                            .resourceId(
                                    limit(
                                            safe(resourceId),
                                            512
                                    )
                            )
                            .outcome(
                                    required(
                                            outcome,
                                            OUTCOME_SUCCESS
                                    )
                            )
                            .ipAddress(
                                    ipAddress(request)
                            )
                            .userAgent(
                                    userAgent(request)
                            )
                            .detailsJson(
                                    detailsJson(details)
                            )
                            .build()
            );

        } catch (RuntimeException e) {

            log.warn(
                    "Audit logging failed for action {}: {}",
                    action,
                    e.getMessage()
            );
        }
    }

    public String sanitizedDetailsJson(
            Map<String, ?> details
    ) {

        return detailsJson(details);
    }

    private String detailsJson(
            Map<String, ?> details
    ) {

        if (
                details == null
                        ||
                        details.isEmpty()
        ) {

            return "{}";
        }

        try {

            String json =
                    objectMapper.writeValueAsString(
                            sanitizeMap(details)
                    );

            return limit(
                    json,
                    MAX_DETAILS_JSON_LENGTH
            );

        } catch (JsonProcessingException e) {

            return "{\"serialization\":\"failed\"}";
        }
    }

    private Map<String, Object> sanitizeMap(
            Map<String, ?> source
    ) {

        Map<String, Object> sanitized =
                new LinkedHashMap<>();

        for (
                Map.Entry<String, ?> entry
                : source.entrySet()
        ) {

            String key =
                    entry.getKey() == null
                            ? "unknown"
                            : entry.getKey();

            sanitized.put(
                    key,
                    sanitizeValue(
                            key,
                            entry.getValue()
                    )
            );
        }

        return sanitized;
    }

    private Object sanitizeValue(
            String key,
            Object value
    ) {

        if (
                isSensitiveKey(key)
        ) {

            return REDACTED;
        }

        if (
                value == null
        ) {

            return null;
        }

        if (
                value instanceof Map<?, ?> map
        ) {

            Map<String, Object> nested =
                    new LinkedHashMap<>();

            for (
                    Map.Entry<?, ?> entry
                    : map.entrySet()
            ) {

                String nestedKey =
                        String.valueOf(
                                entry.getKey()
                        );

                nested.put(
                        nestedKey,
                        sanitizeValue(
                                nestedKey,
                                entry.getValue()
                        )
                );
            }

            return nested;
        }

        if (
                value instanceof Iterable<?> iterable
        ) {

            List<Object> nested =
                    new ArrayList<>();

            for (
                    Object item
                    : iterable
            ) {

                nested.add(
                        sanitizeValue(
                                key,
                                item
                        )
                );
            }

            return nested;
        }

        if (
                value instanceof Number
                        ||
                        value instanceof Boolean
        ) {

            return value;
        }

        String text =
                String.valueOf(value);

        if (
                looksLikeSecret(text)
        ) {

            return REDACTED;
        }

        return limit(
                text,
                MAX_DETAIL_VALUE_LENGTH
        );
    }

    private boolean isSensitiveKey(
            String key
    ) {

        String lower =
                key == null
                        ? ""
                        : key.toLowerCase(Locale.ROOT);

        return lower.contains("password")
                ||
                lower.contains("passwd")
                ||
                lower.contains("pwd")
                ||
                lower.contains("token")
                ||
                lower.contains("authorization")
                ||
                lower.contains("auth_header")
                ||
                lower.contains("api_key")
                ||
                lower.contains("apikey")
                ||
                lower.endsWith("key")
                ||
                lower.contains("secret")
                ||
                lower.contains("credential")
                ||
                lower.contains("jwt")
                ||
                lower.contains("supabase")
                ||
                lower.contains("openai")
                ||
                lower.contains("username")
                ||
                lower.contains("test_data")
                ||
                lower.contains("variables");
    }

    private boolean looksLikeSecret(
            String value
    ) {

        if (
                value == null
        ) {

            return false;
        }

        String trimmed =
                value.trim();

        String lower =
                trimmed.toLowerCase(Locale.ROOT);

        return lower.startsWith("bearer ")
                ||
                lower.startsWith("basic ")
                ||
                trimmed.startsWith("sk-")
                ||
                lower.contains("supabase_service_role")
                ||
                lower.contains("openai_api_key")
                ||
                lower.matches(".*eyJ[A-Za-z0-9_-]{20,}\\.[A-Za-z0-9_-]{10,}.*")
                ||
                lower.matches(".*[A-Za-z0-9_-]{32,}\\.[A-Za-z0-9_-]{16,}\\.[A-Za-z0-9_-]{16,}.*");
    }

    private HttpServletRequest currentRequest() {

        RequestAttributes attributes =
                RequestContextHolder.getRequestAttributes();

        if (
                attributes instanceof ServletRequestAttributes servletAttributes
        ) {

            return servletAttributes.getRequest();
        }

        return null;
    }

    private String ipAddress(
            HttpServletRequest request
    ) {

        if (
                request == null
        ) {

            return null;
        }

        String forwardedFor =
                request.getHeader("X-Forwarded-For");

        if (
                forwardedFor != null
                        &&
                        !forwardedFor.isBlank()
        ) {

            return limit(
                    forwardedFor.split(",")[0].trim(),
                    128
            );
        }

        String realIp =
                request.getHeader("X-Real-IP");

        if (
                realIp != null
                        &&
                        !realIp.isBlank()
        ) {

            return limit(
                    realIp.trim(),
                    128
            );
        }

        return limit(
                request.getRemoteAddr(),
                128
        );
    }

    private String userAgent(
            HttpServletRequest request
    ) {

        if (
                request == null
        ) {

            return null;
        }

        return limit(
                request.getHeader("User-Agent"),
                512
        );
    }

    private String required(
            String value,
            String fallback
    ) {

        String cleaned =
                safe(value);

        if (
                cleaned == null
                        ||
                        cleaned.isBlank()
        ) {

            cleaned =
                    fallback;
        }

        return limit(
                cleaned,
                128
        );
    }

    private String safe(
            String value
    ) {

        if (
                value == null
                        ||
                        value.isBlank()
        ) {

            return null;
        }

        return value.trim();
    }

    private String limit(
            String value,
            int maxLength
    ) {

        if (
                value == null
        ) {

            return null;
        }

        if (
                value.length() <= maxLength
        ) {

            return value;
        }

        return value.substring(
                0,
                maxLength
        );
    }
}
