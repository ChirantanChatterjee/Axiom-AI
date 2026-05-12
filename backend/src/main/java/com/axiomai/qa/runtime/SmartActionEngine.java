package com.axiomai.qa.runtime;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.List;

public class SmartActionEngine {

    // =====================================================
    // SMART CLICK
    // =====================================================

    public static void click(

            Page page,
            List<String> selectors

    ) {

        int retries = 3;

        while (retries > 0) {

            try {

                Locator locator =
                        SmartLocatorResolver.resolve(
                                page,
                                selectors
                        );

                locator.click();

                SmartWaitEngine.stabilize(page);

                System.out.println(
                        "CLICK SUCCESS"
                );

                return;

            } catch (Exception e) {

                retries--;

                System.out.println(
                        "CLICK RETRYING..."
                );

                SmartWaitEngine.stabilize(page);
            }
        }

        throw new RuntimeException(
                "SMART CLICK FAILED"
        );
    }

    // =====================================================
    // SMART TYPE
    // =====================================================

    public static void type(

            Page page,
            List<String> selectors,
            String value

    ) {

        int retries = 3;

        while (retries > 0) {

            try {

                Locator locator =
                        SmartLocatorResolver.resolve(
                                page,
                                selectors
                        );

                locator.fill(value);

                SmartWaitEngine.stabilize(page);

                System.out.println(
                        "TYPE SUCCESS"
                );

                return;

            } catch (Exception e) {

                retries--;

                System.out.println(
                        "TYPE RETRYING..."
                );

                SmartWaitEngine.stabilize(page);
            }
        }

        throw new RuntimeException(
                "SMART TYPE FAILED"
        );
    }
}