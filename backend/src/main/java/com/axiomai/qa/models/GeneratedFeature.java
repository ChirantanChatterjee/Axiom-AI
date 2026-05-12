package com.axiomai.qa.models;

public class GeneratedFeature {

    private String featureName;

    private String scenarioName;

    private String gherkin;

    public GeneratedFeature() {
    }

    public GeneratedFeature(
            String featureName,
            String scenarioName,
            String gherkin
    ) {

        this.featureName = featureName;
        this.scenarioName = scenarioName;
        this.gherkin = gherkin;
    }

    public String getFeatureName() {
        return featureName;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public String getGherkin() {
        return gherkin;
    }

    public void setGherkin(String gherkin) {
        this.gherkin = gherkin;
    }
}