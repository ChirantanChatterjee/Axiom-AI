package com.axiomai.qa.models;

public class FlowStep {

    private String action;

    private String target;

    private String selector;

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
}