package com.axiomai.ai.dto;

import com.axiomai.ai.execution.AIExecutionPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class AICommand {

    // =====================================================
    // INTENT
    // =====================================================

    private String intent;

    // =====================================================
    // FLOW NAME
    // =====================================================

    private String flowName;

    // =====================================================
    // TARGET
    // =====================================================

    private String target;

    // =====================================================
    // URL
    // =====================================================

    private String url;

    // =====================================================
    // FEATURE / ARTIFACT
    // =====================================================

    private String featureName;

    private String artifactName;

    // =====================================================
    // CHAT SESSION
    // =====================================================

    private String userId;

    // =====================================================
    // CHAT VARIABLES
    // =====================================================

    @Builder.Default
    private Map<String, String> variables =
            new HashMap<>();

    // =====================================================
    // RAW MESSAGE
    // =====================================================

    private String message;

    // =====================================================
    // AI EXECUTION PLAN
    // =====================================================

    private AIExecutionPlan executionPlan;

    // =====================================================
    // COMPOUND COMMANDS
    // =====================================================

    private List<AICommand> commands;

}
