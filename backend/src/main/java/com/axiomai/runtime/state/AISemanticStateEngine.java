package com.axiomai.runtime.state;

import com.axiomai.runtime.dom.DOMStabilityWatcher;
import com.axiomai.runtime.session.ExecutionSessionContext;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.Page;

public class AISemanticStateEngine {

    private final UIStateDetector stateDetector =
            new UIStateDetector();

    private final DOMStabilityWatcher stabilityWatcher =
            new DOMStabilityWatcher();

    public UIState synchronizeState(
            Page page,
            ExecutionSessionContext context
    ) {

        try {

            page.waitForLoadState(
                    LoadState.DOMCONTENTLOADED);

            stabilityWatcher.waitForStableDOM(page);

            UIState detectedState =
                    stateDetector.detect(page);

            context.setPreviousState(
                    context.getCurrentState());

            context.setCurrentState(
                    detectedState.name());

            System.out.println(
                    "[STATE ENGINE] CURRENT STATE -> "
                            + detectedState);

            return detectedState;

        } catch (Exception e) {

            System.out.println(
                    "[STATE ENGINE] STATE DETECTION FAILED");

            return UIState.UNKNOWN;
        }
    }

    public void waitForExpectedState(
            Page page,
            UIState expectedState,
            ExecutionSessionContext context
    ) {

        long start = System.currentTimeMillis();

        while(System.currentTimeMillis() - start < 15000) {

            try {

                synchronizeState(page, context);

                UIState current =
                        UIState.valueOf(
                                context.getCurrentState());

                if(current == expectedState) {

                    System.out.println(
                            "[STATE ENGINE] EXPECTED STATE REACHED -> "
                                    + expectedState);

                    return;
                }

                Thread.sleep(500);

            } catch (Exception ignored) {

            }
        }

        throw new RuntimeException(
                "Expected state not reached: "
                        + expectedState);
    }

    public void waitForPasswordStage(Page page) {

        long start = System.currentTimeMillis();

        while(System.currentTimeMillis() - start < 10000) {

            try {

                if(page.locator(
                                "input[type='password']")
                        .count() > 0
                ) {

                    if(page.locator(
                                    "input[type='password']")
                            .first()
                            .isVisible()
                    ) {

                        System.out.println(
                                "[STATE ENGINE] PASSWORD FIELD VISIBLE");

                        return;
                    }
                }

                Thread.sleep(500);

            } catch (Exception ignored) {

            }
        }

        throw new RuntimeException(
                "Password stage not reached");
    }
}