package com.axiomai.qa.models;

public class GeneratedFramework {

    private String featureFile;

    private String pageObject;

    private String stepDefinition;

    public GeneratedFramework() {
    }

    public GeneratedFramework(
            String featureFile,
            String pageObject,
            String stepDefinition
    ) {

        this.featureFile = featureFile;
        this.pageObject = pageObject;
        this.stepDefinition = stepDefinition;
    }

    public String getFeatureFile() {
        return featureFile;
    }

    public void setFeatureFile(String featureFile) {
        this.featureFile = featureFile;
    }

    public String getPageObject() {
        return pageObject;
    }

    public void setPageObject(String pageObject) {
        this.pageObject = pageObject;
    }

    public String getStepDefinition() {
        return stepDefinition;
    }

    public void setStepDefinition(String stepDefinition) {
        this.stepDefinition = stepDefinition;
    }
}