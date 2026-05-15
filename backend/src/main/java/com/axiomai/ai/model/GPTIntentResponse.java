package com.axiomai.ai.model;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class GPTIntentResponse {

    // =====================================================
    // CORE INTENT
    // =====================================================

    private String intent;

    // =====================================================
    // FLOW EXECUTION
    // =====================================================

    private Long flowId;

    private String flowName;

    // =====================================================
    // FRAMEWORK GENERATION
    // =====================================================

    private String frameworkName;

    // =====================================================
    // URL TARGET
    // =====================================================

    private String url;

    // =====================================================
    // REPORTS
    // =====================================================

    private String reportPath;

    // =====================================================
    // RESPONSE
    // =====================================================

    private String responseMessage;

}