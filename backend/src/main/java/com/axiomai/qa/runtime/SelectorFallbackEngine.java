package com.axiomai.qa.runtime;

import com.axiomai.qa.ai.LLMElementLocator;
import com.axiomai.qa.models.FlowStep;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.List;

public class SelectorFallbackEngine {

    // =====================================================
    // AI LOCATOR
    // =====================================================

    private static LLMElementLocator llmElementLocator;

    // =====================================================
    // INITIALIZE AI LOCATOR
    // =====================================================

    public static void initialize(
            LLMElementLocator locator
    ) {

        llmElementLocator = locator;
    }

    // =====================================================
    // FIND WORKING LOCATOR
    // =====================================================

    public static Locator findLocator(

            Page page,
            FlowStep step

    ) {

        // =================================================
        // PRIMARY SELECTOR
        // =================================================

        try {

            String primarySelector =
                    step.getSelector();

            if (

                    primarySelector != null
                            &&
                            !primarySelector.isBlank()

            ) {

                Locator locator =
                        page.locator(
                                primarySelector
                        );

                if (locator.count() > 0) {

                    System.out.println(
                            "[AI SELECTOR] PRIMARY SUCCESS -> "
                                    + primarySelector
                    );

                    return locator.first();
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "[AI SELECTOR] PRIMARY FAILED"
            );
        }

        // =================================================
        // FALLBACK SELECTORS
        // =================================================

        List<String> fallbacks =
                step.getFallbackSelectors();

        if (fallbacks != null) {

            for (String fallback : fallbacks) {

                try {

                    if (

                            fallback == null
                                    ||
                                    fallback.isBlank()

                    ) {

                        continue;
                    }

                    Locator locator =
                            page.locator(fallback);

                    if (locator.count() > 0) {

                        System.out.println(
                                "[AI HEALING] FALLBACK SUCCESS -> "
                                        + fallback
                        );

                        return locator.first();
                    }

                } catch (Exception ignored) {

                    System.out.println(
                            "[AI HEALING] FALLBACK FAILED -> "
                                    + fallback
                    );
                }
            }
        }

        // =================================================
        // AI SEMANTIC RECOVERY
        // =================================================

        try {

            if (

                    llmElementLocator != null
                            &&
                            step.getSemanticDescription() != null
                            &&
                            !step.getSemanticDescription().isBlank()

            ) {

                System.out.println(
                        "[AI RECOVERY] TRYING LLM SEMANTIC LOCATION..."
                );

                Locator aiLocator =
                        llmElementLocator.locate(
                                page,
                                step
                        );

                if (aiLocator != null) {

                    System.out.println(
                            "[AI RECOVERY SUCCESS]"
                    );

                    return aiLocator;
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "[AI RECOVERY FAILED]"
            );

            e.printStackTrace();
        }

        // =================================================
        // FAILURE
        // =================================================

        throw new RuntimeException(

                "Unable to locate element for target: "
                        + step.getTarget()
        );
    }
}