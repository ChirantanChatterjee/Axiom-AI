package com.axiomai.qa.runtime;

import com.axiomai.ai.runtime.AISemanticExecutionEngine;
import com.axiomai.qa.models.FlowStep;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component

public class AIActionExecutor {

    private static final int MANUAL_AUTH_TIMEOUT_MS =
            180_000;

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

                boolean authCompleted =
                        waitForPasswordStage(page);

                if (
                        authCompleted
                ) {

                    System.out.println(
                            "[MANUAL AUTH] Password step skipped because Google sign-in was completed manually."
                    );

                    return;
                }
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

                boolean authCompleted =
                        waitForPasswordStage(page);

                if (
                        authCompleted
                ) {

                    System.out.println(
                            "[MANUAL AUTH] Password step skipped because Google sign-in was completed manually."
                    );

                    return;
                }

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

                if (
                        isAuthSubmitStep(step)
                                &&
                                isGoogleAuthCompleted(page)
                ) {

                    System.out.println(
                            "[MANUAL AUTH] Submit step skipped because Google sign-in is already complete."
                    );

                    return;
                }

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

    private static boolean waitForPasswordStage(
            Page page
    ) {

        try {

            for (
                    int attempt = 0;
                    attempt < 12;
                    attempt++
            ) {

                if (
                        hasVisiblePasswordField(page)
                ) {

                    System.out.println(
                            "[PASSWORD STAGE] Already visible."
                    );

                    return false;
                }

                if (
                        isGoogleTryAgainChallenge(page)
                ) {

                    return waitForManualGoogleAuth(page);
                }

                page.waitForTimeout(1000);
            }

            System.out.println(
                    "[PASSWORD STAGE] Password field not visible after waiting."
            );

            return false;

        } catch (Exception ignored) {

            if (
                    ignored instanceof RuntimeException runtimeException
            ) {

                throw runtimeException;
            }

            return false;
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
                    null;

            try {

                locator =
                        SelectorFallbackEngine
                                .findLocator(
                                        page,
                                        step
                                );

            } catch (Exception e) {

                System.out.println(
                        "[AI SELECTOR] PRIMARY/FALLBACK UNAVAILABLE -> "
                                + step.getTarget()
                );
            }

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

            if (
                    isGoogleTryAgainChallenge(page)
            ) {

                boolean authCompleted =
                        waitForManualGoogleAuth(page);

                if (
                        authCompleted
                ) {

                    return null;
                }

                throw googleChallengeException();
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

    private static boolean hasVisiblePasswordField(
            Page page
    ) {

        String[] selectors = {

                "input[type='password']",

                "[name='Passwd']",

                "#password",

                "input[autocomplete='current-password']"
        };

        for (
                String selector
                : selectors
        ) {

            try {

                Locator locator =
                        page.locator(selector);

                if (
                        locator.count() > 0
                                &&
                                locator.first()
                                        .isVisible()
                ) {

                    return true;
                }

            } catch (Exception ignored) {
            }
        }

        return false;
    }

    private static boolean isGoogleTryAgainChallenge(
            Page page
    ) {

        try {

            String url =
                    page.url() == null
                            ? ""
                            : page.url()
                            .toLowerCase();

            if (
                    !url.contains("accounts.google")
            ) {

                return false;
            }

            if (
                    hasVisibleTryAgainControl(page)
            ) {

                return true;
            }

            String text =
                    page.locator("body")
                            .innerText(
                                    new Locator.InnerTextOptions()
                                            .setTimeout(1000)
                            )
                            .toLowerCase();

            return text.contains("try again")
                    &&
                    (
                            text.contains("couldn't sign you in")
                                    ||
                                    text.contains("couldn’t sign you in")
                                    ||
                                    text.contains("something went wrong")
                                    ||
                                    text.contains("this browser or app may not be secure")
                    );

        } catch (Exception ignored) {

            return false;
        }
    }

    private static boolean waitForManualGoogleAuth(
            Page page
    ) {

        System.out.println(
                "[MANUAL AUTH] Google is showing a Try again/security challenge. Complete the sign-in manually in the open browser. Automation will wait up to "
                        + (MANUAL_AUTH_TIMEOUT_MS / 1000)
                        + " seconds."
        );

        long deadline =
                System.currentTimeMillis()
                        + MANUAL_AUTH_TIMEOUT_MS;

        while (
                System.currentTimeMillis() < deadline
        ) {

            if (
                    hasVisiblePasswordField(page)
            ) {

                System.out.println(
                        "[MANUAL AUTH] Password field is visible again."
                );

                return false;
            }

            if (
                    isGoogleAuthCompleted(page)
            ) {

                System.out.println(
                        "[MANUAL AUTH] Google sign-in appears complete."
                );

                return true;
            }

            page.waitForTimeout(1000);
        }

        throw googleChallengeException();
    }

    private static RuntimeException googleChallengeException() {

        return new RuntimeException(
                "Google did not show the password field. A Try again/security challenge is visible after username submission. Complete the sign-in manually in the open browser or use a pre-authenticated session; the automation will not bypass this security challenge."
        );
    }

    private static boolean isAuthSubmitStep(
            FlowStep step
    ) {

        if (
                step == null
                        ||
                        step.getTarget() == null
        ) {

            return false;
        }

        String target =
                step.getTarget()
                        .toLowerCase();

        return target.contains("login")
                ||
                target.contains("submit")
                ||
                target.contains("next");
    }

    private static boolean isGoogleAuthCompleted(
            Page page
    ) {

        try {

            String url =
                    page.url() == null
                            ? ""
                            : page.url()
                            .toLowerCase();

            if (
                    url.contains("accounts.google")
            ) {

                return false;
            }

            if (
                    url.contains("youtube.com")
                            &&
                            hasVisibleSignedInYouTubeMarker(page)
            ) {

                return true;
            }

            return url.contains("youtube.com")
                    &&
                    !hasVisibleSignInControl(page);

        } catch (Exception ignored) {

            return false;
        }
    }

    private static boolean hasVisibleSignedInYouTubeMarker(
            Page page
    ) {

        String[] selectors = {

                "button[aria-label*='Account menu']",

                "button[aria-label*='Google Account']",

                "ytd-topbar-menu-button-renderer button#avatar-btn",

                "#avatar-btn"
        };

        for (
                String selector
                : selectors
        ) {

            try {

                Locator locator =
                        page.locator(selector);

                if (
                        locator.count() > 0
                                &&
                                locator.first()
                                        .isVisible()
                ) {

                    return true;
                }

            } catch (Exception ignored) {
            }
        }

        return false;
    }

    private static boolean hasVisibleSignInControl(
            Page page
    ) {

        String[] selectors = {

                "a[aria-label='Sign in']",

                "a:has-text('Sign in')",

                "paper-button:has-text('Sign in')"
        };

        for (
                String selector
                : selectors
        ) {

            try {

                Locator locator =
                        page.locator(selector);

                if (
                        locator.count() > 0
                                &&
                                locator.first()
                                        .isVisible()
                ) {

                    return true;
                }

            } catch (Exception ignored) {
            }
        }

        return false;
    }

    private static boolean hasVisibleTryAgainControl(
            Page page
    ) {

        String[] selectors = {

                "button:has-text('Try again')",

                "a:has-text('Try again')",

                "[role='button']:has-text('Try again')",

                "input[value='Try again']"
        };

        for (
                String selector
                : selectors
        ) {

            try {

                Locator locator =
                        page.locator(selector);

                if (
                        locator.count() > 0
                                &&
                                locator.first()
                                        .isVisible()
                ) {

                    return true;
                }

            } catch (Exception ignored) {
            }
        }

        return false;
    }
}
