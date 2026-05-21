package com.axiomai.runtime.locator;

public class CandidateElement {

    private String selector;

    private double semanticScore;

    private double visibilityScore;

    private double interactabilityScore;

    private double stabilityScore;

    private double finalScore;

    public double calculateFinalScore() {

        finalScore =
                semanticScore
                        + visibilityScore
                        + interactabilityScore
                        + stabilityScore;

        return finalScore;
    }

    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public double getSemanticScore() {
        return semanticScore;
    }

    public void setSemanticScore(double semanticScore) {
        this.semanticScore = semanticScore;
    }

    public double getVisibilityScore() {
        return visibilityScore;
    }

    public void setVisibilityScore(double visibilityScore) {
        this.visibilityScore = visibilityScore;
    }

    public double getInteractabilityScore() {
        return interactabilityScore;
    }

    public void setInteractabilityScore(
            double interactabilityScore) {

        this.interactabilityScore =
                interactabilityScore;
    }

    public double getStabilityScore() {
        return stabilityScore;
    }

    public void setStabilityScore(
            double stabilityScore) {

        this.stabilityScore =
                stabilityScore;
    }

    public double getFinalScore() {
        return finalScore;
    }
}