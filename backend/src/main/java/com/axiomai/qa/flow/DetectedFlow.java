package com.axiomai.qa.flow;

import com.axiomai.qa.models.FlowStep;

import java.util.ArrayList;
import java.util.List;

public class DetectedFlow {

    // =====================================================
    // FLOW TYPE
    // =====================================================

    private String flowType;

    // =====================================================
    // PAGE URL
    // =====================================================

    private String pageUrl;

    // =====================================================
    // STEPS
    // =====================================================

    private List<FlowStep> steps =
            new ArrayList<>();

    // =====================================================
    // CONSTRUCTORS
    // =====================================================

    public DetectedFlow() {
    }

    public DetectedFlow(
            String flowType,
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

    public String getFlowType() {
        return flowType;
    }

    public void setFlowType(
            String flowType
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

    // =====================================================
    // DEBUG
    // =====================================================

    @Override
    public String toString() {

        return "DetectedFlow{" +
                "flowType='" + flowType + '\'' +
                ", pageUrl='" + pageUrl + '\'' +
                ", steps=" + steps +
                '}';
    }
}