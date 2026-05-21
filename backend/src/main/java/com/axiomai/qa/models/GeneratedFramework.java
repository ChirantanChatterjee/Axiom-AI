package com.axiomai.qa.models;

import java.util.ArrayList;
import java.util.List;

public class GeneratedFramework {

    private String featureFile;

    private String pageObject;

    private String stepDefinition;

    private List<RequirementTestCase> testCases =
            new ArrayList<>();

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

    public List<RequirementTestCase> getTestCases() {
        return testCases;
    }

    public void setTestCases(
            List<RequirementTestCase> testCases
    ) {

        this.testCases = testCases == null
                ? new ArrayList<>()
                : new ArrayList<>(testCases);
    }
}
