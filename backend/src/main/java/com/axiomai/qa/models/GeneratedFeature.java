package com.axiomai.qa.models;

public class GeneratedFeature {

    // =====================================================
    // FEATURE INFO
    // =====================================================

    private String featureName;

    private String featureContent;

    // =====================================================
    // CONSTRUCTORS
    // =====================================================

    public GeneratedFeature() {
    }

    public GeneratedFeature(
            String featureName,
            String featureContent
    ) {

        this.featureName = featureName;
        this.featureContent = featureContent;
    }

    // =====================================================
    // GETTERS / SETTERS
    // =====================================================

    public String getFeatureName() {
        return featureName;
    }

    public void setFeatureName(
            String featureName
    ) {

        this.featureName = featureName;
    }

    public String getFeatureContent() {
        return featureContent;
    }

    public void setFeatureContent(
            String featureContent
    ) {

        this.featureContent = featureContent;
    }
}