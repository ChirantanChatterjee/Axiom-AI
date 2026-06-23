package com.axiomai.qa.runtime;

import com.axiomai.runtime.wait.SmartWaitEngine;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class SmartLocatorResolver {

    private static Function<List<String>, List<String>> mlSelectorRanker;

    private static BiConsumer<String, List<String>> mlLocatorSuccessRecorder;

    public static void setMlSelectorRanker(
            Function<List<String>, List<String>> selectorRanker
    ) {

        mlSelectorRanker =
                selectorRanker;
    }

    public static void setMlLocatorSuccessRecorder(
            BiConsumer<String, List<String>> locatorSuccessRecorder
    ) {

        mlLocatorSuccessRecorder =
                locatorSuccessRecorder;
    }

    // =====================================================
    // RESOLVE BEST LOCATOR
    // =====================================================

    public static Locator resolve(

            Page page,
            List<String> selectors

    ) {

        if (

                selectors == null
                        ||
                        selectors.isEmpty()

        ) {

            throw new RuntimeException(
                    "No selectors provided"
            );

        }

        List<String> rankedSelectors =
                rankedSelectors(selectors);

        for (String selector : rankedSelectors) {

            try {

                System.out.println(
                        "TRYING LOCATOR: "
                                + selector
                );

                Locator locator =
                        page.locator(selector);

                boolean ready =
                        SmartWaitEngine
                                .waitForElementReady(
                                        locator
                                );

                if (!ready) {

                    System.out.println(
                            "ELEMENT NOT READY: "
                                    + selector
                    );

                    continue;

                }

                int count =
                        locator.count();

                // =========================================
                // SINGLE ELEMENT FOUND
                // =========================================

                if (count == 1) {

                    System.out.println(
                            "LOCATOR RESOLVED: "
                                    + selector
                    );

                    recordLocatorSuccess(
                            selector,
                            selectors
                    );

                    return locator;

                }

                // =========================================
                // MULTIPLE ELEMENTS FOUND
                // =========================================

                if (count > 1) {

                    Locator first =
                            locator.first();

                    boolean firstReady =
                            SmartWaitEngine
                                    .waitForElementReady(
                                            first
                                    );

                    if (firstReady) {

                        System.out.println(
                            "MULTIPLE ELEMENTS FOUND, USING FIRST: "
                                    + selector
                        );

                        recordLocatorSuccess(
                                selector,
                                selectors
                        );

                        return first;

                    }

                }

                System.out.println(
                        "NO MATCH FOUND FOR: "
                                + selector
                );

            } catch (Exception e) {

                System.out.println(
                        "LOCATOR FAILED: "
                                + selector
                );

                System.out.println(
                        "ERROR: "
                                + e.getMessage()
                );

            }

        }

        throw new RuntimeException(
                "No valid locator found after trying all selectors"
        );

    }

    private static List<String> rankedSelectors(
            List<String> selectors
    ) {

        if (
                mlSelectorRanker == null
        ) {

            return selectors;
        }

        try {

            List<String> ranked =
                    mlSelectorRanker.apply(
                            new ArrayList<>(selectors)
                    );

            return ranked == null
                    ||
                    ranked.isEmpty()
                    ? selectors
                    : ranked;

        } catch (Exception ignored) {

            return selectors;
        }
    }

    private static void recordLocatorSuccess(
            String selector,
            List<String> originalSelectors
    ) {

        if (
                mlLocatorSuccessRecorder == null
                        ||
                        originalSelectors == null
                        ||
                        originalSelectors.indexOf(selector) <= 0
        ) {

            return;
        }

        try {

            mlLocatorSuccessRecorder.accept(
                    selector,
                    new ArrayList<>(originalSelectors)
            );

        } catch (Exception ignored) {

        }
    }

}
