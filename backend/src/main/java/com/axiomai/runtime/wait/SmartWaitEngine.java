package com.axiomai.runtime.wait;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;

public class SmartWaitEngine {

    private static final int DEFAULT_TIMEOUT = 10000;

    private static final int RETRY_COUNT = 3;

    // =====================================================
    // WAIT FOR PAGE READY
    // =====================================================

    public static void waitForPageReady(
            Page page
    ) {

        try {

            page.waitForLoadState(
                    LoadState.DOMCONTENTLOADED
            );

            page.waitForLoadState(
                    LoadState.NETWORKIDLE
            );

            page.waitForTimeout(1000);

            System.out.println(
                    "PAGE READY"
            );

        } catch (Exception e) {

            System.out.println(
                    "WAIT FOR PAGE READY FAILED"
            );

        }

    }

    // =====================================================
    // WAIT FOR NETWORK IDLE
    // =====================================================

    public static void waitForNetworkIdle(
            Page page
    ) {

        try {

            page.waitForLoadState(
                    LoadState.NETWORKIDLE
            );

            System.out.println(
                    "NETWORK IDLE ACHIEVED"
            );

        } catch (Exception e) {

            System.out.println(
                    "NETWORK IDLE WAIT FAILED"
            );

        }

    }

    // =====================================================
    // WAIT FOR ELEMENT VISIBLE
    // =====================================================

    public static boolean waitForVisible(
            Locator locator
    ) {

        try {

            locator.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(DEFAULT_TIMEOUT)
            );

            return locator.isVisible();

        } catch (Exception e) {

            return false;

        }

    }

    // =====================================================
    // WAIT FOR ELEMENT ENABLED
    // =====================================================

    public static boolean waitForEnabled(
            Locator locator
    ) {

        try {

            long start =
                    System.currentTimeMillis();

            while (

                    System.currentTimeMillis() - start
                            < DEFAULT_TIMEOUT

            ) {

                if (locator.isEnabled()) {

                    return true;

                }

                Thread.sleep(300);

            }

            return false;

        } catch (Exception e) {

            return false;

        }

    }

    // =====================================================
    // WAIT FOR CLICKABLE
    // =====================================================

    public static boolean waitForClickable(
            Locator locator
    ) {

        try {

            boolean visible =
                    waitForVisible(locator);

            boolean enabled =
                    waitForEnabled(locator);

            return visible && enabled;

        } catch (Exception e) {

            return false;

        }

    }

    // =====================================================
    // WAIT FOR EDITABLE
    // =====================================================

    public static boolean waitForEditable(
            Locator locator
    ) {

        try {

            boolean visible =
                    waitForVisible(locator);

            boolean enabled =
                    waitForEnabled(locator);

            return visible && enabled;

        } catch (Exception e) {

            return false;

        }

    }

    // =====================================================
    // RETRY UNTIL STABLE
    // =====================================================

    public static boolean retryUntilStable(
            Locator locator
    ) {

        for (int i = 1; i <= RETRY_COUNT; i++) {

            try {

                boolean clickable =
                        waitForClickable(locator);

                if (clickable) {

                    System.out.println(
                            "ELEMENT STABILIZED ON RETRY: "
                                    + i
                    );

                    return true;

                }

                Thread.sleep(1000);

            } catch (Exception e) {

                System.out.println(
                        "RETRY FAILED: "
                                + i
                );

            }

        }

        return false;

    }

    // =====================================================
    // DOM STABILIZATION
    // =====================================================

    public static void stabilizeDOM(
            Page page
    ) {

        try {

            page.waitForTimeout(1000);

            System.out.println(
                    "DOM STABILIZED"
            );

        } catch (Exception e) {

            System.out.println(
                    "DOM STABILIZATION FAILED"
            );

        }

    }

    // =====================================================
    // SMART WAIT FOR ELEMENT READY
    // =====================================================

    public static boolean waitForElementReady(
            Locator locator
    ) {

        try {

            boolean visible =
                    waitForVisible(locator);

            if (!visible) {

                return false;

            }

            boolean stable =
                    retryUntilStable(locator);

            return stable;

        } catch (Exception e) {

            return false;

        }

    }

}