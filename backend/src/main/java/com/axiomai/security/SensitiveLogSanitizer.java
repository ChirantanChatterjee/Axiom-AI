package com.axiomai.security;

import java.util.Locale;

public final class SensitiveLogSanitizer {

    private SensitiveLogSanitizer() {
    }

    public static String maskIfSensitive(
            String key,
            String value
    ) {

        if (
                value == null
        ) {

            return null;
        }

        if (
                isSensitiveKey(key)
                        ||
                        looksLikeSecret(value)
        ) {

            return "<redacted>";
        }

        return value;
    }

    public static String redact(
            String value
    ) {

        if (
                value == null
        ) {

            return null;
        }

        String redacted =
                value.replaceAll(
                        "(?i)(password|passwd|pass|token|secret|api[_-]?key|authorization|supabase[_-]?anon[_-]?key|supabase[_-]?service[_-]?role[_-]?key|openai[_-]?api[_-]?key)\\s*(?:[:=]|\\bis\\b|\\bas\\b)\\s*([^\\s,}\\]\"]+|\"[^\"]*\")",
                        "$1=<redacted>"
                );

        redacted =
                redacted.replaceAll(
                        "(?i)Bearer\\s+[A-Za-z0-9._~+/-]+=*",
                        "Bearer <redacted>"
                );

        return redacted;
    }

    public static boolean isSensitiveKey(
            String key
    ) {

        if (
                key == null
                        ||
                        key.isBlank()
        ) {

            return false;
        }

        String lower =
                key.toLowerCase(Locale.ROOT);

        return lower.contains("password")
                ||
                lower.equals("pass")
                ||
                lower.endsWith(" pass")
                ||
                lower.contains("token")
                ||
                lower.contains("secret")
                ||
                lower.contains("api_key")
                ||
                lower.contains("api-key")
                ||
                lower.contains("apikey")
                ||
                lower.contains("authorization")
                ||
                lower.contains("otp")
                ||
                lower.contains("supabase")
                ||
                lower.contains("username")
                ||
                lower.contains("email")
                ||
                lower.equals("user");
    }

    private static boolean looksLikeSecret(
            String value
    ) {

        String trimmed =
                value.trim();

        return trimmed.startsWith("sk-")
                ||
                trimmed.startsWith("sb_secret_")
                ||
                trimmed.matches(
                        "(?i)^Bearer\\s+.+"
                );
    }
}
