package com.axiomai.qa.service;

import com.axiomai.qa.ai.SelectorCandidate;
import com.axiomai.qa.ai.SelectorRanker;
import com.axiomai.qa.ai.SelectorStrategyEngine;
import com.axiomai.qa.models.PageElement;
import com.axiomai.qa.models.PageScanResult;
import com.axiomai.qa.util.ElementClassifier;
import com.axiomai.security.SensitiveLogSanitizer;
import com.microsoft.playwright.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PlaywrightScannerService {

    // =====================================================
    // MAIN SCAN
    // =====================================================

    public PageScanResult scan(String url) {

        List<PageElement> elements =
                new ArrayList<>();

        Set<String> uniqueSelectors =
                new HashSet<>();

        try (

                Playwright playwright =
                        Playwright.create()

        ) {

            Browser browser =
                    playwright.chromium()
                            .launch(

                                    new BrowserType
                                            .LaunchOptions()
                                            .setHeadless(true)

                            );

            Page page =
                    browser.newPage();

            page.navigate(url);

            page.waitForLoadState();

            String title =
                    page.title();

            // =================================================
            // ELEMENT TYPES
            // =================================================

            String[] selectors = {

                    "input",
                    "button",
                    "a",
                    "textarea",
                    "select"
            };

            for (String selector : selectors) {

                List<ElementHandle> handles =
                        page.querySelectorAll(selector);

                for (ElementHandle handle : handles) {

                    try {

                        PageElement element =
                                extractElement(handle);

                        // =====================================
                        // FILTER INVISIBLE
                        // =====================================

                        if (!element.isVisible()) {

                            continue;
                        }

                        // =====================================
                        // FILTER DUPLICATES
                        // =====================================

                        String uniquenessKey =
                                element.getCssSelector()
                                        +
                                        "_"
                                        +
                                        element.getXpath();

                        if (
                                uniqueSelectors.contains(
                                        uniquenessKey
                                )
                        ) {

                            continue;
                        }

                        uniqueSelectors.add(
                                uniquenessKey
                        );

                        // =====================================
                        // CLASSIFY ELEMENT
                        // =====================================

                        ElementClassifier
                                .classify(element);

                        // =====================================
                        // GENERATE AI SELECTORS
                        // =====================================

                        List<SelectorCandidate>
                                selectorCandidates =

                                SelectorStrategyEngine
                                        .generateSelectors(
                                                element
                                        );

                        // =====================================
                        // BEST SELECTOR
                        // =====================================

                        SelectorCandidate bestSelector =
                                SelectorRanker
                                        .getBestSelector(
                                                selectorCandidates
                                        );

                        if (bestSelector != null) {

                            element.setBestSelector(
                                    bestSelector.getSelector()
                            );
                        }

                        // =====================================
                        // FALLBACK SELECTORS
                        // =====================================

                        List<String> fallbackSelectors =
                                new ArrayList<>();

                        for (
                                SelectorCandidate candidate
                                : selectorCandidates
                        ) {

                            fallbackSelectors.add(
                                    candidate.getSelector()
                            );
                        }

                        element.setFallbackSelectors(
                                fallbackSelectors
                        );

                        // =====================================
                        // ADD ELEMENT
                        // =====================================

                        elements.add(element);

                    } catch (Exception e) {

                        System.out.println(
                                "ELEMENT EXTRACTION FAILED"
                        );

                        System.out.println(
                                SensitiveLogSanitizer.redact(
                                        e.getMessage()
                                )
                        );
                    }
                }
            }

            browser.close();

            return new PageScanResult(
                    url,
                    title,
                    elements
            );

        } catch (Exception e) {

            System.out.println(
                    "[PLAYWRIGHT SCAN FAILED] "
                            + SensitiveLogSanitizer.redact(
                            e.getMessage()
                    )
            );

            return new PageScanResult(
                    url,
                    "SCAN FAILED",
                    elements
            );
        }
    }

    // =====================================================
    // EXTRACT ELEMENT
    // =====================================================

    private PageElement extractElement(
            ElementHandle handle
    ) {

        String tag =
                handle.evaluate(
                        "el => el.tagName"
                ).toString();

        String text =
                safeInnerText(handle);

        String id =
                safeAttribute(handle, "id");

        String name =
                safeAttribute(handle, "name");

        String type =
                safeAttribute(handle, "type");

        String value =
                safeAttribute(handle, "value");

        String placeholder =
                safeAttribute(handle, "placeholder");

        String ariaLabel =
                safeAttribute(handle, "aria-label");

        String dataTestId =
                safeAttribute(handle, "data-testid");

        text =
                effectiveElementText(
                        tag,
                        type,
                        text,
                        value,
                        ariaLabel
                );

        boolean visible =
                handle.isVisible();

        boolean clickable =
                detectClickable(
                        tag,
                        type,
                        handle
                );

        String cssSelector =
                buildCssSelector(
                        tag,
                        id,
                        name,
                        type,
                        text
                );

        String xpath =
                buildXpath(
                        tag,
                        id,
                        name
                );

        PageElement element =
                new PageElement(

                        tag,
                        text,
                        id,
                        name,
                        type,
                        placeholder,
                        cssSelector,
                        xpath,

                        visible,
                        clickable,
                        false,
                        0,
                        "",
                        ""
                );

        // =====================================
        // ADVANCED ATTRIBUTES
        // =====================================

        element.setAriaLabel(
                ariaLabel
        );

        element.setDataTestId(
                dataTestId
        );

        return element;
    }

    // =====================================================
    // CLICKABLE DETECTION
    // =====================================================

    private boolean detectClickable(
            String tag,
            String type,
            ElementHandle handle
    ) {

        try {

            tag =
                    tag == null
                            ? ""
                            : tag.toLowerCase();

            type =
                    type == null
                            ? ""
                            : type.toLowerCase();

            if (

                    tag.equals("button")
                            ||

                            tag.equals("a")
                            ||

                            type.equals("submit")
                            ||

                            type.equals("button")
                            ||

                            type.equals("checkbox")
                            ||

                            type.equals("radio")

            ) {

                return true;
            }

            return handle.isEnabled();

        } catch (Exception e) {

            return false;
        }
    }

    // =====================================================
    // SAFE ATTRIBUTE
    // =====================================================

    private String safeAttribute(
            ElementHandle handle,
            String attr
    ) {

        try {

            String value =
                    handle.getAttribute(attr);

            return value == null
                    ? ""
                    : value;

        } catch (Exception e) {

            return "";
        }
    }

    // =====================================================
    // SAFE INNER TEXT
    // =====================================================

    private String safeInnerText(
            ElementHandle handle
    ) {

        try {

            Object value =
                    handle.evaluate(
                            "el => el.innerText"
                    );

            return value == null
                    ? ""
                    : value.toString();

        } catch (Exception e) {

            return "";
        }
    }

    // =====================================================
    // CSS SELECTOR
    // =====================================================

    private String buildCssSelector(
            String tag,
            String id,
            String name,
            String type,
            String text
    ) {

        tag =
                tag == null
                        ? ""
                        : tag.toLowerCase();

        if (
                id != null
                        &&
                        !id.isBlank()
        ) {

            return tag + "#" + id;
        }

        if (
                name != null
                        &&
                        !name.isBlank()
        ) {

            return tag
                    + "[name='"
                    + name
                    + "']";
        }

        if (
                isValueSelectableInput(tag, type)
                        &&
                        text != null
                        &&
                        !text.isBlank()
        ) {

            return tag
                    + "[type='"
                    + cssAttr(type)
                    + "'][value='"
                    + cssAttr(text)
                    + "']";
        }

        return tag;
    }

    private boolean isValueSelectableInput(
            String tag,
            String type
    ) {

        String normalizedTag =
                tag == null
                        ? ""
                        : tag.toLowerCase();

        String normalizedType =
                type == null
                        ? ""
                        : type.toLowerCase();

        return normalizedTag.equals("input")
                &&
                (
                        normalizedType.equals("submit")
                                ||
                                normalizedType.equals("button")
                                ||
                                normalizedType.equals("reset")
                );
    }

    private String effectiveElementText(
            String tag,
            String type,
            String text,
            String value,
            String ariaLabel
    ) {

        String visibleText =
                text == null
                        ? ""
                        : text.trim();

        if (
                !visibleText.isBlank()
        ) {

            return visibleText;
        }

        if (
                isValueSelectableInput(tag, type)
                        &&
                        value != null
                        &&
                        !value.isBlank()
        ) {

            return value.trim();
        }

        if (
                tag != null
                        &&
                        tag.equalsIgnoreCase("button")
                        &&
                        ariaLabel != null
                        &&
                        !ariaLabel.isBlank()
        ) {

            return ariaLabel.trim();
        }

        return "";
    }

    private String cssAttr(
            String value
    ) {

        return value == null
                ? ""
                : value.replace("\\", "\\\\")
                .replace("'", "\\'");
    }

    // =====================================================
    // XPATH
    // =====================================================

    private String buildXpath(
            String tag,
            String id,
            String name
    ) {

        tag =
                tag == null
                        ? ""
                        : tag.toLowerCase();

        if (
                id != null
                        &&
                        !id.isBlank()
        ) {

            return "//"
                    + tag
                    + "[@id='"
                    + id
                    + "']";
        }

        if (
                name != null
                        &&
                        !name.isBlank()
        ) {

            return "//"
                    + tag
                    + "[@name='"
                    + name
                    + "']";
        }

        return "//" + tag;
    }
}
