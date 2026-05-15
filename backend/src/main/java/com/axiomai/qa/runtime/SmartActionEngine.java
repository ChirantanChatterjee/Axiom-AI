package com.axiomai.qa.runtime;

import com.axiomai.runtime.locator.SmartLocatorResolver;
import com.axiomai.runtime.model.ActionExecutionResult;
import com.axiomai.runtime.wait.SmartWaitEngine;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.List;

public class SmartActionEngine {

    // =====================================================
    // SMART CLICK
    // =====================================================

    public static ActionExecutionResult click(

            Page page,
            List<String> selectors

    ) {

        int retries = 3;

        int retryCounter = 0;

        long start =
                System.currentTimeMillis();

        for (String selector : selectors) {

            retries = 3;

            while (retries > 0) {

                try {

                    Locator locator =
                            SmartLocatorResolver.resolve(
                                    page,
                                    List.of(selector)
                            );

                    SmartWaitEngine
                            .waitForElementReady(locator);

                    locator.click();

                    SmartWaitEngine
                            .stabilizeDOM(page);

                    System.out.println(
                            "CLICK SUCCESS"
                    );

                    String strategy =
                            selectors.indexOf(selector) == 0
                                    ? "PRIMARY"
                                    : "FALLBACK";

                    return ActionExecutionResult
                            .builder()
                            .success(true)
                            .locatorUsed(selector)
                            .locatorStrategy(strategy)
                            .retryCount(retryCounter)
                            .durationMs(
                                    System.currentTimeMillis()
                                            - start
                            )
                            .fallbackUsed(
                                    "FALLBACK".equals(strategy)
                            )
                            .healed(
                                    "FALLBACK".equals(strategy)
                            )
                            .build();

                } catch (Exception e) {

                    retries--;

                    retryCounter++;

                    System.out.println(
                            "CLICK RETRYING..."
                    );

                    SmartWaitEngine
                            .stabilizeDOM(page);
                }
            }
        }

        return ActionExecutionResult
                .builder()
                .success(false)
                .errorMessage(
                        "SMART CLICK FAILED"
                )
                .retryCount(retryCounter)
                .durationMs(
                        System.currentTimeMillis()
                                - start
                )
                .build();
    }

    // =====================================================
    // SMART TYPE
    // =====================================================

    public static ActionExecutionResult type(

            Page page,
            List<String> selectors,
            String value

    ) {

        int retries = 3;

        int retryCounter = 0;

        long start =
                System.currentTimeMillis();

        for (String selector : selectors) {

            retries = 3;

            while (retries > 0) {

                try {

                    Locator locator =
                            SmartLocatorResolver.resolve(
                                    page,
                                    List.of(selector)
                            );

                    SmartWaitEngine
                            .waitForElementReady(locator);

                    locator.fill(value);

                    SmartWaitEngine
                            .stabilizeDOM(page);

                    System.out.println(
                            "TYPE SUCCESS"
                    );

                    String strategy =
                            selectors.indexOf(selector) == 0
                                    ? "PRIMARY"
                                    : "FALLBACK";

                    return ActionExecutionResult
                            .builder()
                            .success(true)
                            .locatorUsed(selector)
                            .locatorStrategy(strategy)
                            .retryCount(retryCounter)
                            .durationMs(
                                    System.currentTimeMillis()
                                            - start
                            )
                            .fallbackUsed(
                                    "FALLBACK".equals(strategy)
                            )
                            .healed(
                                    "FALLBACK".equals(strategy)
                            )
                            .build();

                } catch (Exception e) {

                    retries--;

                    retryCounter++;

                    System.out.println(
                            "TYPE RETRYING..."
                    );

                    SmartWaitEngine
                            .stabilizeDOM(page);
                }
            }
        }

        return ActionExecutionResult
                .builder()
                .success(false)
                .errorMessage(
                        "SMART TYPE FAILED"
                )
                .retryCount(retryCounter)
                .durationMs(
                        System.currentTimeMillis()
                                - start
                )
                .build();
    }

}