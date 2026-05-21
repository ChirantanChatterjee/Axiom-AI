package com.axiomai.core.execution;

import com.axiomai.core.graph.FlowGraph;
import com.axiomai.core.runtime.SemanticRuntimeExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor

public class GraphExecutionBridge {

    private final SemanticRuntimeExecutor
            semanticRuntimeExecutor;

    // =====================================================
    // EXECUTE FLOW GRAPH
    // =====================================================

    public ExecutionResult execute(
            FlowGraph graph
    ) {

        return semanticRuntimeExecutor
                .execute(graph);
    }
}