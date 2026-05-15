package com.axiomai.qa.runtime;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.List;

public class OverlayGuard {

    // =====================================================
    // REMOVE BLOCKING OVERLAYS
    // =====================================================

    public static void clearOverlays(
            Page page
    ) {

        try {

            List<String> overlaySelectors = List.of(

                    // =========================================
                    // YOUTUBE / GOOGLE
                    // =========================================

                    "button:has-text('Accept all')",
                    "button:has-text('I agree')",
                    "button:has-text('Accept')",
                    "button:has-text('Reject all')",

                    // =========================================
                    // COOKIE MODALS
                    // =========================================

                    "[aria-label='Accept all']",
                    "[aria-label='Accept']",

                    // =========================================
                    // COMMON OVERLAY BACKDROPS
                    // =========================================

                    "tp-yt-iron-overlay-backdrop",
                    ".overlay",
                    ".modal-backdrop",
                    ".cookie-banner",
                    "#cookie-banner"

            );

            for (String selector : overlaySelectors) {

                try {

                    Locator locator =
                            page.locator(selector);

                    if (locator.count() > 0) {

                        Locator first =
                                locator.first();

                        if (first.isVisible()) {

                            System.out.println(
                                    "[OVERLAY GUARD] FOUND -> "
                                            + selector
                            );

                            // =============================
                            // CLICKABLE BUTTONS
                            // =============================

                            if (

                                    selector.contains("Accept")
                                            ||
                                            selector.contains("Reject")
                                            ||
                                            selector.contains("agree")

                            ) {

                                try {

                                    first.click(
                                            new Locator.ClickOptions()
                                                    .setTimeout(2000)
                                    );

                                    page.waitForTimeout(1000);

                                    System.out.println(
                                            "[OVERLAY GUARD] DISMISSED"
                                    );

                                } catch (Exception ignored) {
                                }
                            }

                            // =============================
                            // PURE BACKDROP
                            // =============================

                            else {

                                try {

                                    page.evaluate(
                                            """
                                            selector => {
                                                const elements =
                                                    document.querySelectorAll(selector);

                                                elements.forEach(
                                                    e => e.remove()
                                                );
                                            }
                                            """,
                                            selector
                                    );

                                    System.out.println(
                                            "[OVERLAY GUARD] REMOVED BACKDROP"
                                    );

                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }

                } catch (Exception ignored) {
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "[OVERLAY GUARD] FAILED"
            );

            e.printStackTrace();
        }
    }
}