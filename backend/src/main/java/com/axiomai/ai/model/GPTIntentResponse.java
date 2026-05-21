package com.axiomai.ai.model;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

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
    // FEATURE / ARTIFACT
    // =====================================================

    private String featureName;

    private String artifactName;

    // =====================================================
    // VARIABLES
    // =====================================================

    @Builder.Default
    private Map<String, String> variables =
            new HashMap<>();

    // =====================================================
    // REPORTS
    // =====================================================

    private String reportPath;

    // =====================================================
    // RESPONSE
    // =====================================================

    private String responseMessage;

}
