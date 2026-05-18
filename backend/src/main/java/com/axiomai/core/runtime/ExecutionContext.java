package com.axiomai.core.runtime;

import com.microsoft.playwright.Page;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ExecutionContext {

    // =====================================================
    // PLAYWRIGHT
    // =====================================================

    private Page page;

    // =====================================================
    // VARIABLES
    // =====================================================

    @Builder.Default
    private Map<String, Object> variables =
            new HashMap<>();

    // =====================================================
    // RESOLVED LOCATORS
    // =====================================================

    @Builder.Default
    private Map<String, String> resolvedLocators =
            new HashMap<>();

    // =====================================================
    // FAILURE HISTORY
    // =====================================================

    @Builder.Default
    private Map<String, Integer> retryHistory =
            new HashMap<>();

    // =====================================================
    // RUNTIME METADATA
    // =====================================================

    @Builder.Default
    private Map<String, Object> runtimeMetadata =
            new HashMap<>();
}