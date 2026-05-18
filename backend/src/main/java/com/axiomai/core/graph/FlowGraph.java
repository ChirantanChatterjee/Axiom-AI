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

public class FlowGraph {

    // =====================================================
    // GRAPH INFO
    // =====================================================

    private String graphId;

    private String name;

    private String description;

    private String sourceType;

    private String baseUrl;

    // =====================================================
    // EXECUTION NODES
    // =====================================================

    @Builder.Default
    private List<ActionNode> nodes =
            new ArrayList<>();

    // =====================================================
    // METADATA
    // =====================================================

    private Map<String, Object> metadata;
}