package com.axiomai.qa.service;

import com.axiomai.qa.models.PageElement;
import com.axiomai.qa.models.PageLink;
import com.axiomai.qa.models.PageNode;
import com.axiomai.qa.models.SiteMapResult;
import com.axiomai.qa.runtime.PlaywrightBrowserFactory;
import com.axiomai.qa.util.ElementClassifier;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.stereotype.Service;

import java.net.URI;
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

                    PlaywrightBrowserFactory
                            .launchVisibleChromium(playwright);

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

                    BrowserContext browserContext =
                            browser.newContext(
                                    new Browser.NewContextOptions()
                                            .setViewportSize(null)
                            );

                    Page page =
                            browserContext.newPage();

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
                                !isAllowedCrawlTarget(
                                        rootUrl,
                                        href
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

                    browserContext.close();

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

                    String ariaLabel =
                            safeAttr(
                                    handle,
                                    "aria-label"
                            );

                    String dataTestId =
                            safeAttr(
                                    handle,
                                    "data-testid"
                            );

                    String dataTest =
                            safeAttr(
                                    handle,
                                    "data-test"
                            );

                    String dataCy =
                            safeAttr(
                                    handle,
                                    "data-cy"
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
                                    type,
                                    text,
                                    ariaLabel,
                                    dataTestId,
                                    dataTest,
                                    dataCy
                            );

                    String xpath =

                            buildXpath(

                                    tag,
                                    id,
                                    name,
                                    ariaLabel,
                                    text
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

                    element.setAriaLabel(ariaLabel);
                    element.setDataTestId(
                            firstNonBlank(
                                    dataTestId,
                                    dataTest,
                                    dataCy
                            )
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

                                    + " | ARIA="
                                    + ariaLabel

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
                                resolveUrl(
                                        sourceUrl,
                                        href
                                );
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
            String type,
            String text,
            String ariaLabel,
            String dataTestId,
            String dataTest,
            String dataCy

    ) {

        tag = safe(tag).toLowerCase();

        if (
                !safe(dataTestId).isBlank()
        ) {

            return "[data-testid='"
                    + cssAttr(dataTestId)
                    + "']";
        }

        if (
                !safe(dataTest).isBlank()
        ) {

            return "[data-test='"
                    + cssAttr(dataTest)
                    + "']";
        }

        if (
                !safe(dataCy).isBlank()
        ) {

            return "[data-cy='"
                    + cssAttr(dataCy)
                    + "']";
        }

        if (
                !safe(ariaLabel).isBlank()
        ) {

            return tag
                    + "[aria-label='"
                    + cssAttr(ariaLabel)
                    + "']";
        }

        if (

                !safe(id).isBlank()
                        &&
                        !isGenericId(id)

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
                isTextSelectableTag(tag)
                        &&
                        !selectorText(text).isBlank()
        ) {

            return tag
                    + ":has-text(\""
                    + cssText(selectorText(text))
                    + "\")";
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
            String name,
            String ariaLabel,
            String text

    ) {

        tag = safe(tag).toLowerCase();

        if (

                !safe(id).isBlank()
                        &&
                        !isGenericId(id)

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

        if (
                !safe(ariaLabel).isBlank()
        ) {

            return "//"
                    + tag
                    + "[@aria-label='"
                    + xpathAttr(ariaLabel)
                    + "']";
        }

        if (
                isTextSelectableTag(tag)
                        &&
                        !selectorText(text).isBlank()
        ) {

            return "//"
                    + tag
                    + "[normalize-space()='"
                    + xpathAttr(selectorText(text))
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

            URI uri =
                    URI.create(url);

            if (
                    uri.getHost() != null
            ) {

                return uri.getHost()
                        .replaceFirst(
                                "^www\\.",
                                ""
                        );
            }

            String cleaned = url

                    .replace("https://", "")
                    .replace("http://", "")
                    .replace("www.", "");

            return cleaned.split("/")[0];

        } catch (Exception e) {

            return "unknown-domain";
        }
    }

    private boolean isAllowedCrawlTarget(

            String rootUrl,

            String href

    ) {

        String rootDomain =
                extractDomain(rootUrl);

        String hrefDomain =
                extractDomain(href);

        if (
                rootDomain.equals(hrefDomain)
                        ||
                        hrefDomain.endsWith(
                                "." + rootDomain
                        )
        ) {

            return true;
        }

        return rootDomain.equals("youtube.com")
                &&
                (
                        hrefDomain.equals("accounts.google.com")
                                ||
                                hrefDomain.equals("consent.youtube.com")
                );
    }

    private String resolveUrl(

            String sourceUrl,

            String href

    ) {

        try {

            return URI.create(sourceUrl)
                    .resolve(href)
                    .toString();

        } catch (Exception e) {

            return sourceUrl + href;
        }
    }

    private boolean isGenericId(
            String id
    ) {

        String normalized =
                safe(id).toLowerCase();

        return normalized.equals("button")
                ||
                normalized.equals("endpoint")
                ||
                normalized.equals("text")
                ||
                normalized.equals("content")
                ||
                normalized.equals("contents")
                ||
                normalized.equals("icon")
                ||
                normalized.equals("label")
                ||
                normalized.equals("container");
    }

    private boolean isTextSelectableTag(
            String tag
    ) {

        return "button".equals(tag)
                ||
                "a".equals(tag);
    }

    private String selectorText(
            String text
    ) {

        String cleaned =
                safe(text)
                        .replaceAll(
                                "\\s+",
                                " "
                        )
                        .trim();

        if (
                cleaned.length() > 80
        ) {

            return cleaned.substring(0, 80);
        }

        return cleaned;
    }

    private String cssAttr(
            String value
    ) {

        return safe(value)
                .replace("\\", "\\\\")
                .replace("'", "\\'");
    }

    private String cssText(
            String value
    ) {

        return safe(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private String xpathAttr(
            String value
    ) {

        return safe(value)
                .replace("'", "&apos;");
    }

    private String firstNonBlank(
            String... values
    ) {

        for (
                String value
                : values
        ) {

            if (
                    value != null
                            &&
                            !value.isBlank()
            ) {

                return value;
            }
        }

        return "";
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
