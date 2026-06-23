package com.axiomai.ml;

import com.axiomai.security.SensitiveLogSanitizer;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class SecretRedactionService {

    private static final String REDACTED =
            "<redacted>";

    private static final Pattern EMAIL =
            Pattern.compile(
                    "(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}"
            );

    private static final Pattern COOKIE_HEADER =
            Pattern.compile(
                    "(?i)(cookie|set-cookie)\\s*[:=]\\s*[^\\r\\n;]+(?:;[^\\r\\n]+)?"
            );

    private static final Pattern AUTH_HEADER =
            Pattern.compile(
                    "(?i)(authorization|x-api-key|api-key|apikey)\\s*[:=]\\s*[^\\r\\n,}\\]]+"
            );

    private static final Pattern JWT =
            Pattern.compile(
                    "eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}"
            );

    private static final Pattern OPENAI_KEY =
            Pattern.compile(
                    "sk-[A-Za-z0-9_-]{16,}"
            );

    private static final Pattern LONG_SECRET =
            Pattern.compile(
                    "(?i)(password|passwd|pass|token|secret|api[_-]?key|client[_-]?secret)\\s*(?:[:=]|\\bis\\b|\\bas\\b)\\s*(\"[^\"]*\"|'[^']*'|[^\\s,}\\]]+)"
            );

    public String redact(
            String value
    ) {

        if (
                value == null
        ) {

            return null;
        }

        String redacted =
                SensitiveLogSanitizer.redact(value);

        redacted =
                EMAIL.matcher(redacted)
                        .replaceAll(REDACTED);

        redacted =
                COOKIE_HEADER.matcher(redacted)
                        .replaceAll("$1=" + REDACTED);

        redacted =
                AUTH_HEADER.matcher(redacted)
                        .replaceAll("$1=" + REDACTED);

        redacted =
                JWT.matcher(redacted)
                        .replaceAll(REDACTED);

        redacted =
                OPENAI_KEY.matcher(redacted)
                        .replaceAll(REDACTED);

        redacted =
                LONG_SECRET.matcher(redacted)
                        .replaceAll("$1=" + REDACTED);

        return redacted;
    }

    public Map<String, Object> redactMetadata(
            Map<String, ?> metadata
    ) {

        Map<String, Object> redacted =
                new LinkedHashMap<>();

        if (
                metadata == null
                        ||
                        metadata.isEmpty()
        ) {

            return redacted;
        }

        for (
                Map.Entry<String, ?> entry
                : metadata.entrySet()
        ) {

            String key =
                    entry.getKey() == null
                            ? "unknown"
                            : entry.getKey();

            redacted.put(
                    key,
                    redactMetadataValue(
                            key,
                            entry.getValue()
                    )
            );
        }

        return redacted;
    }

    private Object redactMetadataValue(
            String key,
            Object value
    ) {

        if (
                SensitiveLogSanitizer.isSensitiveKey(key)
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
                        redactMetadataValue(
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

            return redactIterable(
                    key,
                    iterable
            );
        }

        if (
                value instanceof Number
                        ||
                        value instanceof Boolean
        ) {

            return value;
        }

        return redact(
                String.valueOf(value)
        );
    }

    private Object redactIterable(
            String key,
            Iterable<?> iterable
    ) {

        java.util.List<Object> values =
                new java.util.ArrayList<>();

        for (
                Object item
                : iterable
        ) {

            values.add(
                    redactMetadataValue(
                            key,
                            item
                    )
            );
        }

        return values;
    }
}
