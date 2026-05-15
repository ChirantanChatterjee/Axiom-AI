package com.axiomai.ai.execution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class PlannedAction {

    // =====================================================
    // ACTION TYPE
    // =====================================================

    private String actionType;

    // =====================================================
    // SEMANTIC TARGET
    // =====================================================

    private String semanticTarget;

    // =====================================================
    // VARIABLE KEY
    // =====================================================

    private String variableKey;

    // =====================================================
    // DESCRIPTION
    // =====================================================

    private String description;

    // =====================================================
    // METADATA
    // =====================================================

    private Map<String, Object> metadata;

}