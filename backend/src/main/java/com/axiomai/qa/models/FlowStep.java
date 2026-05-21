package com.axiomai.qa.models;

import java.util.ArrayList;
import java.util.List;

public class FlowStep {

    // =====================================================
    // STEP ACTION
    // =====================================================

    private String action;

    // =====================================================
    // TARGET ELEMENT
    // =====================================================

    private String target;

    // =====================================================
    // PRIMARY SELECTOR
    // =====================================================

    private String selector;

    // =====================================================
    // FALLBACK SELECTORS
    // =====================================================

    private List<String> fallbackSelectors =
            new ArrayList<>();

    // =====================================================
    // AI / SEMANTIC INFO
    // =====================================================

    private String semanticDescription;

    private String businessRole;

    private double confidenceScore;

    // =====================================================
    // CONSTRUCTORS
    // =====================================================

    public FlowStep() {
    }

    public FlowStep(
            String action,
            String target,
            String selector
    ) {

        this.action = action;
        this.target = target;
        this.selector = selector;
    }

    // =====================================================
    // ACTION
    // =====================================================

    public String getAction() {
        return action;
    }

    public void setAction(
            String action
    ) {

        this.action = action;
    }

    // =====================================================
    // TARGET
    // =====================================================

    public String getTarget() {
        return target;
    }

    public void setTarget(
            String target
    ) {

        this.target = target;
    }

    // =====================================================
    // SELECTOR
    // =====================================================

    public String getSelector() {
        return selector;
    }

    public void setSelector(
            String selector
    ) {

        this.selector = selector;
    }

    // =====================================================
    // FALLBACK SELECTORS
    // =====================================================

    public List<String> getFallbackSelectors() {
        return fallbackSelectors;
    }

    public void setFallbackSelectors(
            List<String> fallbackSelectors
    ) {

        this.fallbackSelectors = fallbackSelectors;
    }

    // =====================================================
    // SEMANTIC DESCRIPTION
    // =====================================================

    public String getSemanticDescription() {
        return semanticDescription;
    }

    public void setSemanticDescription(
            String semanticDescription
    ) {

        this.semanticDescription =
                semanticDescription;
    }

    // =====================================================
    // BUSINESS ROLE
    // =====================================================

    public String getBusinessRole() {
        return businessRole;
    }

    public void setBusinessRole(
            String businessRole
    ) {

        this.businessRole = businessRole;
    }

    // =====================================================
    // CONFIDENCE SCORE
    // =====================================================

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(
            double confidenceScore
    ) {

        this.confidenceScore =
                confidenceScore;
    }
}