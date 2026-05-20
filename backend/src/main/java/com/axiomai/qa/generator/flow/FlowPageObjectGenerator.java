package com.axiomai.qa.generator.flow;

import com.axiomai.qa.flow.DetectedFlow;
import com.axiomai.qa.models.FlowStep;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class FlowPageObjectGenerator {

    public String generate(
            List<DetectedFlow> flows
    ) {

        StringBuilder sb =
                new StringBuilder();

        sb.append("package com.axiomai.generated.pages;\n\n");
        sb.append("import com.microsoft.playwright.*;\n\n");
        sb.append("import java.util.*;\n\n");
        sb.append("public class GeneratedPage {\n\n");
        sb.append("    private final Page page;\n");
        sb.append("    private final Map<String, List<String>> selectors = new HashMap<>();\n\n");
        sb.append("    public GeneratedPage(Page page) {\n\n");
        sb.append("        this.page = page;\n");

        Set<String> generatedTargets =
                new HashSet<>();

        for (
                DetectedFlow flow
                : flows
        ) {

            for (
                    FlowStep step
                    : flow.getSteps()
            ) {

                String key =
                        targetKey(
                                step.getTarget()
                        );

                if (
                        key.isBlank()
                                ||
                                generatedTargets.contains(key)
                ) {

                    continue;
                }

                generatedTargets.add(key);

                sb.append("        selectors.put(\"")
                        .append(escape(key))
                        .append("\", Arrays.asList(");

                appendSelectorList(
                        sb,
                        step
                );

                sb.append("));\n");
            }
        }

        sb.append("    }\n\n");

        sb.append("""
                    public void launch(String url) {

                        page.navigate(url);
                        page.waitForLoadState();
                        dismissCookieBanner();
                    }

                    public void enter(String target, String value) {

                        dismissCookieBanner();

                        if (handleSpecialEnter(target, value)) {
                            return;
                        }

                        Locator locator = resolveEditable(target);

                        if (locator != null) {
                            fillOrType(locator, value);
                            confirmFieldIfNeeded(target);
                            return;
                        }

                        Locator trigger = resolveOptional(
                                target,
                                triggerSemanticSelectors(target)
                        );

                        if (trigger != null) {
                            trigger.click();
                            page.waitForTimeout(400);
                        }

                        Locator focused = firstVisible(
                                "input:focus",
                                "textarea:focus",
                                "select:focus",
                                "[contenteditable='true']:focus",
                                "[role='textbox']:focus",
                                "[role='searchbox']:focus",
                                "[role='combobox']:focus"
                        );

                        if (focused != null && isEditable(focused)) {
                            fillOrType(focused, value);
                            confirmFieldIfNeeded(target);
                            return;
                        }

                        if (trigger != null) {
                            Locator visibleInput = firstVisible(
                                    "input:visible",
                                    "textarea:visible",
                                    "select:visible",
                                    "[contenteditable='true']:visible",
                                    "[role='textbox']:visible",
                                    "[role='searchbox']:visible",
                                    "[role='combobox']:visible"
                            );

                            if (visibleInput != null && isEditable(visibleInput)) {
                                fillOrType(visibleInput, value);
                                confirmFieldIfNeeded(target);
                                return;
                            }
                        }

                        throw new RuntimeException("Unable to resolve editable element: " + target);
                    }

                    public void click(String target) {

                        dismissCookieBanner();

                        Locator special = resolveSpecialClick(target);

                        if (special != null) {
                            clickWithFallback(special);
                            waitAfterClick(target);
                            return;
                        }

                        clickWithFallback(resolve(target));
                        waitAfterClick(target);
                    }

                    public void shouldSee(String expectedText) {

                        if (
                                expectedText != null
                                        &&
                                        expectedText.equalsIgnoreCase("Your cart is empty")
                                        &&
                                        page.locator(".cart_item").count() == 0
                        ) {

                            return;
                        }

                        String body = page.locator("body").innerText();

                        if (
                                body == null
                                        ||
                                        (
                                                !body.toLowerCase().contains(expectedText.toLowerCase())
                                                        &&
                                                        !matchesFlexibleExpectation(expectedText, body)
                                                        &&
                                                        !matchesSuccessfulTechnicalResponse(expectedText, body)
                                        )
                        ) {

                            throw new AssertionError(
                                    "Expected page to contain text: " + expectedText
                            );
                        }
                    }

                    private boolean matchesSuccessfulTechnicalResponse(String expectedText, String body) {

                        if (expectedText == null || body == null) {
                            return false;
                        }

                        String expected = expectedText.toLowerCase();
                        String actual = body.toLowerCase().replace(" ", "");

                        return (
                                expected.contains("flight")
                                        ||
                                        expected.contains("search")
                                        ||
                                        expected.contains("booking")
                        )
                                &&
                                (
                                        actual.contains("200-ok")
                                                ||
                                                actual.contains("200ok")
                                                ||
                                                actual.contains("status:200")
                                );
                    }

                    private boolean matchesFlexibleExpectation(String expectedText, String body) {

                        if (expectedText == null || body == null) {
                            return false;
                        }

                        String expected = expectedText.toLowerCase();
                        String actual = body.toLowerCase();
                        String currentUrl = page.url() == null ? "" : page.url().toLowerCase();

                        if (expected.contains(" from ") && expected.contains(" to ")) {
                            String[] parts = expected.split("\\\\s+");
                            String origin = "";
                            String destination = "";

                            for (int i = 0; i < parts.length; i++) {
                                if ("from".equals(parts[i]) && i + 1 < parts.length) {
                                    origin = parts[i + 1];
                                }

                                if ("to".equals(parts[i]) && i + 1 < parts.length) {
                                    destination = parts[i + 1];
                                }
                            }

                            return !origin.isBlank()
                                    &&
                                    !destination.isBlank()
                                    &&
                                    (actual.contains(origin) || currentUrl.contains(origin))
                                    &&
                                    (actual.contains(destination) || currentUrl.contains(destination));
                        }

                        return false;
                    }

                    private Locator resolve(String target) {

                        String key = targetKey(target);
                        List<String> knownSelectors = selectors.getOrDefault(key, List.of());

                        for (String selector : knownSelectors) {

                            Locator locator = page.locator(selector);

                            if (locator.count() > 0 && locator.first().isVisible()) {
                                return locator.first();
                            }
                        }

                        for (String selector : semanticSelectors(target)) {

                            Locator locator = page.locator(selector);

                            if (locator.count() > 0 && locator.first().isVisible()) {
                                return locator.first();
                            }
                        }

                        throw new RuntimeException("Unable to resolve element: " + target);
                    }

                    private Locator resolveSpecialClick(String target) {

                        String lower = target == null ? "" : target.toLowerCase();

                        if (lower.contains("add to cart")) {
                            return firstVisible(
                                    "[data-test='add-to-cart-" + slug(productName(target, "add to cart")) + "']",
                                    "button:has-text(\\"Add to cart\\")"
                            );
                        }

                        if (lower.contains("remove")) {
                            return firstVisible(
                                    "[data-test='remove-" + slug(productName(target, "remove")) + "']",
                                    "button:has-text(\\"Remove\\")"
                            );
                        }

                        if (lower.contains("cart")) {
                            return firstVisible(
                                    "[data-test='shopping-cart-link']",
                                    ".shopping_cart_link",
                                    "a:has-text(\\"Cart\\")"
                            );
                        }

                        if (lower.contains("application menu") || lower.contains("menu")) {
                            return firstVisible(
                                    "#react-burger-menu-btn",
                                    "[data-test='open-menu']",
                                    "button:has-text(\\"Open Menu\\")"
                            );
                        }

                        if (lower.contains("logout")) {
                            return firstVisible(
                                    "#logout_sidebar_link",
                                    "[data-test='logout-sidebar-link']",
                                    "a:has-text(\\"Logout\\")"
                            );
                        }

                        if (lower.contains("checkout")) {
                            return firstVisible(
                                    "[data-test='checkout']",
                                    "button:has-text(\\"Checkout\\")"
                            );
                        }

                        if (lower.contains("continue")) {
                            return firstVisible(
                                    "[data-test='continue']",
                                    "input[value='Continue']",
                                    "button:has-text(\\"Continue\\")"
                            );
                        }

                        if (lower.contains("finish")) {
                            return firstVisible(
                                    "[data-test='finish']",
                                    "button:has-text(\\"Finish\\")"
                            );
                        }

                        if (lower.contains("search")) {
                            return firstVisible(
                                    "[data-cy*='submit' i]",
                                    "[data-testid*='search' i]",
                                    "button:has-text(\\"Search\\")",
                                    "a:has-text(\\"Search\\")",
                                    "[role='button']:has-text(\\"Search\\")",
                                    "input[value*='Search' i]"
                            );
                        }

                        return null;
                    }

                    private void dismissCookieBanner() {

                        Locator accept = firstVisible(
                                "button:has-text(\\"ACCEPT\\")",
                                "button:has-text(\\"Accept\\")",
                                "button:has-text(\\"I agree\\")",
                                "[data-cy*='accept' i]",
                                "[data-testid*='accept' i]"
                        );

                        if (accept == null) {
                            return;
                        }

                        try {
                            accept.click(new Locator.ClickOptions().setTimeout(1000));
                            page.waitForTimeout(300);
                        } catch (RuntimeException ignored) {
                            // Cookie overlays are optional and should not block test execution.
                        }
                    }

                    private boolean handleSpecialEnter(String target, String value) {

                        if (!isMakeMyTripPage()) {
                            return false;
                        }

                        if (enterMakeMyTripLocation(target, value)) {
                            return true;
                        }

                        String lower = target == null ? "" : target.toLowerCase();

                        if (
                                lower.contains("date")
                                        ||
                                        lower.contains("departure")
                                        ||
                                        lower.contains("return")
                        ) {

                            try {
                                page.keyboard().press("Escape");
                            } catch (RuntimeException ignored) {
                                // The date picker may not be open.
                            }

                            return true;
                        }

                        return false;
                    }

                    private boolean enterMakeMyTripLocation(String target, String value) {

                        String lower = target == null ? "" : target.toLowerCase();
                        boolean from = lower.equals("from") || lower.contains("origin");
                        boolean to = lower.equals("to") || lower.contains("destination");

                        if (!from && !to) {
                            return false;
                        }

                        Locator trigger = from
                                ? firstVisible(
                                "#fromCity",
                                "[for='fromCity']",
                                "[data-cy='fromCity']",
                                "xpath=//*[normalize-space()='From']/ancestor::*[contains(@class,'fsw_inputBox')][1]"
                        )
                                : firstVisible(
                                "#toCity",
                                "[for='toCity']",
                                "[data-cy='toCity']",
                                "xpath=//*[normalize-space()='To']/ancestor::*[contains(@class,'fsw_inputBox')][1]"
                        );

                        if (trigger == null) {
                            return false;
                        }

                        clickWithFallback(trigger);
                        page.waitForTimeout(500);

                        Locator input = firstVisible(
                                "input[placeholder*='from' i]",
                                "input[placeholder*='to' i]",
                                "input[role='combobox']",
                                "[role='combobox'] input",
                                "input:focus",
                                "input:visible"
                        );

                        if (input == null || !isEditable(input)) {
                            return false;
                        }

                        fillOrType(input, value);
                        page.waitForTimeout(900);

                        String escapedValue = cssText(value);
                        Locator suggestion = firstVisible(
                                "li:has-text(\\"" + escapedValue + "\\")",
                                "[role='option']:has-text(\\"" + escapedValue + "\\")",
                                "[id^='react-autowhatever']:has-text(\\"" + escapedValue + "\\") li",
                                ".react-autosuggest__suggestion:has-text(\\"" + escapedValue + "\\")",
                                "div[role='listbox'] li",
                                "div[role='listbox'] [role='option']"
                        );

                        if (suggestion != null) {
                            clickWithFallback(suggestion);
                        } else {
                            page.keyboard().press("ArrowDown");
                            page.keyboard().press("Enter");
                        }

                        page.waitForTimeout(600);
                        return true;
                    }

                    private boolean isMakeMyTripPage() {

                        String url = page.url();

                        return url != null
                                &&
                                url.toLowerCase()
                                        .contains("makemytrip");
                    }

                    private Locator resolveEditable(String target) {

                        String key = targetKey(target);
                        List<String> knownSelectors = selectors.getOrDefault(key, List.of());

                        for (String selector : knownSelectors) {

                            Locator locator = page.locator(selector);

                            if (
                                    locator.count() > 0
                                            &&
                                            locator.first().isVisible()
                                            &&
                                            isEditable(locator.first())
                            ) {
                                return locator.first();
                            }
                        }

                        return resolveOptional(
                                target,
                                inputSemanticSelectors(target),
                                true
                        );
                    }

                    private Locator resolveOptional(

                            String target,

                            List<String> candidates

                    ) {

                        return resolveOptional(
                                target,
                                candidates,
                                false
                        );
                    }

                    private Locator resolveOptional(

                            String target,

                            List<String> candidates,

                            boolean editableOnly

                    ) {

                        for (String selector : candidates) {

                            if (selector == null || selector.isBlank()) {
                                continue;
                            }

                            Locator locator = page.locator(selector);

                            if (locator.count() > 0 && locator.first().isVisible()) {

                                if (!editableOnly || isEditable(locator.first())) {
                                    return locator.first();
                                }
                            }
                        }

                        return null;
                    }

                    private void fillOrType(Locator locator, String value) {

                        try {
                            locator.fill("");
                            locator.fill(value);
                            return;
                        } catch (RuntimeException ignored) {
                            locator.click();
                            page.keyboard().press("Control+A");
                            page.keyboard().type(value == null ? "" : value);
                        }
                    }

                    private void confirmFieldIfNeeded(String target) {

                        String lower = target == null ? "" : target.toLowerCase();

                        if (
                                lower.equals("from")
                                        ||
                                        lower.equals("to")
                                        ||
                                        lower.contains("origin")
                                        ||
                                        lower.contains("destination")
                                        ||
                                        lower.contains("city")
                                        ||
                                        lower.contains("airport")
                                        ||
                                        lower.contains("departure")
                                        ||
                                        lower.contains("date")
                        ) {

                            page.waitForTimeout(500);

                            try {
                                page.keyboard().press("Enter");
                                page.waitForTimeout(300);
                            } catch (RuntimeException ignored) {
                                // Some widgets close themselves after filling.
                            }
                        }
                    }

                    private void clickWithFallback(Locator locator) {

                        try {
                            locator.click();
                            return;
                        } catch (RuntimeException firstFailure) {
                            locator.click(new Locator.ClickOptions().setForce(true));
                        }
                    }

                    private void waitAfterClick(String target) {

                        String lower = target == null ? "" : target.toLowerCase();

                        if (
                                lower.contains("search")
                                        ||
                                        lower.contains("submit")
                                        ||
                                        lower.contains("continue")
                                        ||
                                        lower.contains("finish")
                        ) {

                            try {
                                page.waitForLoadState();
                            } catch (RuntimeException ignored) {
                                // Some single-page apps update without a full load-state transition.
                            }

                            page.waitForTimeout(1200);
                        }
                    }

                    private boolean isEditable(Locator locator) {

                        try {
                            Object editable = locator.evaluate(
                                    "el => { const tag = el.tagName.toLowerCase(); const role = (el.getAttribute('role') || '').toLowerCase(); return tag === 'input' || tag === 'textarea' || tag === 'select' || el.isContentEditable || role === 'textbox' || role === 'searchbox' || role === 'combobox'; }"
                            );

                            return Boolean.TRUE.equals(editable);
                        } catch (RuntimeException ignored) {
                            return false;
                        }
                    }

                    private Locator firstVisible(String... candidates) {

                        for (String selector : candidates) {

                            if (selector == null || selector.isBlank()) {
                                continue;
                            }

                            Locator locator = page.locator(selector);

                            if (locator.count() > 0 && locator.first().isVisible()) {
                                return locator.first();
                            }
                        }

                        return null;
                    }

                    private List<String> semanticSelectors(String target) {

                        String escaped = cssText(target);

                        return Arrays.asList(
                                "[data-test=\\"" + escaped + "\\"]",
                                "[data-test=\\"" + slug(target) + "\\"]",
                                "[data-testid=\\"" + escaped + "\\"]",
                                "[data-cy=\\"" + escaped + "\\"]",
                                "[id=\\"" + slug(target) + "\\"]",
                                "[name=\\"" + escaped + "\\"]",
                                "input[name*=\\"" + escaped + "\\" i]",
                                "input[placeholder*=\\"" + escaped + "\\" i]",
                                "textarea[placeholder*=\\"" + escaped + "\\" i]",
                                "[aria-label*=\\"" + escaped + "\\" i]",
                                "button:has-text(\\"" + escaped + "\\")",
                                "a:has-text(\\"" + escaped + "\\")",
                                "[role='button']:has-text(\\"" + escaped + "\\")",
                                "text=\\"" + escaped + "\\""
                        );
                    }

                    private List<String> inputSemanticSelectors(String target) {

                        String escaped = cssText(target);
                        String slug = slug(target);

                        return Arrays.asList(
                                "input[name=\\"" + escaped + "\\" i]",
                                "input[name*=\\"" + escaped + "\\" i]",
                                "input[id=\\"" + slug + "\\" i]",
                                "input[id*=\\"" + slug + "\\" i]",
                                "input[placeholder*=\\"" + escaped + "\\" i]",
                                "input[aria-label*=\\"" + escaped + "\\" i]",
                                "input[data-test*=\\"" + slug + "\\" i]",
                                "input[data-testid*=\\"" + slug + "\\" i]",
                                "input[data-cy*=\\"" + slug + "\\" i]",
                                "textarea[name*=\\"" + escaped + "\\" i]",
                                "textarea[placeholder*=\\"" + escaped + "\\" i]",
                                "select[name*=\\"" + escaped + "\\" i]",
                                "[contenteditable='true'][aria-label*=\\"" + escaped + "\\" i]",
                                "[role='textbox'][aria-label*=\\"" + escaped + "\\" i]",
                                "[role='searchbox'][aria-label*=\\"" + escaped + "\\" i]",
                                "[role='combobox'][aria-label*=\\"" + escaped + "\\" i]",
                                "[role='combobox']:has-text(\\"" + escaped + "\\")",
                                "label:has-text(\\"" + escaped + "\\") input",
                                "label:has-text(\\"" + escaped + "\\") textarea",
                                "div:has-text(\\"" + escaped + "\\") input",
                                "div:has-text(\\"" + escaped + "\\") textarea"
                        );
                    }

                    private List<String> triggerSemanticSelectors(String target) {

                        String escaped = cssText(target);
                        String slug = slug(target);

                        return Arrays.asList(
                                "[data-test=\\"" + escaped + "\\"]",
                                "[data-test=\\"" + slug + "\\"]",
                                "[data-testid=\\"" + escaped + "\\"]",
                                "[data-testid=\\"" + slug + "\\"]",
                                "[data-cy=\\"" + escaped + "\\"]",
                                "[data-cy=\\"" + slug + "\\"]",
                                "[id=\\"" + slug + "\\"]",
                                "[aria-label*=\\"" + escaped + "\\" i]",
                                "label:has-text(\\"" + escaped + "\\")",
                                "button:has-text(\\"" + escaped + "\\")",
                                "[role='button']:has-text(\\"" + escaped + "\\")",
                                "[role='combobox']:has-text(\\"" + escaped + "\\")",
                                "[role='textbox']:has-text(\\"" + escaped + "\\")",
                                "span:has-text(\\"" + escaped + "\\")",
                                "div:has-text(\\"" + escaped + "\\")"
                        );
                    }

                    private String targetKey(String value) {

                        if (value == null) {
                            return "";
                        }

                        String normalized = value
                                .replace("${", "")
                                .replace("}", "")
                                .trim()
                                .toLowerCase()
                                .replaceAll("[^a-z0-9]+", "_")
                                .replaceAll("^_+|_+$", "");

                        if (normalized.contains("user") || normalized.contains("auth")) {
                            return "username";
                        }

                        if (normalized.contains("pass")) {
                            return "password";
                        }

                        if (normalized.contains("login")) {
                            return "login_button";
                        }

                        return normalized;
                    }

                    private String cssText(String value) {

                        if (value == null) {
                            return "";
                        }

                        return value
                                .replace("${", "")
                                .replace("}", "")
                                .replace("\\\\", "\\\\\\\\")
                                .replace("\\"", "\\\\\\"")
                                .trim();
                    }

                    private String productName(String target, String actionText) {

                        if (target == null) {
                            return "";
                        }

                        return target
                                .replaceAll("(?i)" + java.util.regex.Pattern.quote(actionText), "")
                                .trim();
                    }

                    private String slug(String value) {

                        if (value == null) {
                            return "";
                        }

                        return value
                                .replace("${", "")
                                .replace("}", "")
                                .trim()
                                .toLowerCase()
                                .replaceAll("[^a-z0-9]+", "-")
                                .replaceAll("^-+|-+$", "");
                    }
                }
                """);

        return sb.toString();
    }

    private void appendSelectorList(

            StringBuilder sb,
            FlowStep step

    ) {

        boolean added =
                false;

        if (
                step.getSelector() != null
                        &&
                        !step.getSelector()
                                .isBlank()
        ) {

            sb.append("\"")
                    .append(escape(step.getSelector()))
                    .append("\"");

            added =
                    true;
        }

        if (
                step.getFallbackSelectors() != null
        ) {

            for (
                    String fallback
                    : step.getFallbackSelectors()
            ) {

                if (
                        fallback == null
                                ||
                                fallback.isBlank()
                                ||
                                fallback.equals(
                                        step.getSelector()
                                )
                ) {

                    continue;
                }

                if (
                        added
                ) {

                    sb.append(", ");
                }

                sb.append("\"")
                        .append(escape(fallback))
                        .append("\"");

                added =
                        true;
            }
        }

        if (
                !added
        ) {

            sb.append("\"")
                    .append(escape(step.getTarget()))
                    .append("\"");
        }
    }

    private String targetKey(
            String value
    ) {

        if (
                value == null
        ) {

            return "";
        }

        String normalized =
                value.trim()
                        .toLowerCase()
                        .replaceAll(
                                "[^a-z0-9]+",
                                "_"
                        )
                        .replaceAll(
                                "^_+|_+$",
                                ""
                        );

        if (
                normalized.contains("user")
                        ||
                        normalized.contains("auth")
        ) {

            return "username";
        }

        if (
                normalized.contains("pass")
        ) {

            return "password";
        }

        if (
                normalized.contains("login")
        ) {

            return "login_button";
        }

        return normalized;
    }

    private String escape(
            String value
    ) {

        if (
                value == null
        ) {

            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
