package com.axiomai.qa.runtime;

import com.axiomai.runtime.wait.SmartWaitEngine;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class SmartLocatorResolver {

    private static Function<List<String>, List<String>> mlSelectorRanker;

    private static BiConsumer<String, List<String>> mlLocatorSuccessRecorder;

    public static void setMlSelectorRanker(
            Function<List<String>, List<String>> selectorRanker
    ) {

        mlSelectorRanker =
                selectorRanker;
    }

    public static void setMlLocatorSuccessRecorder(
            BiConsumer<String, List<String>> locatorSuccessRecorder
    ) {

        mlLocatorSuccessRecorder =
                locatorSuccessRecorder;
    }

    // =====================================================
    // RESOLVE BEST LOCATOR
    // =====================================================

    public static Locator resolve(

            Page page,
            List<String> selectors

    ) {

        return resolve(
                page,
                selectors,
                ""
        );
    }

    public static Locator resolve(

            Page page,
            List<String> selectors,
            String fieldIntent

    ) {

        if (

                selectors == null
                        ||
                        selectors.isEmpty()

        ) {

            throw new RuntimeException(
                    "No selectors provided"
            );

        }

        List<String> rankedSelectors =
                rankedSelectors(selectors);

        for (String selector : rankedSelectors) {

            try {

                System.out.println(
                        "TRYING LOCATOR: "
                                + selector
                );

                Locator locator =
                        page.locator(selector);

                boolean ready =
                        SmartWaitEngine
                                .waitForElementReady(
                                        locator
                                );

                if (!ready) {

                    System.out.println(
                            "ELEMENT NOT READY: "
                                    + selector
                    );

                    continue;

                }

                int count =
                        locator.count();

                // =========================================
                // SINGLE ELEMENT FOUND
                // =========================================

                if (count == 1) {

                    System.out.println(
                            "LOCATOR RESOLVED: "
                                    + selector
                    );

                    recordLocatorSuccess(
                            selector,
                            selectors
                    );

                    return locator;

                }

                // =========================================
                // MULTIPLE ELEMENTS FOUND
                // =========================================

                if (count > 1) {

                    Locator semanticMatch =
                            bestSemanticMatch(
                                    locator,
                                    count,
                                    fieldIntent
                            );

                    if (
                            semanticMatch != null
                    ) {

                        System.out.println(
                                "MULTIPLE ELEMENTS FOUND, USING SEMANTIC MATCH: "
                                        + selector
                        );

                        recordLocatorSuccess(
                                selector,
                                selectors
                        );

                        return semanticMatch;
                    }

                    Locator first =
                            locator.first();

                    boolean firstReady =
                            SmartWaitEngine
                                    .waitForElementReady(
                                            first
                                    );

                    if (firstReady) {

                        System.out.println(
                            "MULTIPLE ELEMENTS FOUND, USING FIRST: "
                                    + selector
                        );

                        recordLocatorSuccess(
                                selector,
                                selectors
                        );

                        return first;

                    }

                }

                System.out.println(
                        "NO MATCH FOUND FOR: "
                                + selector
                );

            } catch (Exception e) {

                System.out.println(
                        "LOCATOR FAILED: "
                                + selector
                );

                System.out.println(
                        "ERROR: "
                                + e.getMessage()
                );

            }

        }

        throw new RuntimeException(
                "No valid locator found after trying all selectors"
        );

    }

    public static double semanticScore(
            String fieldIntent,
            Map<String, String> metadata
    ) {

        String intent =
                normalizeTokens(fieldIntent);

        if (
                intent.isBlank()
                        ||
                        metadata == null
                        ||
                        metadata.isEmpty()
        ) {

            return 0.0;
        }

        String haystack =
                normalizeTokens(
                        String.join(
                                " ",
                                metadata.values()
                        )
                );

        if (
                haystack.isBlank()
        ) {

            return 0.0;
        }

        double score =
                0.0;

        for (
                String token
                : intent.split(" ")
        ) {

            if (
                    token.length() >= 3
                            &&
                            haystack.contains(token)
            ) {

                score += 1.0;
            }
        }

        if (
                haystack.contains(intent)
        ) {

            score += 2.0;
        }

        if (
                intent.contains("multi")
                        &&
                        (
                                haystack.contains("multiple")
                                        ||
                                        haystack.contains("multi")
                        )
        ) {

            score += 2.0;
        }

        if (
                intent.contains("single")
                        &&
                        haystack.contains("single")
        ) {

            score += 2.0;
        }

        return score;
    }

    private static Locator bestSemanticMatch(
            Locator locator,
            int count,
            String fieldIntent
    ) {

        if (
                fieldIntent == null
                        ||
                        fieldIntent.isBlank()
        ) {

            return null;
        }

        double bestScore =
                0.0;

        Locator best =
                null;

        int limit =
                Math.min(
                        count,
                        25
                );

        for (
                int index = 0;
                index < limit;
                index++
        ) {

            try {

                Locator candidate =
                        locator.nth(index);

                if (
                        !SmartWaitEngine.waitForElementReady(candidate)
                ) {

                    continue;
                }

                double score =
                        semanticScore(
                                fieldIntent,
                                elementMetadata(candidate)
                        );

                if (
                        score > bestScore
                ) {

                    bestScore =
                            score;

                    best =
                            candidate;
                }

            } catch (Exception ignored) {

            }
        }

        return bestScore >= 2.0
                ? best
                : null;
    }

    private static Map<String, String> elementMetadata(
            Locator locator
    ) {

        Map<String, String> metadata =
                new LinkedHashMap<>();

        try {

            Object raw =
                    locator.evaluate(
                            """
                                    el => {
                                      const labels = Array.from(document.querySelectorAll('label'))
                                        .filter(label => (el.id && label.htmlFor === el.id) || label.contains(el))
                                        .map(label => (label.innerText || label.textContent || '').trim())
                                        .join(' ');
                                      return {
                                        id: el.id || '',
                                        name: el.getAttribute('name') || '',
                                        placeholder: el.getAttribute('placeholder') || '',
                                        ariaLabel: el.getAttribute('aria-label') || '',
                                        title: el.getAttribute('title') || '',
                                        dataTest: el.getAttribute('data-test') || el.getAttribute('data-testid') || el.getAttribute('data-cy') || '',
                                        role: el.getAttribute('role') || '',
                                        label: labels,
                                        text: (el.innerText || el.textContent || '').trim()
                                      };
                                    }
                                    """
                    );

            if (
                    raw instanceof Map<?, ?> rawMap
            ) {

                rawMap.forEach((key, value) -> metadata.put(
                        String.valueOf(key),
                        value == null
                                ? ""
                                : String.valueOf(value)
                ));
            }

        } catch (Exception ignored) {

        }

        return metadata;
    }

    private static String normalizeTokens(
            String value
    ) {

        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT)
                .replaceAll(
                        "[^a-z0-9]+",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    private static List<String> rankedSelectors(
            List<String> selectors
    ) {

        if (
                mlSelectorRanker == null
        ) {

            return selectors;
        }

        try {

            List<String> ranked =
                    mlSelectorRanker.apply(
                            new ArrayList<>(selectors)
                    );

            return ranked == null
                    ||
                    ranked.isEmpty()
                    ? selectors
                    : ranked;

        } catch (Exception ignored) {

            return selectors;
        }
    }

    private static void recordLocatorSuccess(
            String selector,
            List<String> originalSelectors
    ) {

        if (
                mlLocatorSuccessRecorder == null
                        ||
                        originalSelectors == null
                        ||
                        originalSelectors.indexOf(selector) <= 0
        ) {

            return;
        }

        try {

            mlLocatorSuccessRecorder.accept(
                    selector,
                    new ArrayList<>(originalSelectors)
            );

        } catch (Exception ignored) {

        }
    }

}
