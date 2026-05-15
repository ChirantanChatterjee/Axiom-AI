package com.axiomai.runtime.locator;

import com.microsoft.playwright.Locator;

public class ElementInteractabilityScorer {

    public double score(Locator locator) {

        try {

            double score = 0;

            if(locator.isVisible()) {
                score += 1.5;
            }

            if(locator.isEnabled()) {
                score += 1.0;
            }

            if(locator.boundingBox() != null) {
                score += 1.0;
            }

            return score;

        } catch (Exception e) {

            return 0;
        }
    }
}