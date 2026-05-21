package com.axiomai.qa.runtime;

import com.axiomai.runtime.wait.SmartWaitEngine;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.List;

public class SmartLocatorResolver {

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

        for (String selector : selectors) {

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

}