package com.axiomai.ai.planner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ScenarioExtractionResult {

    // =====================================================
    // TARGET URL
    // =====================================================

    private String targetUrl;

    // =====================================================
    // SCENARIOS
    // =====================================================

    private List<String> scenarios;

    // =====================================================
    // VARIABLES
    // =====================================================

    private Map<String, String> variables;

}