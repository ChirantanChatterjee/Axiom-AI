package com.axiomai.ai.runtime;

import com.axiomai.ai.execution.RuntimeVariableContext;
import org.springframework.stereotype.Component;

@Component

public class RuntimeValueResolver {

    // =====================================================
    // RESOLVE VARIABLE VALUE
    // =====================================================

    public String resolve(

            RuntimeVariableContext context,
            String variableKey

    ) {

        try {

            // =================================================
            // VALIDATION
            // =================================================

            if (

                    context == null
                            ||
                            variableKey == null
                            ||
                            variableKey.isBlank()

            ) {

                return null;
            }

            // =================================================
            // NORMALIZE KEY
            // =================================================

            String normalizedKey =
                    normalize(variableKey);

            // =================================================
            // FETCH VALUE
            // =================================================

            String value =
                    context.resolve(
                            normalizedKey
                    );

            // =================================================
            // NULL CHECK
            // =================================================

            if (
                    value == null
            ) {

                System.out.println(
                        "[VARIABLE RESOLUTION FAILED] "
                                + normalizedKey
                );

                return null;
            }

            // =================================================
            // CLEAN VALUE
            // =================================================

            value = sanitizeValue(value);

            // =================================================
            // DEBUG
            // =================================================

            System.out.println(
                    "[VARIABLE RESOLUTION] "
                            + normalizedKey
                            + " -> "
                            + value
            );

            return value;

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }
    }

    // =====================================================
    // SANITIZE VALUE
    // =====================================================

    private String sanitizeValue(
            String value
    ) {

        if (
                value == null
        ) {

            return null;
        }

        String cleaned =
                value.trim();

        // =================================================
        // REMOVE COMMON TRAILING TOKENS
        // =================================================

        cleaned = cleaned

                .replaceAll(
                        "\\s+password$",
                        ""
                )

                .replaceAll(
                        "\\s+username$",
                        ""
                )

                .replaceAll(
                        "\\s+search$",
                        ""
                )

                .replaceAll(
                        "\\s+text$",
                        ""
                )

                .trim();

        // =================================================
        // REMOVE QUOTES
        // =================================================

        cleaned = cleaned

                .replace("\"", "")
                .replace("'", "")
                .trim();

        return cleaned;
    }

    // =====================================================
    // NORMALIZE
    // =====================================================

    private String normalize(
            String value
    ) {

        return value == null
                ? null
                : value.trim().toLowerCase();
    }
}