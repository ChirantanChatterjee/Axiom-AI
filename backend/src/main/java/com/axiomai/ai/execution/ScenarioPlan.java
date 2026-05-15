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

public class ScenarioPlan {

    // =====================================================
    // SCENARIO NAME
    // =====================================================

    private String scenarioName;

    // =====================================================
    // SCENARIO DESCRIPTION
    // =====================================================

    private String description;

    // =====================================================
    // EXECUTION ACTIONS
    // =====================================================

    private List<PlannedAction> actions;

    // =====================================================
    // ENABLED
    // =====================================================

    private boolean enabled;

}