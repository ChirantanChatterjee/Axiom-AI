package com.axiomai.ai.runtime;

import com.axiomai.ai.execution.AIExecutionPlan;
import com.axiomai.ai.execution.ScenarioPlan;
import com.axiomai.core.adapter.ScenarioPlanAdapter;
import com.axiomai.core.execution.ExecutionResult;
import com.axiomai.core.graph.FlowGraph;
import com.axiomai.core.runtime.SemanticRuntimeExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor

public class AIExecutionRuntimeExecutor {

    private final ScenarioPlanAdapter
            scenarioPlanAdapter;

    private final SemanticRuntimeExecutor
            semanticRuntimeExecutor;

    // =====================================================
    // EXECUTE PLAN
    // =====================================================

    public ExecutionResult execute(
            AIExecutionPlan plan
    ) {

        if (

                plan.getScenarios() == null
                        ||
                        plan.getScenarios().isEmpty()

        ) {

            throw new RuntimeException(
                    "No scenarios available for execution."
            );
        }

        // =================================================
        // PRIMARY SCENARIO
        // =================================================

        ScenarioPlan scenario =
                plan.getScenarios().get(0);

        // =================================================
        // CONVERT TO FLOW GRAPH
        // =================================================

        FlowGraph graph =

                scenarioPlanAdapter
                        .convert(scenario);

        // =================================================
        // APPLY URL
        // =================================================

        graph.setBaseUrl(
                plan.getTargetUrl()
        );

        // =================================================
        // EXECUTE
        // =================================================

        return semanticRuntimeExecutor
                .execute(graph);
    }
}