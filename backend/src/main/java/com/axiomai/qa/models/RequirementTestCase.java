package com.axiomai.qa.models;

import java.util.ArrayList;
import java.util.List;

public class RequirementTestCase {

    private String tcId;

    private String userStory;

    private String scenario;

    private String testData;

    private String expectedResult;

    private List<String> tags =
            new ArrayList<>();

    public RequirementTestCase() {
    }

    public RequirementTestCase(
            String tcId,
            String userStory,
            String scenario,
            String testData,
            String expectedResult,
            List<String> tags
    ) {

        this.tcId = tcId;
        this.userStory = userStory;
        this.scenario = scenario;
        this.testData = testData;
        this.expectedResult = expectedResult;
        this.tags = tags == null
                ? new ArrayList<>()
                : new ArrayList<>(tags);
    }

    public String getTcId() {
        return tcId;
    }

    public void setTcId(String tcId) {
        this.tcId = tcId;
    }

    public String getUserStory() {
        return userStory;
    }

    public void setUserStory(String userStory) {
        this.userStory = userStory;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public String getTestData() {
        return testData;
    }

    public void setTestData(String testData) {
        this.testData = testData;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null
                ? new ArrayList<>()
                : new ArrayList<>(tags);
    }
}
