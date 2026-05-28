package com.axiomai.workspace;

import com.axiomai.qa.flow.DetectedFlow;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class AutomationSession {

    // =====================================================
    // SESSION
    // =====================================================

    private String sessionId;

    private String userId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // =====================================================
    // WEBSITE CONTEXT
    // =====================================================

    private String websiteUrl;

    private String domainName;

    // =====================================================
    // DETECTED FLOWS
    // =====================================================

    @Builder.Default
    private List<DetectedFlow> detectedFlows =
            new ArrayList<>();

    // =====================================================
    // GENERATED FEATURES
    // =====================================================

    @Builder.Default
    private Map<String, String> generatedFeatures =
            new HashMap<>();

    // =====================================================
    // VARIABLES
    // =====================================================

    @Builder.Default
    private Map<String, WorkspaceVariable> variables =
            new HashMap<>();

    // =====================================================
    // GENERATED ARTIFACTS
    // =====================================================

    @Builder.Default
    private List<GeneratedArtifact> artifacts =
            new ArrayList<>();

    // =====================================================
    // ACTIVE FLOW
    // =====================================================

    private String activeFlowName;

    // =====================================================
    // ACTIVE FEATURE
    // =====================================================

    private String activeFeature;

    // =====================================================
    // PENDING ACTIONS
    // =====================================================

    private String pendingGeneratedTestTagExpression;

    private String lastGeneratedTestTagExpression;

    private String pendingFrameworkGenerationUrl;

    // =====================================================
    // EXECUTION HISTORY
    // =====================================================

    @Builder.Default
    private List<String> executionHistory =
            new ArrayList<>();

    // =====================================================
    // REPORT
    // =====================================================

    private String latestReportPath;
}
