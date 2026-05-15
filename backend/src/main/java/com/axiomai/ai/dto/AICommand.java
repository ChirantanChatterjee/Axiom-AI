package com.axiomai.ai.dto;

import com.axiomai.ai.execution.AIExecutionPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    // RAW MESSAGE
    // =====================================================

    private String message;

    // =====================================================
    // AI EXECUTION PLAN
    // =====================================================

    private AIExecutionPlan executionPlan;

}