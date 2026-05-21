package com.axiomai.runtime.dom;

import com.microsoft.playwright.Page;

public class DOMStabilityWatcher {

    public void waitForStableDOM(Page page) {

        try {

            page.waitForLoadState();

            Thread.sleep(800);

        } catch (Exception e) {

            System.out.println(
                    "[DOM STABILITY] WAIT FAILED");
        }
    }
}