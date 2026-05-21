package com.axiomai.qa.ai;

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

public class ElementSemanticMatch {

    // =====================================================
    // RESOLVED SELECTOR
    // =====================================================

    private String selector;

    // =====================================================
    // WHY MATCHED
    // =====================================================

    private String reasoning;

    // =====================================================
    // CONFIDENCE SCORE
    // =====================================================

    private double confidence;

    // =====================================================
    // MATCH SOURCE
    // =====================================================

    private String source;

    // =====================================================
    // MATCHED BUSINESS ROLE
    // =====================================================

    private String businessRole;

    // =====================================================
    // TARGET INTENT
    // =====================================================

    private String semanticTarget;
}