package com.axiomai.runtime.locator;

import com.axiomai.runtime.wait.SmartWaitEngine;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Component
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

    public static Locator resolve(

            Page page,
            List<String> selectors

    ) {

        if (

                selectors == null
                        || selectors.isEmpty()

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

                    continue;

                }

                int count =
                        locator.count();

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
                                "MULTIPLE ELEMENTS FOUND, USING FIRST"
                        );

                        recordLocatorSuccess(
                                selector,
                                selectors
                        );

                        return first;

                    }

                }

            } catch (Exception e) {

                System.out.println(
                        "LOCATOR FAILED: "
                                + selector
                );

            }

        }

        throw new RuntimeException(
                "No valid locator found"
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
