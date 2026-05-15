package com.axiomai.runtime.locator;

import com.axiomai.runtime.wait.SmartWaitEngine;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SmartLocatorResolver {

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

                    continue;

                }

                int count =
                        locator.count();

                if (count == 1) {

                    System.out.println(
                            "LOCATOR RESOLVED: "
                                    + selector
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