package com.axiomai.ai.runtime;

import com.axiomai.ai.execution.RuntimeVariableContext;
import com.microsoft.playwright.Page;
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

public class RuntimeExecutionContext {

    // =====================================================
    // PLAYWRIGHT PAGE
    // =====================================================

    private Page page;

    // =====================================================
    // VARIABLE CONTEXT
    // =====================================================

    private RuntimeVariableContext variableContext;

    // =====================================================
    // CURRENT SCENARIO
    // =====================================================

    private String currentScenario;

}