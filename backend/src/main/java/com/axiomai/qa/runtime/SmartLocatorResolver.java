package com.axiomai.qa.runtime;

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

                Locator locator =
                        page.locator(selector);

                int count =
                        locator.count();

                if (count == 1) {

                    if (
                            SmartWaitEngine
                                    .waitForElement(locator)
                    ) {

                        System.out.println(
                                "LOCATOR RESOLVED: "
                                        + selector
                        );

                        return locator;
                    }
                }

                // =========================================
                // MULTIPLE ELEMENTS
                // =========================================

                if (count > 1) {

                    Locator first =
                            locator.first();

                    if (
                            SmartWaitEngine
                                    .waitForElement(first)
                    ) {

                        System.out.println(
                                "MULTIPLE LOCATORS FOUND, USING FIRST: "
                                        + selector
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
}