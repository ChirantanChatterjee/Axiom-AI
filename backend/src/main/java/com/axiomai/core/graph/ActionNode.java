package com.axiomai.core.graph;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ActionNode {

    // =====================================================
    // NODE INFO
    // =====================================================

    private String nodeId;

    private String actionType;

    private String semanticTarget;

    private String description;

    // =====================================================
    // LOCATORS
    // =====================================================

    private String primaryLocator;

    @Builder.Default
    private List<String> fallbackLocators =
            new ArrayList<>();

    // =====================================================
    // DATA
    // =====================================================

    private String inputValue;

    private String expectedValue;

    // =====================================================
    // AI / SEMANTIC
    // =====================================================

    private String businessRole;

    private Double confidenceScore;

    // =====================================================
    // METADATA
    // =====================================================

    private Map<String, Object> metadata;
}