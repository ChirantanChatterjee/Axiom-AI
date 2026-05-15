package com.axiomai.qa.runtime;

import com.axiomai.ai.runtime.AISemanticExecutionEngine;
import com.axiomai.qa.models.FlowStep;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component

public class AIActionExecutor {

    // =====================================================
    // OVERLAY RESOLVER
    // =====================================================

    private static AIOverlayResolver overlayResolver;

    // =====================================================
    // SEMANTIC ENGINE
    // =====================================================

    private static AISemanticExecutionEngine
            semanticExecutionEngine;

    // =====================================================
    // SET OVERLAY
    // =====================================================

    @Autowired

    public void setOverlayResolver(
            AIOverlayResolver resolver
    ) {

        overlayResolver = resolver;
    }

    // =====================================================
    // SET SEMANTIC ENGINE
    // =====================================================

    @Autowired

    public void setSemanticExecutionEngine(
            AISemanticExecutionEngine engine
    ) {

        semanticExecutionEngine = engine;
    }

    // =====================================================
    // TYPE ACTION
    // =====================================================

    public static void type(

            Page page,
            FlowStep step,
            String value

    ) {

        try {

            preActionRecovery(page);

            OverlayGuard.clearOverlays(page);

            // =============================================
            // PASSWORD FLOW INTELLIGENCE
            // =============================================

            if (isPasswordStep(step)) {

                ensurePasswordStage(page);
            }

            Locator locator =
                    resolveLocator(
                            page,
                            step
                    );

            // =============================================
            // AUTO TRANSITION RECOVERY
            // =============================================

            if (

                    locator == null
                            &&
                            isPasswordStep(step)

            ) {

                System.out.println(
                        "[AI RECOVERY] Password field missing."
                );

                System.out.println(
                        "[AI RECOVERY] Attempting continue flow..."
                );

                attemptNextButtonFlow(page);

                page.waitForTimeout(3000);

                locator =
                        resolveLocator(
                                page,
                                step
                        );
            }

            if (locator == null) {

                throw new RuntimeException(
                        "Unable to resolve locator for: "
                                + step.getTarget()
                );
            }

            locator.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(10000)
            );

            locator.scrollIntoViewIfNeeded();

            locator.fill("");

            locator.fill(
                    value,
                    new Locator.FillOptions()
                            .setTimeout(10000)
            );

            System.out.println(
                    "[AI ACTION] TYPE SUCCESS -> "
                            + step.getTarget()
            );

        } catch (Exception e) {

            System.out.println(
                    "[AI ACTION] TYPE FAILED -> "
                            + step.getTarget()
            );

            retryOverlayRecovery(page);

            throw new RuntimeException(e);
        }
    }

    // =====================================================
    // CLICK ACTION
    // =====================================================

    public static void click(

            Page page,
            FlowStep step

    ) {

        try {

            preActionRecovery(page);

            OverlayGuard.clearOverlays(page);

            Locator locator =
                    resolveLocator(
                            page,
                            step
                    );

            if (locator == null) {

                throw new RuntimeException(
                        "Unable to resolve locator for: "
                                + step.getTarget()
                );
            }

            locator.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(10000)
            );

            page.waitForTimeout(500);

            locator.click(

                    new Locator.ClickOptions()
                            .setTimeout(10000)

            );

            System.out.println(
                    "[AI ACTION] CLICK SUCCESS -> "
                            + step.getTarget()
            );

        } catch (Exception e) {

            System.out.println(
                    "[AI ACTION] CLICK FAILED -> "
                            + step.getTarget()
            );

            retryOverlayRecovery(page);

            throw new RuntimeException(e);
        }
    }

    // =====================================================
    // PASSWORD STEP DETECTION
    // =====================================================

    private static boolean isPasswordStep(
            FlowStep step
    ) {

        if (step == null) {
            return false;
        }

        String target =
                step.getTarget();

        if (target == null) {
            return false;
        }

        target =
                target.toLowerCase();

        return target.contains("password");
    }

    // =====================================================
    // ENSURE PASSWORD STAGE
    // =====================================================

    private static void ensurePasswordStage(
            Page page
    ) {

        try {

            Locator passwordField =
                    page.locator(
                            "input[type='password']"
                    );

            if (passwordField.count() > 0) {

                System.out.println(
                        "[PASSWORD STAGE] Already visible."
                );

                return;
            }

            System.out.println(
                    "[PASSWORD STAGE] Password field not visible."
            );

        } catch (Exception ignored) {
        }
    }

    // =====================================================
    // NEXT BUTTON FLOW
    // =====================================================

    private static void attemptNextButtonFlow(
            Page page
    ) {

        try {

            String[] nextSelectors = {

                    "#identifierNext",

                    "button:has-text('Next')",

                    "button:has-text('Continue')",

                    "[role='button']:has-text('Next')",

                    "[role='button']:has-text('Continue')",

                    "div[role='button']:has-text('Next')"

            };

            for (String selector : nextSelectors) {

                try {

                    Locator locator =
                            page.locator(selector);

                    if (

                            locator.count() > 0
                                    &&
                                    locator.first().isVisible()

                    ) {

                        System.out.println(
                                "[AI RECOVERY] Clicking next button -> "
                                        + selector
                        );

                        locator.first().click();

                        page.waitForLoadState();

                        page.waitForTimeout(3000);

                        return;
                    }

                } catch (Exception ignored) {
                }
            }

            System.out.println(
                    "[AI RECOVERY] No next button found."
            );

        } catch (Exception ignored) {
        }
    }

    // =====================================================
    // RESOLVE LOCATOR
    // =====================================================

    private static Locator resolveLocator(

            Page page,
            FlowStep step

    ) {

        // =================================================
        // DIRECT SELECTOR
        // =================================================

        if (

                step.getSelector() != null
                        &&
                        !step.getSelector().isBlank()

        ) {

            Locator locator =
                    SelectorFallbackEngine
                            .findLocator(
                                    page,
                                    step
                            );

            if (locator != null) {

                return locator;
            }
        }

        // =================================================
        // PASSWORD SPECIAL FALLBACK
        // =================================================

        if (isPasswordStep(step)) {

            try {

                String[] passwordSelectors = {

                        "input[type='password']",

                        "[name='Passwd']",

                        "#password",

                        "input[autocomplete='current-password']"

                };

                for (String selector : passwordSelectors) {

                    Locator locator =
                            page.locator(selector);

                    if (

                            locator.count() > 0
                                    &&
                                    locator.first().isVisible()

                    ) {

                        System.out.println(
                                "[PASSWORD FALLBACK] "
                                        + selector
                        );

                        return locator.first();
                    }
                }

            } catch (Exception ignored) {
            }
        }

        // =================================================
        // AI SEMANTIC RESOLUTION
        // =================================================

        if (semanticExecutionEngine != null) {

            Locator semantic =
                    semanticExecutionEngine
                            .resolve(
                                    page,
                                    step
                            );

            if (semantic != null) {

                try {

                    if (!semantic.isVisible()) {

                        System.out.println(
                                "[LOCATOR VALIDATION] ELEMENT NOT VISIBLE"
                        );

                        return null;
                    }

                } catch (Exception ignored) {
                }

                System.out.println(
                        "[LOCATOR VALIDATION] SUCCESS"
                );

                return semantic;
            }
        }

        return null;
    }

    // =====================================================
    // PRE ACTION RECOVERY
    // =====================================================

    private static void preActionRecovery(
            Page page
    ) {

        try {

            if (overlayResolver != null) {

                overlayResolver.resolve(page);
            }

            OverlayGuard.clearOverlays(page);

            page.waitForLoadState();

            page.waitForTimeout(1000);

        } catch (Exception ignored) {
        }
    }

    // =====================================================
    // RETRY RECOVERY
    // =====================================================

    private static void retryOverlayRecovery(
            Page page
    ) {

        try {

            System.out.println(
                    "[AI ACTION] Attempting overlay recovery..."
            );

            if (overlayResolver != null) {

                overlayResolver.resolve(page);
            }

            OverlayGuard.clearOverlays(page);

            page.waitForTimeout(1500);

        } catch (Exception ignored) {
        }
    }
}