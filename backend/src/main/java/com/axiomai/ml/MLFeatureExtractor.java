package com.axiomai.ml;

import com.axiomai.ml.config.AIFMLProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MLFeatureExtractor {

    private final SecretRedactionService secretRedactionService;

    private final AIFMLProperties properties;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public String normalizeInput(
            String input
    ) {

        String redacted =
                secretRedactionService.redact(
                        input == null
                                ? ""
                                : input
                );

        String normalized =
                redacted.replaceAll("\\s+", " ")
                        .trim();

        int max =
                Math.max(
                        1_000,
                        properties.getMaxStoredInputChars()
                );

        if (
                normalized.length() <= max
        ) {

            return normalized;
        }

        return normalized.substring(
                0,
                max
        );
    }

    public String metadataJson(
            Map<String, ?> metadata
    ) {

        try {

            return objectMapper.writeValueAsString(
                    secretRedactionService.redactMetadata(metadata)
            );

        } catch (JsonProcessingException e) {

            return "{\"serialization\":\"failed\"}";
        }
    }

    public String inputHash(
            String input
    ) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            normalizeInput(input)
                                    .getBytes(StandardCharsets.UTF_8)
                    );

            StringBuilder builder =
                    new StringBuilder();

            for (
                    byte value
                    : hash
            ) {

                builder.append(
                        String.format(
                                "%02x",
                                value
                        )
                );
            }

            return builder.toString();

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "SHA-256 is not available.",
                    e
            );
        }
    }

    public Set<String> significantTokens(
            String input
    ) {

        String normalized =
                normalizeInput(input)
                        .toLowerCase(Locale.ROOT);

        return Arrays.stream(
                        normalized.split("[^a-z0-9_@.-]+")
                )
                .map(String::trim)
                .filter(token -> token.length() >= 3)
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(
                        Collectors.toCollection(
                                LinkedHashSet::new
                        )
                );
    }

    public double tokenOverlap(
            String left,
            String right
    ) {

        Set<String> leftTokens =
                significantTokens(left);

        Set<String> rightTokens =
                significantTokens(right);

        if (
                leftTokens.isEmpty()
                        ||
                        rightTokens.isEmpty()
        ) {

            return 0.0;
        }

        long matches =
                leftTokens.stream()
                        .filter(rightTokens::contains)
                        .count();

        return (double) matches
                / Math.max(
                leftTokens.size(),
                rightTokens.size()
        );
    }

    private static final Set<String> STOP_WORDS =
            Set.of(
                    "the",
                    "and",
                    "for",
                    "with",
                    "that",
                    "this",
                    "can",
                    "please",
                    "user",
                    "test",
                    "tests",
                    "failed",
                    "failure",
                    "error",
                    "into",
                    "from",
                    "then",
                    "when",
                    "given"
            );
}
