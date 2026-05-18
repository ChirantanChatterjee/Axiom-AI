package com.axiomai.core.session;

import com.axiomai.core.graph.FlowGraph;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ExecutionSession {

    // =====================================================
    // SESSION
    // =====================================================

    private String sessionId;

    private String userId;

    // =====================================================
    // ACTIVE CONTEXT
    // =====================================================

    private Long activeFlowId;

    private String activeFlowName;

    private String activeProject;

    private String activeUrl;

    // =====================================================
    // CURRENT FLOW GRAPH
    // =====================================================

    private FlowGraph currentFlowGraph;

    // =====================================================
    // LAST EXECUTION
    // =====================================================

    private String lastIntent;

    private String lastAction;

    private String lastReportPath;

    // =====================================================
    // MEMORY
    // =====================================================

    @Builder.Default
    private Map<String, Object> memoryContext =
            new HashMap<>();

    @Builder.Default
    private Map<String, Object> runtimeVariables =
            new HashMap<>();

    // =====================================================
    // STATUS
    // =====================================================

    private String status;

    // =====================================================
    // TIMESTAMPS
    // =====================================================

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}