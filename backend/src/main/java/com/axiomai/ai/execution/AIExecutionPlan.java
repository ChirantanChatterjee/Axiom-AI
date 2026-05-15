package com.axiomai.ai.execution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class AIExecutionPlan {

    // =====================================================
    // TARGET WEBSITE
    // =====================================================

    private String targetUrl;

    // =====================================================
    // EXECUTION SCENARIOS
    // =====================================================

    private List<ScenarioPlan> scenarios;

    // =====================================================
    // RUNTIME VARIABLES
    // =====================================================

    private RuntimeVariableContext variables;

    // =====================================================
    // EXECUTION MODE
    // =====================================================

    private String executionMode;

    // =====================================================
    // HEADLESS
    // =====================================================

    private boolean headless;

    // =====================================================
    // ENABLE OVERLAY RESOLUTION
    // =====================================================

    private boolean overlayHandlingEnabled;

}