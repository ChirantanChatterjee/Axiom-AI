package com.axiomai.core.runtime;

import com.microsoft.playwright.Page;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class UnifiedRuntimeContext {

    // =====================================================
    // EXECUTION
    // =====================================================

    private String executionId;

    private String flowName;

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
    // LOCATOR MEMORY
    // =====================================================

    @Builder.Default
    private Map<String, String> resolvedLocators =
            new HashMap<>();

    // =====================================================
    // EXECUTED NODES
    // =====================================================

    @Builder.Default
    private List<String> executedNodes =
            new ArrayList<>();

    // =====================================================
    // FAILED NODES
    // =====================================================

    @Builder.Default
    private List<String> failedNodes =
            new ArrayList<>();

    // =====================================================
    // RECOVERY EVENTS
    // =====================================================

    @Builder.Default
    private List<String> recoveryEvents =
            new ArrayList<>();

    // =====================================================
    // SCREENSHOTS
    // =====================================================

    @Builder.Default
    private List<String> screenshots =
            new ArrayList<>();

    // =====================================================
    // METADATA
    // =====================================================

    @Builder.Default
    private Map<String, Object> metadata =
            new HashMap<>();

    // =====================================================
    // STATUS
    // =====================================================

    private String status;

    // =====================================================
    // TIMESTAMPS
    // =====================================================

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}