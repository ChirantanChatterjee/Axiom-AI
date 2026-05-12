package com.axiomai.qa.flow;

import java.util.List;

public class DetectedFlow {

    // =====================================================
    // FLOW INFO
    // =====================================================

    private FlowType flowType;

    private String pageUrl;

    private List<FlowStep> steps;

    // =====================================================
    // CONSTRUCTORS
    // =====================================================

    public DetectedFlow() {
    }

    public DetectedFlow(
            FlowType flowType,
            String pageUrl,
            List<FlowStep> steps
    ) {

        this.flowType = flowType;
        this.pageUrl = pageUrl;
        this.steps = steps;
    }

    // =====================================================
    // GETTERS / SETTERS
    // =====================================================

    public FlowType getFlowType() {
        return flowType;
    }

    public void setFlowType(
            FlowType flowType
    ) {

        this.flowType = flowType;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(
            String pageUrl
    ) {

        this.pageUrl = pageUrl;
    }

    public List<FlowStep> getSteps() {
        return steps;
    }

    public void setSteps(
            List<FlowStep> steps
    ) {

        this.steps = steps;
    }
}