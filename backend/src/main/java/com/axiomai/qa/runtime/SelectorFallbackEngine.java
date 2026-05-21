package com.axiomai.qa.runtime;

import com.axiomai.qa.ai.LLMElementLocator;
import com.axiomai.qa.models.FlowStep;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
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
        // COMMON TEST ATTRIBUTE VARIANTS
        // =================================================

        for (
                String selector
                : equivalentTestAttributeSelectors(
                step.getSelector()
        )
        ) {

            try {

                Locator locator =
                        page.locator(selector);

                if (locator.count() > 0) {

                    System.out.println(
                            "[AI HEALING] TEST ATTRIBUTE SUCCESS -> "
                                    + selector
                    );

                    return locator.first();
                }

            } catch (Exception ignored) {

                System.out.println(
                        "[AI HEALING] TEST ATTRIBUTE FAILED -> "
                                + selector
                );
            }
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

    private static List<String> equivalentTestAttributeSelectors(
            String selector
    ) {

        List<String> selectors =
                new ArrayList<>();

        if (
                selector == null
                        ||
                        selector.isBlank()
        ) {

            return selectors;
        }

        addVariant(
                selectors,
                selector,
                "data-testid",
                "data-test"
        );

        addVariant(
                selectors,
                selector,
                "data-testid",
                "data-cy"
        );

        addVariant(
                selectors,
                selector,
                "data-test",
                "data-testid"
        );

        addVariant(
                selectors,
                selector,
                "data-test",
                "data-cy"
        );

        addVariant(
                selectors,
                selector,
                "data-cy",
                "data-testid"
        );

        addVariant(
                selectors,
                selector,
                "data-cy",
                "data-test"
        );

        return selectors;
    }

    private static void addVariant(

            List<String> selectors,
            String selector,
            String from,
            String to

    ) {

        String singleQuoted =
                "[" + from + "='";

        String doubleQuoted =
                "[" + from + "=\"";

        if (
                selector.contains(singleQuoted)
        ) {

            selectors.add(
                    selector.replace(
                            singleQuoted,
                            "[" + to + "='"
                    )
            );
        }

        if (
                selector.contains(doubleQuoted)
        ) {

            selectors.add(
                    selector.replace(
                            doubleQuoted,
                            "[" + to + "=\""
                    )
            );
        }
    }
}
