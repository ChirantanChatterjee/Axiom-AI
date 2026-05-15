package com.axiomai.qa.service;

import com.axiomai.qa.models.PageElement;
import com.axiomai.qa.models.PageLink;
import com.axiomai.qa.models.PageNode;
import com.axiomai.qa.models.SiteMapResult;
import com.axiomai.qa.util.ElementClassifier;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WebsiteCrawlerService {

    private static final int MAX_PAGES = 3;

    // =====================================================
    // MAIN CRAWLER
    // =====================================================

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

                                            .setHeadless(false)
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
                        "\n================================================="
                );

                System.out.println(
                        "CRAWLING = "
                                + currentUrl
                );

                System.out.println(
                        "=================================================\n"
                );

                try {

                    Page page =
                            browser.newPage();

                    // =============================================
                    // NAVIGATE
                    // =============================================

                    page.navigate(

                            currentUrl,

                            new Page.NavigateOptions()

                                    .setWaitUntil(
                                            WaitUntilState.NETWORKIDLE
                                    )
                    );

                    page.waitForTimeout(3000);

                    System.out.println(
                            "PAGE LOADED = "
                                    + currentUrl
                    );

                    // =============================================
                    // TITLE
                    // =============================================

                    String title =
                            page.title();

                    System.out.println(
                            "TITLE = "
                                    + title
                    );

                    // =============================================
                    // ELEMENT EXTRACTION
                    // =============================================

                    List<PageElement> elements =
                            scanElements(page);

                    System.out.println(
                            "TOTAL ELEMENTS EXTRACTED = "
                                    + elements.size()
                    );

                    // =============================================
                    // LINKS
                    // =============================================

                    List<PageLink> links =
                            extractLinks(
                                    page,
                                    currentUrl
                            );

                    System.out.println(
                            "TOTAL LINKS FOUND = "
                                    + links.size()
                    );

                    // =============================================
                    // PAGE NODE
                    // =============================================

                    PageNode node =
                            new PageNode(

                                    currentUrl,

                                    title,

                                    elements,

                                    links
                            );

                    pages.add(node);

                    // =============================================
                    // QUEUE NEW LINKS
                    // =============================================

                    for (PageLink link : links) {

                        String href =
                                link.getHref();

                        if (

                                href == null
                                        ||
                                        href.isBlank()

                        ) {

                            continue;
                        }

                        if (
                                !href.startsWith("http")
                        ) {

                            continue;
                        }

                        if (
                                visited.contains(href)
                        ) {

                            continue;
                        }

                        // =========================================
                        // SAME DOMAIN ONLY
                        // =========================================

                        if (

                                !href.contains(
                                        extractDomain(rootUrl)
                                )

                        ) {

                            continue;
                        }

                        // =========================================
                        // SKIP NOISY LINKS
                        // =========================================

                        String lower =
                                href.toLowerCase();

                        if (

                                lower.contains("privacy")
                                        ||
                                        lower.contains("terms")
                                        ||
                                        lower.contains("docs")
                                        ||
                                        lower.contains("support")
                                        ||
                                        lower.contains("features")
                                        ||
                                        lower.contains("enterprise")
                                        ||
                                        lower.contains("pricing")
                                        ||
                                        lower.contains("copilot")
                                        ||
                                        lower.contains("marketplace")
                                        ||
                                        lower.contains("about")
                                        ||
                                        lower.contains("blog")
                                        ||
                                        lower.contains("careers")

                        ) {

                            continue;
                        }

                        System.out.println(
                                "QUEUE ADD = "
                                        + href
                        );

                        queue.add(href);
                    }

                    page.close();

                } catch (Exception e) {

                    System.out.println(
                            "FAILED PAGE = "
                                    + currentUrl
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

    private List<PageElement> scanElements(
            Page page
    ) {

        List<PageElement> elements =
                new ArrayList<>();

        Set<String> uniqueSelectors =
                new HashSet<>();

        try {

            List<ElementHandle> handles =

                    page.querySelectorAll(
                            "input, button, textarea, select, a"
                    );

            System.out.println(
                    "RAW ELEMENTS FOUND = "
                            + handles.size()
            );

            for (ElementHandle handle : handles) {

                try {

                    // =========================================
                    // RAW DATA
                    // =========================================

                    String tag =
                            safeEval(
                                    handle,
                                    "el => el.tagName"
                            );

                    String text =
                            safeEval(
                                    handle,
                                    "el => el.innerText"
                            );

                    String id =
                            safeAttr(
                                    handle,
                                    "id"
                            );

                    String name =
                            safeAttr(
                                    handle,
                                    "name"
                            );

                    String type =
                            safeAttr(
                                    handle,
                                    "type"
                            );

                    String placeholder =
                            safeAttr(
                                    handle,
                                    "placeholder"
                            );

                    boolean visible =
                            handle.isVisible();

                    // =========================================
                    // SKIP INVISIBLE
                    // =========================================

                    if (!visible) {

                        continue;
                    }

                    // =========================================
                    // SELECTORS
                    // =========================================

                    String cssSelector =

                            buildCssSelector(

                                    tag,
                                    id,
                                    name,
                                    type
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

                            uniqueSelectors.contains(
                                    cssSelector
                            )

                    ) {

                        continue;
                    }

                    uniqueSelectors.add(cssSelector);

                    // =========================================
                    // PAGE ELEMENT
                    // =========================================

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

                    ElementClassifier
                            .classify(element);

                    // =========================================
                    // DEBUG
                    // =========================================

                    System.out.println(

                            "ELEMENT -> "

                                    + "TAG="
                                    + tag

                                    + " | TEXT="
                                    + text

                                    + " | TYPE="
                                    + type

                                    + " | NAME="
                                    + name

                                    + " | PLACEHOLDER="
                                    + placeholder

                                    + " | ROLE="
                                    + element.getBusinessRole()

                                    + " | SELECTOR="
                                    + cssSelector
                    );

                    elements.add(element);

                } catch (Exception e) {

                    e.printStackTrace();
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
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

        try {

            List<ElementHandle> anchors =
                    page.querySelectorAll("a");

            for (ElementHandle anchor : anchors) {

                try {

                    String text =
                            safeEval(
                                    anchor,
                                    "el => el.innerText"
                            );

                    String href =
                            anchor.getAttribute("href");

                    if (

                            href == null
                                    ||
                                    href.isBlank()

                    ) {

                        continue;
                    }

                    // =========================================
                    // RELATIVE URL
                    // =========================================

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

        } catch (Exception e) {

            e.printStackTrace();
        }

        return links;
    }

    // =====================================================
    // CSS SELECTOR
    // =====================================================

    private String buildCssSelector(

            String tag,
            String id,
            String name,
            String type

    ) {

        tag = safe(tag).toLowerCase();

        if (

                !safe(id).isBlank()

        ) {

            return "#" + id;
        }

        if (

                !safe(name).isBlank()

        ) {

            return tag
                    + "[name='"
                    + name
                    + "']";
        }

        if (

                !safe(type).isBlank()

        ) {

            return tag
                    + "[type='"
                    + type
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

        tag = safe(tag).toLowerCase();

        if (

                !safe(id).isBlank()

        ) {

            return "//"
                    + tag
                    + "[@id='"
                    + id
                    + "']";
        }

        if (

                !safe(name).isBlank()

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
                    : val.trim();

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
                    : val.toString().trim();

        } catch (Exception e) {

            return "";
        }
    }

    // =====================================================
    // EXTRACT DOMAIN
    // =====================================================

    private String extractDomain(
            String url
    ) {

        try {

            String cleaned = url

                    .replace("https://", "")
                    .replace("http://", "")
                    .replace("www.", "");

            return cleaned.split("/")[0];

        } catch (Exception e) {

            return "unknown-domain";
        }
    }

    // =====================================================
    // SAFE
    // =====================================================

    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }
}