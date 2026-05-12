package com.axiomai.qa.runtime;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class SmartWaitEngine {

    // =====================================================
    // WAIT FOR PAGE LOAD
    // =====================================================

    public static void waitForPage(
            Page page
    ) {

        try {

            page.waitForLoadState();

            page.waitForTimeout(1500);

        } catch (Exception e) {

            System.out.println(
                    "WAIT PAGE FAILED"
            );
        }
    }

    // =====================================================
    // WAIT FOR ELEMENT
    // =====================================================

    public static boolean waitForElement(
            Locator locator
    ) {

        try {

            locator.waitFor();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    // =====================================================
    // STABILIZATION WAIT
    // =====================================================

    public static void stabilize(
            Page page
    ) {

        try {

            page.waitForTimeout(1000);

        } catch (Exception e) {

            System.out.println(
                    "STABILIZATION FAILED"
            );
        }
    }
}