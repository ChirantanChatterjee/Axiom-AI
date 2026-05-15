package com.axiomai.runtime.assertion;

import com.axiomai.runtime.wait.SmartWaitEngine;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class SmartAssertionEngine {

    // =====================================================
    // VERIFY TEXT
    // =====================================================

    public static AssertionResult verifyText(

            Page page,
            String locator,
            String expectedText

    ) {

        long start =
                System.currentTimeMillis();

        try {

            Locator element =
                    page.locator(locator);

            boolean ready =
                    SmartWaitEngine
                            .waitForElementReady(
                                    element
                            );

            if (!ready) {

                return AssertionResult
                        .builder()
                        .success(false)
                        .assertionType("VERIFY_TEXT")
                        .expectedValue(expectedText)
                        .actualValue("ELEMENT_NOT_READY")
                        .message("Element was not ready")
                        .durationMs(
                                System.currentTimeMillis()
                                        - start
                        )
                        .build();

            }

            String actualText =
                    element.textContent();

            boolean matched =
                    actualText != null
                            &&
                            actualText.trim()
                                    .equals(expectedText.trim());

            return AssertionResult
                    .builder()
                    .success(matched)
                    .assertionType("VERIFY_TEXT")
                    .expectedValue(expectedText)
                    .actualValue(actualText)
                    .message(
                            matched
                                    ? "TEXT MATCHED"
                                    : "TEXT MISMATCH"
                    )
                    .durationMs(
                            System.currentTimeMillis()
                                    - start
                    )
                    .build();

        } catch (Exception e) {

            return AssertionResult
                    .builder()
                    .success(false)
                    .assertionType("VERIFY_TEXT")
                    .expectedValue(expectedText)
                    .actualValue("ERROR")
                    .message(e.getMessage())
                    .durationMs(
                            System.currentTimeMillis()
                                    - start
                    )
                    .build();

        }

    }

    // =====================================================
    // VERIFY ELEMENT VISIBLE
    // =====================================================

    public static AssertionResult verifyVisible(

            Page page,
            String locator

    ) {

        long start =
                System.currentTimeMillis();

        try {

            Locator element =
                    page.locator(locator);

            boolean visible =
                    SmartWaitEngine
                            .waitForVisible(element);

            return AssertionResult
                    .builder()
                    .success(visible)
                    .assertionType("VERIFY_VISIBLE")
                    .expectedValue("VISIBLE")
                    .actualValue(
                            visible
                                    ? "VISIBLE"
                                    : "NOT_VISIBLE"
                    )
                    .message(
                            visible
                                    ? "ELEMENT IS VISIBLE"
                                    : "ELEMENT IS NOT VISIBLE"
                    )
                    .durationMs(
                            System.currentTimeMillis()
                                    - start
                    )
                    .build();

        } catch (Exception e) {

            return AssertionResult
                    .builder()
                    .success(false)
                    .assertionType("VERIFY_VISIBLE")
                    .expectedValue("VISIBLE")
                    .actualValue("ERROR")
                    .message(e.getMessage())
                    .durationMs(
                            System.currentTimeMillis()
                                    - start
                    )
                    .build();

        }

    }

    // =====================================================
    // VERIFY URL
    // =====================================================

    public static AssertionResult verifyUrl(

            Page page,
            String expectedUrl

    ) {

        long start =
                System.currentTimeMillis();

        try {

            String actualUrl =
                    page.url();

            boolean matched =
                    actualUrl.contains(expectedUrl);

            return AssertionResult
                    .builder()
                    .success(matched)
                    .assertionType("VERIFY_URL")
                    .expectedValue(expectedUrl)
                    .actualValue(actualUrl)
                    .message(
                            matched
                                    ? "URL MATCHED"
                                    : "URL MISMATCH"
                    )
                    .durationMs(
                            System.currentTimeMillis()
                                    - start
                    )
                    .build();

        } catch (Exception e) {

            return AssertionResult
                    .builder()
                    .success(false)
                    .assertionType("VERIFY_URL")
                    .expectedValue(expectedUrl)
                    .actualValue("ERROR")
                    .message(e.getMessage())
                    .durationMs(
                            System.currentTimeMillis()
                                    - start
                    )
                    .build();

        }

    }

    // =====================================================
    // VERIFY TITLE
    // =====================================================

    public static AssertionResult verifyTitle(

            Page page,
            String expectedTitle

    ) {

        long start =
                System.currentTimeMillis();

        try {

            String actualTitle =
                    page.title();

            boolean matched =
                    actualTitle.trim()
                            .equals(expectedTitle.trim());

            return AssertionResult
                    .builder()
                    .success(matched)
                    .assertionType("VERIFY_TITLE")
                    .expectedValue(expectedTitle)
                    .actualValue(actualTitle)
                    .message(
                            matched
                                    ? "TITLE MATCHED"
                                    : "TITLE MISMATCH"
                    )
                    .durationMs(
                            System.currentTimeMillis()
                                    - start
                    )
                    .build();

        } catch (Exception e) {

            return AssertionResult
                    .builder()
                    .success(false)
                    .assertionType("VERIFY_TITLE")
                    .expectedValue(expectedTitle)
                    .actualValue("ERROR")
                    .message(e.getMessage())
                    .durationMs(
                            System.currentTimeMillis()
                                    - start
                    )
                    .build();

        }

    }

    // =====================================================
    // VERIFY ELEMENT PRESENT
    // =====================================================

    public static AssertionResult verifyPresent(

            Page page,
            String locator

    ) {

        long start =
                System.currentTimeMillis();

        try {

            Locator element =
                    page.locator(locator);

            int count =
                    element.count();

            boolean present =
                    count > 0;

            return AssertionResult
                    .builder()
                    .success(present)
                    .assertionType("VERIFY_PRESENT")
                    .expectedValue("PRESENT")
                    .actualValue(
                            present
                                    ? "PRESENT"
                                    : "NOT_PRESENT"
                    )
                    .message(
                            present
                                    ? "ELEMENT PRESENT"
                                    : "ELEMENT NOT PRESENT"
                    )
                    .durationMs(
                            System.currentTimeMillis()
                                    - start
                    )
                    .build();

        } catch (Exception e) {

            return AssertionResult
                    .builder()
                    .success(false)
                    .assertionType("VERIFY_PRESENT")
                    .expectedValue("PRESENT")
                    .actualValue("ERROR")
                    .message(e.getMessage())
                    .durationMs(
                            System.currentTimeMillis()
                                    - start
                    )
                    .build();

        }

    }

}