package com.axiomai.qa.service;

import com.axiomai.qa.models.*;
import com.axiomai.qa.util.ElementClassifier;

import com.microsoft.playwright.*;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WebsiteCrawlerService {

    private static final int MAX_PAGES = 10;

    public SiteMapResult crawl(String rootUrl) {

        List<PageNode> pages =
                new ArrayList<>();

        Set<String> visited =
                new HashSet<>();

        Queue<String> queue =
                new LinkedList<>();

        queue.add(rootUrl);

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

            while (
                    !queue.isEmpty()
                            &&
                            pages.size() < MAX_PAGES
            ) {

                String currentUrl =
                        queue.poll();

                if (
                        currentUrl == null
                                ||
                                visited.contains(currentUrl)
                ) {
                    continue;
                }

                visited.add(currentUrl);

                System.out.println(
                        "CRAWLING = " + currentUrl
                );

                try {

                    Page page =
                            browser.newPage();

                    page.navigate(currentUrl);

                    page.waitForLoadState();

                    String title =
                            page.title();

                    List<PageElement> elements =
                            scanElements(page);

                    List<PageLink> links =
                            extractLinks(page, currentUrl);

                    PageNode node =
                            new PageNode(
                                    currentUrl,
                                    title,
                                    elements,
                                    links
                            );

                    pages.add(node);

                    for (PageLink link : links) {

                        String href =
                                link.getHref();

                        if (
                                href != null
                                        &&
                                        href.startsWith("http")
                                        &&
                                        !visited.contains(href)
                        ) {

                            queue.add(href);
                        }
                    }

                    page.close();

                } catch (Exception e) {

                    System.out.println(
                            "FAILED PAGE = " + currentUrl
                    );

                    e.printStackTrace();
                }
            }

            browser.close();

        } catch (Exception e) {

            e.printStackTrace();
        }

        return new SiteMapResult(
                rootUrl,
                pages
        );
    }

    // =====================================================
    // SCAN ELEMENTS
    // =====================================================

    private List<PageElement> scanElements(Page page) {

        List<PageElement> elements =
                new ArrayList<>();

        Set<String> uniqueSelectors =
                new HashSet<>();

        List<ElementHandle> handles =
                page.querySelectorAll(
                        "input, button, textarea, select, a"
                );

        for (ElementHandle handle : handles) {

            try {

                String tag =
                        safeEval(handle, "el => el.tagName");

                String text =
                        safeEval(handle, "el => el.innerText");

                String id =
                        safeAttr(handle, "id");

                String name =
                        safeAttr(handle, "name");

                String type =
                        safeAttr(handle, "type");

                String placeholder =
                        safeAttr(handle, "placeholder");

                boolean visible =
                        handle.isVisible();

                String cssSelector =
                        buildCssSelector(
                                tag,
                                id,
                                name
                        );

                String xpath =
                        buildXpath(
                                tag,
                                id,
                                name
                        );

                // =========================================
                // REMOVE DUPLICATES
                // =========================================

                if (
                        uniqueSelectors.contains(cssSelector)
                ) {

                    continue;
                }

                uniqueSelectors.add(cssSelector);

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
                                false,
                                false,
                                0,
                                "",
                                ""
                        );

                // =========================================
                // AI CLASSIFICATION
                // =========================================

                ElementClassifier.classify(element);

                elements.add(element);

            } catch (Exception e) {

                e.printStackTrace();
            }
        }

        return elements;
    }

    // =====================================================
    // EXTRACT LINKS
    // =====================================================

    private List<PageLink> extractLinks(
            Page page,
            String sourceUrl
    ) {

        List<PageLink> links =
                new ArrayList<>();

        List<ElementHandle> anchors =
                page.querySelectorAll("a");

        for (ElementHandle anchor : anchors) {

            try {

                String text =
                        safeEval(anchor, "el => el.innerText");

                String href =
                        anchor.getAttribute("href");

                if (
                        href == null
                                ||
                                href.isBlank()
                ) {

                    continue;
                }

                if (href.startsWith("/")) {

                    href =
                            sourceUrl + href;
                }

                PageLink link =
                        new PageLink(
                                text,
                                href,
                                sourceUrl
                        );

                links.add(link);

            } catch (Exception e) {

                e.printStackTrace();
            }
        }

        return links;
    }

    // =====================================================
    // CSS SELECTOR
    // =====================================================

    private String buildCssSelector(
            String tag,
            String id,
            String name
    ) {

        tag = tag.toLowerCase();

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

        return tag;
    }

    // =====================================================
    // XPATH
    // =====================================================

    private String buildXpath(
            String tag,
            String id,
            String name
    ) {

        tag = tag.toLowerCase();

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

    // =====================================================
    // SAFE ATTRIBUTE
    // =====================================================

    private String safeAttr(
            ElementHandle handle,
            String attr
    ) {

        try {

            String val =
                    handle.getAttribute(attr);

            return val == null
                    ? ""
                    : val;

        } catch (Exception e) {

            return "";
        }
    }

    // =====================================================
    // SAFE EVAL
    // =====================================================

    private String safeEval(
            ElementHandle handle,
            String script
    ) {

        try {

            Object val =
                    handle.evaluate(script);

            return val == null
                    ? ""
                    : val.toString();

        } catch (Exception e) {

            return "";
        }
    }
}