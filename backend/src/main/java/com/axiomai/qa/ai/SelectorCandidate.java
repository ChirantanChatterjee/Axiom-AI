package com.axiomai.qa.ai;

public class SelectorCandidate {

    private SelectorType type;

    private String selector;

    private int score;

    public SelectorCandidate() {
    }

    public SelectorCandidate(
            SelectorType type,
            String selector,
            int score
    ) {

        this.type = type;
        this.selector = selector;
        this.score = score;
    }

    public SelectorType getType() {
        return type;
    }

    public void setType(SelectorType type) {
        this.type = type;
    }

    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}