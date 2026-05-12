package com.axiomai.qa.flow;

import java.util.List;

public class FlowStep {

    // =====================================================
    // STEP INFO
    // =====================================================

    private String action;

    private String target;

    // =====================================================
    // PRIMARY SELECTOR
    // =====================================================

    private String selector;

    // =====================================================
    // FALLBACK SELECTORS
    // =====================================================

    private List<String> fallbackSelectors;

    // =====================================================
    // FUTURE AI METADATA
    // =====================================================

    private String businessRole;

    private int confidenceScore;

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

    public FlowStep(

            String action,
            String target,
            String selector,
            List<String> fallbackSelectors

    ) {

        this.action = action;
        this.target = target;
        this.selector = selector;
        this.fallbackSelectors = fallbackSelectors;
    }

    // =====================================================
    // GETTERS / SETTERS
    // =====================================================

    public String getAction() {

        return action;
    }

    public void setAction(String action) {

        this.action = action;
    }

    public String getTarget() {

        return target;
    }

    public void setTarget(String target) {

        this.target = target;
    }

    public String getSelector() {

        return selector;
    }

    public void setSelector(String selector) {

        this.selector = selector;
    }

    public List<String> getFallbackSelectors() {

        return fallbackSelectors;
    }

    public void setFallbackSelectors(
            List<String> fallbackSelectors
    ) {

        this.fallbackSelectors =
                fallbackSelectors;
    }

    public String getBusinessRole() {

        return businessRole;
    }

    public void setBusinessRole(
            String businessRole
    ) {

        this.businessRole =
                businessRole;
    }

    public int getConfidenceScore() {

        return confidenceScore;
    }

    public void setConfidenceScore(
            int confidenceScore
    ) {

        this.confidenceScore =
                confidenceScore;
    }
}