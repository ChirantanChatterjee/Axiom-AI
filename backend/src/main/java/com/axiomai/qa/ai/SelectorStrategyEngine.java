package com.axiomai.qa.ai;

import com.axiomai.qa.models.PageElement;

import java.util.ArrayList;
import java.util.List;

public class SelectorStrategyEngine {

    // =====================================================
    // GENERATE SELECTORS
    // =====================================================

    public static List<SelectorCandidate> generateSelectors(
            PageElement element
    ) {

        List<SelectorCandidate> candidates =
                new ArrayList<>();

        // =====================================================
        // ID
        // =====================================================

        if (
                element.getId() != null
                        &&
                        !element.getId().isBlank()
        ) {

            candidates.add(

                    new SelectorCandidate(

                            SelectorType.ID,

                            "#" + element.getId(),

                            100
                    )
            );
        }

        // =====================================================
        // NAME
        // =====================================================

        if (
                element.getName() != null
                        &&
                        !element.getName().isBlank()
        ) {

            candidates.add(

                    new SelectorCandidate(

                            SelectorType.NAME,

                            element.getTag()
                                    .toLowerCase()
                                    +
                                    "[name='"
                                    +
                                    element.getName()
                                    +
                                    "']",

                            80
                    )
            );
        }

        // =====================================================
        // PLACEHOLDER
        // =====================================================

        if (
                element.getPlaceholder() != null
                        &&
                        !element.getPlaceholder().isBlank()
        ) {

            candidates.add(

                    new SelectorCandidate(

                            SelectorType.PLACEHOLDER,

                            "[placeholder='"
                                    +
                                    element.getPlaceholder()
                                    +
                                    "']",

                            70
                    )
            );
        }

        // =====================================================
        // CSS
        // =====================================================

        if (
                element.getCssSelector() != null
                        &&
                        !element.getCssSelector().isBlank()
        ) {

            candidates.add(

                    new SelectorCandidate(

                            SelectorType.CSS,

                            element.getCssSelector(),

                            50
                    )
            );
        }

        // =====================================================
        // XPATH
        // =====================================================

        if (
                element.getXpath() != null
                        &&
                        !element.getXpath().isBlank()
        ) {

            candidates.add(

                    new SelectorCandidate(

                            SelectorType.XPATH,

                            element.getXpath(),

                            20
                    )
            );
        }

        return candidates;
    }
}