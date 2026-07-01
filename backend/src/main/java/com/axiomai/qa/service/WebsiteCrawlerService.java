package com.axiomai.qa.service;

import com.axiomai.qa.models.PageElement;
import com.axiomai.qa.models.PageLink;
import com.axiomai.qa.models.PageNode;
import com.axiomai.qa.models.SiteMapResult;
import com.axiomai.qa.runtime.PlaywrightBrowserFactory;
import com.axiomai.qa.util.ElementClassifier;
import com.axiomai.security.SensitiveLogSanitizer;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.*;

@Service
public class WebsiteCrawlerService {

    private static final int MAX_PAGES = 25;

    // =====================================================
    // MAIN CRAWLER
    // =====================================================

    public SiteMapResult crawl(String rootUrl) {

        return crawl(
                rootUrl,
                Map.of()
        );
    }

    public SiteMapResult crawl(
            String rootUrl,
            Map<String, String> variables
    ) {

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

                    if (
                            pages.size() < MAX_PAGES
                                    &&
                                    maybeSubmitLogin(
                                            page,
                                            variables
                                    )
                    ) {

                        String authenticatedUrl =
                                page.url();

                        if (
                                authenticatedUrl != null
                                        &&
                                        !authenticatedUrl.isBlank()
                                        &&
                                        !visited.contains(authenticatedUrl)
                        ) {

                            visited.add(authenticatedUrl);

                            List<PageElement> authenticatedElements =
                                    scanElements(page);

                            List<PageLink> authenticatedLinks =
                                    extractLinks(
                                            page,
                                            authenticatedUrl
                                    );

                            pages.add(
                                    new PageNode(
                                            authenticatedUrl,
                                            page.title(),
                                            authenticatedElements,
                                            authenticatedLinks
                                    )
                            );

                            for (
                                    PageLink link
                                    : authenticatedLinks
                            ) {

                                maybeQueueLink(
                                        rootUrl,
                                        link,
                                        visited,
                                        queue
                                );
                            }
                        }
                    }

                    // =============================================
                    // QUEUE NEW LINKS
                    // =============================================

                    for (PageLink link : links) {

                        maybeQueueLink(
                                rootUrl,
                                link,
                                visited,
                                queue
                        );
                    }

                    page.close();

                    browserContext.close();

                } catch (Exception e) {

                    System.out.println(
                            "FAILED PAGE = "
                                    + currentUrl
                    );

                    System.out.println(
                            SensitiveLogSanitizer.redact(
                                    e.getMessage()
                            )
                    );
                }
            }

            browser.close();

        } catch (Exception e) {

            System.out.println(
                    "[WEBSITE CRAWL FAILED] "
                            + SensitiveLogSanitizer.redact(
                            e.getMessage()
                    )
            );
        }

        return new SiteMapResult(
                rootUrl,
                pages
        );
    }

    private void maybeQueueLink(

            String rootUrl,
            PageLink link,
            Set<String> visited,
            Queue<String> queue

    ) {

        String href =
                link == null
                        ? null
                        : link.getHref();

        if (
                href == null
                        ||
                        href.isBlank()
                        ||
                        !href.startsWith("http")
                        ||
                        visited.contains(href)
        ) {

            return;
        }

        if (
                !isAllowedCrawlTarget(
                        rootUrl,
                        href
                )
        ) {

            return;
        }

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

            return;
        }

        System.out.println(
                "QUEUE ADD = "
                        + href
        );

        queue.add(href);
    }

    private boolean maybeSubmitLogin(

            Page page,
            Map<String, String> variables

    ) {

        String username =
                variableValue(
                        variables,
                        "username",
                        "user",
                        "email",
                        "login"
                );

        String password =
                variableValue(
                        variables,
                        "password",
                        "pass"
                );

        if (
                username.isBlank()
                        ||
                        password.isBlank()
        ) {

            return false;
        }

        try {

            Locator authField =
                    page.locator(
                                    "input[type='text'], input[type='email'], input[name*='user' i], input[id*='user' i], input[placeholder*='user' i], input[name*='email' i], input[id*='email' i], input[placeholder*='email' i]"
                            )
                            .first();

            Locator passwordField =
                    page.locator(
                                    "input[type='password']"
                            )
                            .first();

            if (
                    authField.count() == 0
                            ||
                            passwordField.count() == 0
            ) {

                return false;
            }

            authField.fill(username);

            passwordField.fill(password);

            Locator submit =
                    page.locator(
                                    "input[type='submit'], button[type='submit'], button:has-text('Login'), button:has-text('Log in'), button:has-text('Sign in'), input[value*='Login' i], input[value*='Sign in' i]"
                            )
                            .first();

            if (
                    submit.count() == 0
            ) {

                return false;
            }

            submit.click();

            page.waitForTimeout(2500);

            return true;

        } catch (Exception e) {

            System.out.println(
                    "[CRAWLER LOGIN CONTINUATION FAILED] "
                            + SensitiveLogSanitizer.redact(
                            e.getMessage()
                    )
            );

            return false;
        }
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

                    String value =
                            safeAttr(
                                    handle,
                                    "value"
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
                                    type,
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

                    System.out.println(
                            "[ELEMENT EXTRACTION FAILED] "
                                    + SensitiveLogSanitizer.redact(
                                    e.getMessage()
                            )
                    );
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "[ELEMENT EXTRACTION FAILED] "
                            + SensitiveLogSanitizer.redact(
                            e.getMessage()
                    )
            );
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

                    String normalizedHref =
                            href.trim()
                                    .toLowerCase();

                    if (
                            normalizedHref.startsWith("#")
                                    ||
                                    normalizedHref.startsWith("javascript:")
                                    ||
                                    normalizedHref.startsWith("mailto:")
                                    ||
                                    normalizedHref.startsWith("tel:")
                    ) {

                        continue;
                    }

                    // =========================================
                    // RELATIVE URL
                    // =========================================

                    if (!href.startsWith("http")) {

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

                    System.out.println(
                            "[LINK EXTRACTION FAILED] "
                                    + SensitiveLogSanitizer.redact(
                                    e.getMessage()
                            )
                    );
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "[LINK EXTRACTION FAILED] "
                            + SensitiveLogSanitizer.redact(
                            e.getMessage()
                    )
            );
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
                isValueSelectableInput(tag, type)
                        &&
                        !selectorText(text).isBlank()
        ) {

            return tag
                    + "[type='"
                    + cssAttr(type)
                    + "'][value='"
                    + cssAttr(selectorText(text))
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
            String name,
            String type,
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

        if (
                isValueSelectableInput(tag, type)
                        &&
                        !selectorText(text).isBlank()
        ) {

            return "//"
                    + tag
                    + "[@value='"
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

    private boolean isValueSelectableInput(
            String tag,
            String type
    ) {

        String normalizedTag =
                safe(tag)
                        .toLowerCase();

        String normalizedType =
                safe(type)
                        .toLowerCase();

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
                safe(text)
                        .trim();

        if (
                !visibleText.isBlank()
        ) {

            return visibleText;
        }

        if (
                isValueSelectableInput(tag, type)
                        &&
                        !safe(value).isBlank()
        ) {

            return safe(value).trim();
        }

        if (
                safe(tag).equalsIgnoreCase("button")
                        &&
                        !safe(ariaLabel).isBlank()
        ) {

            return safe(ariaLabel).trim();
        }

        return "";
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

    private String variableValue(

            Map<String, String> variables,
            String... keys

    ) {

        if (
                variables == null
                        ||
                        variables.isEmpty()
        ) {

            return "";
        }

        for (
                String key
                : keys
        ) {

            if (
                    key == null
            ) {

                continue;
            }

            String value =
                    variables.get(key.toLowerCase());

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
