package com.axiomai.qa.models;

import java.util.List;

public class PageElement {

    // =====================================================
    // BASIC ELEMENT INFO
    // =====================================================

    private String tag;

    private String text;

    private String id;

    private String name;

    private String type;

    private String placeholder;

    private String cssSelector;

    private String xpath;

    // =====================================================
    // ADVANCED ATTRIBUTES
    // =====================================================

    private String ariaLabel;

    private String dataTestId;

    // =====================================================
    // AI INTELLIGENCE FIELDS
    // =====================================================

    private boolean visible;

    private boolean clickable;

    private boolean testCandidate;

    private int importanceScore;

    private String businessRole;

    private String recommendedAction;

    // =====================================================
    // AI SELECTOR ENGINE
    // =====================================================

    private String bestSelector;

    private List<String> fallbackSelectors;

    // =====================================================
    // CONSTRUCTORS
    // =====================================================

    public PageElement() {
    }

    public PageElement(
            String tag,
            String text,
            String id,
            String name,
            String type,
            String placeholder,
            String cssSelector,
            String xpath,
            boolean visible,
            boolean clickable,
            boolean testCandidate,
            int importanceScore,
            String businessRole,
            String recommendedAction
    ) {

        this.tag = tag;
        this.text = text;
        this.id = id;
        this.name = name;
        this.type = type;
        this.placeholder = placeholder;
        this.cssSelector = cssSelector;
        this.xpath = xpath;

        this.visible = visible;
        this.clickable = clickable;
        this.testCandidate = testCandidate;
        this.importanceScore = importanceScore;
        this.businessRole = businessRole;
        this.recommendedAction = recommendedAction;
    }

    // =====================================================
    // GETTERS / SETTERS
    // =====================================================

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getCssSelector() {
        return cssSelector;
    }

    public void setCssSelector(String cssSelector) {
        this.cssSelector = cssSelector;
    }

    public String getXpath() {
        return xpath;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    public String getAriaLabel() {
        return ariaLabel;
    }

    public void setAriaLabel(String ariaLabel) {
        this.ariaLabel = ariaLabel;
    }

    public String getDataTestId() {
        return dataTestId;
    }

    public void setDataTestId(String dataTestId) {
        this.dataTestId = dataTestId;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isClickable() {
        return clickable;
    }

    public void setClickable(boolean clickable) {
        this.clickable = clickable;
    }

    public boolean isTestCandidate() {
        return testCandidate;
    }

    public void setTestCandidate(boolean testCandidate) {
        this.testCandidate = testCandidate;
    }

    public int getImportanceScore() {
        return importanceScore;
    }

    public void setImportanceScore(int importanceScore) {
        this.importanceScore = importanceScore;
    }

    public String getBusinessRole() {
        return businessRole;
    }

    public void setBusinessRole(String businessRole) {
        this.businessRole = businessRole;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public String getBestSelector() {
        return bestSelector;
    }

    public void setBestSelector(String bestSelector) {
        this.bestSelector = bestSelector;
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
}