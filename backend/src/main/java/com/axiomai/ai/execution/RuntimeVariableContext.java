package com.axiomai.ai.execution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class RuntimeVariableContext {

    // =====================================================
    // VARIABLES
    // =====================================================

    @Builder.Default
    private Map<String, String> variables =
            new HashMap<>();

    // =====================================================
    // ADD VARIABLE
    // =====================================================

    public void addVariable(

            String key,
            String value

    ) {

        variables.put(
                normalize(key),
                value
        );
    }

    // =====================================================
    // RESOLVE VARIABLE
    // =====================================================

    public String resolve(
            String key
    ) {

        if (key == null) {

            return null;
        }

        return variables.get(
                normalize(key)
        );
    }

    // =====================================================
    // CHECK EXISTS
    // =====================================================

    public boolean contains(
            String key
    ) {

        return variables.containsKey(
                normalize(key)
        );
    }

    // =====================================================
    // NORMALIZE
    // =====================================================

    private String normalize(
            String value
    ) {

        return value == null
                ? ""
                : value
                .trim()
                .toLowerCase();
    }
}