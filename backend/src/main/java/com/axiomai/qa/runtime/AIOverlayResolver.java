package com.axiomai.qa.runtime;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component

public class AIOverlayResolver {

    // =====================================================
    // DISMISS KEYWORDS
    // =====================================================

    private static final List<String> DISMISS_WORDS =
            List.of(

                    "accept",
                    "accept all",
                    "agree",
                    "i agree",
                    "allow",
                    "continue",
                    "got it",
                    "dismiss",
                    "close",
                    "skip",
                    "reject",
                    "reject all",
                    "no thanks"

            );

    // =====================================================
    // MAIN RESOLUTION
    // =====================================================

    public boolean resolve(Page page) {

        try {

            OverlayDetectionResult detection =
                    detectOverlay(page);

            if (

                    detection == null
                            ||
                            !detection.isOverlayFound()

            ) {

                return false;
            }

            System.out.println(
                    "[OVERLAY DETECTED] "
                            + detection.getOverlayType()
            );

            OverlayActionCandidate action =
                    findDismissAction(
                            detection.getOverlay()
                    );

            if (action == null) {

                System.out.println(
                        "[OVERLAY] No dismiss action found."
                );

                return false;
            }

            System.out.println(
                    "[OVERLAY ACTION] "
                            + action.getActionName()
            );

            safeClick(action.getLocator());

            waitForOverlayToDisappear(
                    detection.getOverlay()
            );

            System.out.println(
                    "[OVERLAY RESOLVED]"
            );

            return true;

        } catch (Exception e) {

            System.out.println(
                    "[OVERLAY ERROR] "
                            + e.getMessage()
            );

            return false;
        }
    }

    // =====================================================
    // DETECT OVERLAY
    // =====================================================

    public OverlayDetectionResult detectOverlay(
            Page page
    ) {

        try {

            List<Locator> candidates =
                    new ArrayList<>();

            candidates.addAll(
                    findVisibleFixedElements(page)
            );

            candidates.addAll(
                    findModalElements(page)
            );

            for (Locator candidate : candidates) {

                if (!candidate.isVisible()) {

                    continue;
                }

                String text =
                        safeText(candidate);

                if (containsOverlaySignals(text)) {

                    return OverlayDetectionResult
                            .builder()

                            .overlayFound(true)

                            .overlay(candidate)

                            .overlayType("INTERRUPTING_OVERLAY")

                            .detectionReason(
                                    "Semantic overlay keywords detected"
                            )

                            .build();
                }
            }

            return OverlayDetectionResult
                    .builder()
                    .overlayFound(false)
                    .build();

        } catch (Exception e) {

            return OverlayDetectionResult
                    .builder()
                    .overlayFound(false)
                    .build();
        }
    }

    // =====================================================
    // FIND FIXED ELEMENTS
    // =====================================================

    private List<Locator> findVisibleFixedElements(
            Page page
    ) {

        List<Locator> overlays =
                new ArrayList<>();

        try {

            Locator fixedElements =

                    page.locator(

                            """
                            div[style*='position: fixed'],
                            section[style*='position: fixed'],
                            aside[style*='position: fixed']
                            """

                    );

            int count =
                    Math.min(
                            fixedElements.count(),
                            10
                    );

            for (int i = 0; i < count; i++) {

                overlays.add(
                        fixedElements.nth(i)
                );
            }

        } catch (Exception ignored) {
        }

        return overlays;
    }

    // =====================================================
    // FIND MODAL ELEMENTS
    // =====================================================

    private List<Locator> findModalElements(
            Page page
    ) {

        List<Locator> overlays =
                new ArrayList<>();

        try {

            Locator modals =

                    page.locator(

                            """
                            [role='dialog'],
                            [aria-modal='true'],
                            .modal,
                            .popup,
                            .overlay
                            """

                    );

            int count =
                    Math.min(
                            modals.count(),
                            10
                    );

            for (int i = 0; i < count; i++) {

                overlays.add(
                        modals.nth(i)
                );
            }

        } catch (Exception ignored) {
        }

        return overlays;
    }

    // =====================================================
    // OVERLAY SIGNALS
    // =====================================================

    private boolean containsOverlaySignals(
            String text
    ) {

        if (text == null) {

            return false;
        }

        String lower =
                text.toLowerCase();

        return lower.contains("cookie")
                ||
                lower.contains("privacy")
                ||
                lower.contains("gdpr")
                ||
                lower.contains("consent")
                ||
                lower.contains("notification")
                ||
                lower.contains("allow")
                ||
                lower.contains("accept");
    }

    // =====================================================
    // FIND DISMISS ACTION
    // =====================================================

    public OverlayActionCandidate findDismissAction(
            Locator overlay
    ) {

        try {

            for (String keyword : DISMISS_WORDS) {

                Locator button =

                        overlay.locator(

                                String.format(

                                        """
                                        button:has-text('%s')
                                        """,

                                        keyword
                                )

                        );

                if (

                        button.count() > 0
                                &&
                                button.first().isVisible()

                ) {

                    return OverlayActionCandidate
                            .builder()

                            .actionName(keyword)

                            .locator(
                                    button.first()
                            )

                            .confidence(0.95)

                            .build();
                }
            }

        } catch (Exception ignored) {
        }

        return null;
    }

    // =====================================================
    // SAFE CLICK
    // =====================================================

    private void safeClick(
            Locator locator
    ) {

        try {

            locator.click();

            return;

        } catch (Exception ignored) {
        }

        try {

            locator.click(

                    new Locator.ClickOptions()
                            .setForce(true)

            );

            return;

        } catch (Exception ignored) {
        }

        try {

            locator.evaluate(
                    "el => el.click()"
            );

        } catch (Exception ignored) {
        }
    }

    // =====================================================
    // WAIT FOR OVERLAY
    // =====================================================

    private void waitForOverlayToDisappear(
            Locator overlay
    ) {

        try {

            overlay.waitFor(

                    new Locator.WaitForOptions()
                            .setTimeout(5000)
                            .setState(
                                    com.microsoft.playwright.options.WaitForSelectorState.HIDDEN
                            )

            );

        } catch (Exception ignored) {
        }
    }

    // =====================================================
    // SAFE TEXT
    // =====================================================

    private String safeText(
            Locator locator
    ) {

        try {

            return locator.innerText();

        } catch (Exception e) {

            return "";
        }
    }
}