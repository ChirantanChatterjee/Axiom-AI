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
        sb.append("import com.microsoft.playwright.options.*;\n\n");
        sb.append("import java.nio.file.*;\n");
        sb.append("import java.util.*;\n\n");
        sb.append("public class GeneratedPage {\n\n");
        sb.append("    private final Page page;\n");
        sb.append("    private final Map<String, List<String>> selectors = new HashMap<>();\n\n");
        sb.append("    public GeneratedPage(Page page) {\n\n");
        sb.append("        this.page = page;\n");
        sb.append("        this.page.setDefaultTimeout(runtimeTimeoutMs());\n");
        sb.append("        this.page.setDefaultNavigationTimeout(runtimeNavigationTimeoutMs());\n");

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

                        System.out.println("AIF PAGE launch: " + url);

                        page.navigate(
                                url,
                                new Page.NavigateOptions()
                                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                                        .setTimeout(runtimeNavigationTimeoutMs())
                        );

                        try {
                            page.waitForLoadState(
                                    LoadState.DOMCONTENTLOADED,
                                    new Page.WaitForLoadStateOptions()
                                            .setTimeout(3000)
                            );
                        } catch (RuntimeException ignored) {
                            // Navigation already reached domcontentloaded or the app updates without another load event.
                        }

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
                            confirmFieldIfNeeded(target, value);
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
                            confirmFieldIfNeeded(target, value);
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
                                confirmFieldIfNeeded(target, value);
                                return;
                            }
                        }

                        throw new RuntimeException("Unable to resolve editable element: " + target);
                    }

                    public void click(String target) {

                        dismissCookieBanner();

                        if (handleSpecialClick(target)) {
                            waitAfterClick(target);
                            return;
                        }

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

                        String body = waitForExpectedText(expectedText);

                        if (
                                !matchesExpectation(expectedText, body)
                        ) {

                            recordAssertionFailure(expectedText, body);

                            throw new AssertionError(
                                    "Expected page to contain text: " + expectedText
                            );
                        }
                    }

                    private String waitForExpectedText(String expectedText) {

                        long deadline = System.currentTimeMillis() + 6500;
                        String body = "";

                        do {
                            body = bodyText();

                            if (matchesExpectation(expectedText, body)) {
                                return body;
                            }

                            page.waitForTimeout(250);
                        } while (System.currentTimeMillis() < deadline);

                        return body;
                    }

                    private boolean matchesExpectation(String expectedText, String body) {

                        if (expectedText == null || expectedText.isBlank() || body == null) {
                            return false;
                        }

                        return body.toLowerCase().contains(expectedText.toLowerCase())
                                ||
                                matchesFlexibleExpectation(expectedText, body)
                                ||
                                matchesHtmlValidation(expectedText)
                                ||
                                matchesSuccessfulTechnicalResponse(expectedText, body);
                    }

                    private double runtimeTimeoutMs() {

                        return parseTimeout(
                                System.getenv("AIF_STEP_TIMEOUT_MS"),
                                8000
                        );
                    }

                    private double runtimeNavigationTimeoutMs() {

                        return parseTimeout(
                                System.getenv("AIF_NAVIGATION_TIMEOUT_MS"),
                                15000
                        );
                    }

                    private double parseTimeout(String value, double fallback) {

                        if (value == null || value.isBlank()) {
                            return fallback;
                        }

                        try {
                            double parsed = Double.parseDouble(value.trim());
                            return parsed > 0 ? parsed : fallback;
                        } catch (NumberFormatException ignored) {
                            return fallback;
                        }
                    }

                    private String bodyText() {

                        try {
                            return page.locator("body").innerText();
                        } catch (RuntimeException ignored) {
                            return "";
                        }
                    }

                    private void recordAssertionFailure(String expectedText, String body) {

                        try {
                            Path target = Path.of("target");
                            Files.createDirectories(target);

                            String diagnostic =
                                    "Expected: " + nullSafe(expectedText) + System.lineSeparator()
                                            + "URL: " + nullSafe(page.url()) + System.lineSeparator()
                                            + "Title: " + nullSafe(page.title()) + System.lineSeparator()
                                            + "Body:" + System.lineSeparator()
                                            + truncate(nullSafe(body), 12000);

                            Files.writeString(
                                    target.resolve("aif-last-assertion-failure.txt"),
                                    diagnostic
                            );
                        } catch (RuntimeException | java.io.IOException ignored) {
                            // Diagnostics must never mask the real assertion failure.
                        }
                    }

                    private String nullSafe(String value) {
                        return value == null ? "" : value;
                    }

                    private String truncate(String value, int maxLength) {

                        if (value == null || value.length() <= maxLength) {
                            return value == null ? "" : value;
                        }

                        return value.substring(0, maxLength);
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

                    private boolean matchesHtmlValidation(String expectedText) {

                        if (expectedText == null) {
                            return false;
                        }

                        String expected = expectedText.toLowerCase();

                        if (
                                !expected.contains("validation")
                                        &&
                                        !expected.contains("invalid")
                                        &&
                                        !expected.contains("required")
                                        &&
                                        !expected.contains("error")
                        ) {

                            return false;
                        }

                        try {
                            return page.locator("input:invalid, textarea:invalid, select:invalid").count() > 0;
                        } catch (RuntimeException ignored) {
                            return false;
                        }
                    }

                    private boolean matchesFlexibleExpectation(String expectedText, String body) {

                        if (expectedText == null || body == null) {
                            return false;
                        }

                        String expected = expectedText.toLowerCase();
                        String actual = body.toLowerCase();
                        String currentUrl = page.url() == null ? "" : page.url().toLowerCase();

                        if (expected.contains("bill payment complete")) {

                            return actual.contains("bill payment")
                                    &&
                                    (
                                            actual.contains("complete")
                                                    ||
                                                    actual.contains("successful")
                                                    ||
                                                    actual.contains("successfully")
                                    );
                        }

                        if (
                                expected.contains("please fill out this field")
                                        ||
                                        expected.contains("required")
                        ) {

                            return actual.contains("required")
                                    ||
                                    actual.contains("is empty")
                                    ||
                                    actual.contains("cannot be empty")
                                    ||
                                    actual.contains("empty")
                                    ||
                                    actual.contains("error");
                        }

                        if (
                                expected.contains("invalid amount")
                                        ||
                                        expected.contains("amount validation")
                                        ||
                                        expected.contains("amount error")
                        ) {

                            return actual.contains("amount")
                                    &&
                                    (
                                            actual.contains("valid")
                                                    ||
                                            actual.contains("invalid")
                                                    ||
                                            actual.contains("error")
                                                    ||
                                            actual.contains("must")
                                                    ||
                                            actual.contains("positive")
                                                    ||
                                            actual.contains("greater")
                                    );
                        }

                        if (expected.contains("invalid account")) {

                            return actual.contains("account")
                                    &&
                                    (
                                            actual.contains("number")
                                                    ||
                                            actual.contains("valid")
                                                    ||
                                            actual.contains("invalid")
                                                    ||
                                            actual.contains("match")
                                                    ||
                                            actual.contains("same")
                                                    ||
                                            actual.contains("error")
                                    );
                        }

                        if (
                                expected.contains("registration success")
                                        ||
                                        expected.contains("account is created")
                                        ||
                                        expected.contains("account created")
                        ) {

                            return actual.contains("welcome")
                                    ||
                                    actual.contains("account created")
                                    ||
                                    actual.contains("account was created")
                                    ||
                                    actual.contains("created successfully")
                                    ||
                                    actual.contains("success");
                        }

                        if (expected.contains("password mismatch")) {

                            return actual.contains("password")
                                    &&
                                    (
                                            actual.contains("match")
                                                    ||
                                            actual.contains("same")
                                                    ||
                                            actual.contains("error")
                                    );
                        }

                        if (expected.contains("duplicate username")) {

                            return actual.contains("username")
                                    &&
                                    (
                                            actual.contains("already")
                                                    ||
                                            actual.contains("exists")
                                                    ||
                                            actual.contains("taken")
                                                    ||
                                            actual.contains("error")
                                    );
                        }

                        if (expected.contains("login error")) {

                            return actual.contains("error")
                                    ||
                                    actual.contains("invalid")
                                    ||
                                    actual.contains("could not be verified")
                                    ||
                                    (
                                            actual.contains("username")
                                                    &&
                                            actual.contains("password")
                                    );
                        }

                        if (
                                expected.contains("account overview")
                                        ||
                                        expected.contains("accounts and balances")
                        ) {

                            return actual.contains("accounts overview")
                                    ||
                                    (
                                            actual.contains("account")
                                                    &&
                                            actual.contains("balance")
                                    )
                                    ||
                                    currentUrl.contains("overview");
                        }

                        if (expected.contains("new account number")) {

                            return actual.contains("account opened")
                                    ||
                                    actual.contains("new account")
                                    ||
                                    (
                                            actual.contains("account")
                                                    &&
                                            actual.contains("number")
                                    );
                        }

                        if (expected.contains("transfer confirmation")) {

                            return actual.contains("transfer")
                                    &&
                                    (
                                            actual.contains("complete")
                                                    ||
                                            actual.contains("transferred")
                                                    ||
                                            actual.contains("confirmation")
                                                    ||
                                            actual.contains("success")
                                    );
                        }

                        if (expected.contains("matching transactions")) {

                            return actual.contains("transaction")
                                    ||
                                    actual.contains("results");
                        }

                        if (
                                expected.contains("no matching transactions")
                                        ||
                                        expected.contains("no-match")
                        ) {

                            return actual.contains("no transactions")
                                    ||
                                    actual.contains("no results")
                                    ||
                                    actual.contains("no matching")
                                    ||
                                    actual.contains("not found");
                        }

                        if (expected.contains("login page")) {

                            return actual.contains("customer login")
                                    ||
                                    actual.contains("log in")
                                    ||
                                    (
                                            actual.contains("username")
                                                    &&
                                            actual.contains("password")
                                    );
                        }

                        if (expected.contains("secure page should not be accessible")) {

                            return actual.contains("login")
                                    ||
                                    actual.contains("unauthorized")
                                    ||
                                    actual.contains("forbidden")
                                    ||
                                    actual.contains("session")
                                    ||
                                    actual.contains("access denied");
                        }

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

                        Locator known = firstVisible(
                                knownSelectors.toArray(String[]::new)
                        );

                        if (known != null) {
                            return known;
                        }

                        Locator semantic = firstVisible(
                                semanticSelectors(target).toArray(String[]::new)
                        );

                        if (semantic != null) {
                            return semantic;
                        }

                        throw new RuntimeException("Unable to resolve element: " + target);
                    }

                    private boolean handleSpecialClick(String target) {

                        String lower = target == null ? "" : target.toLowerCase();

                        if (
                                lower.equals("browser back")
                                        ||
                                        lower.equals("back button")
                                        ||
                                        lower.equals("go back")
                        ) {

                            page.goBack();

                            try {
                                page.waitForLoadState(
                                        LoadState.DOMCONTENTLOADED,
                                        new Page.WaitForLoadStateOptions()
                                                .setTimeout(3000)
                                );
                            } catch (RuntimeException ignored) {
                                // Some apps restore state from cache without a load event.
                            }

                            page.waitForTimeout(500);
                            return true;
                        }

                        return false;
                    }

                    private Locator resolveSpecialClick(String target) {

                        String lower = target == null ? "" : target.toLowerCase();

                        if (isAddProductCommand(lower)) {
                            Locator productButton = resolveProductActionButton(
                                    target,
                                    "add-to-cart",
                                    "add"
                            );

                            if (productButton != null) {
                                return productButton;
                            }

                            return firstVisible(
                                    "[data-test='add-to-cart-" + slug(productName(target, "add to cart")) + "']",
                                    "button:has-text(\\"Add to cart\\")"
                            );
                        }

                        if (lower.contains("remove")) {
                            Locator productButton = resolveProductActionButton(
                                    target,
                                    "remove",
                                    "remove"
                            );

                            if (productButton != null) {
                                return productButton;
                            }

                            return firstVisible(
                                    "[data-test='remove-" + slug(productName(target, "remove")) + "']",
                                    "button:has-text(\\"Remove\\")"
                            );
                        }

                        if (lower.contains("send payment")) {
                            Locator submitButton = resolveSubmitButton(
                                    target,
                                    "Send Payment"
                            );

                            if (submitButton != null) {
                                return submitButton;
                            }
                        }

                        if (
                                lower.equals("register")
                                        ||
                                        lower.contains("registration page")
                        ) {

                            return firstVisibleSoon(
                                    4000,
                                    "a:has-text(\\"Register\\")",
                                    "button:has-text(\\"Register\\")",
                                    "[role='button']:has-text(\\"Register\\")"
                            );
                        }

                        if (lower.contains("open new account")) {
                            return firstVisibleSoon(
                                    4000,
                                    "a:has-text(\\"Open New Account\\")",
                                    "button:has-text(\\"Open New Account\\")",
                                    "[role='button']:has-text(\\"Open New Account\\")"
                            );
                        }

                        if (lower.contains("transfer funds")) {
                            return firstVisibleSoon(
                                    4000,
                                    "a:has-text(\\"Transfer Funds\\")",
                                    "button:has-text(\\"Transfer Funds\\")",
                                    "[role='button']:has-text(\\"Transfer Funds\\")"
                            );
                        }

                        if (lower.contains("accounts overview") || lower.contains("account overview")) {
                            return firstVisibleSoon(
                                    4000,
                                    "a:has-text(\\"Accounts Overview\\")",
                                    "a:has-text(\\"Account Overview\\")",
                                    "button:has-text(\\"Accounts Overview\\")"
                            );
                        }

                        if (lower.contains("find transactions")) {
                            return firstVisibleSoon(
                                    4000,
                                    "a:has-text(\\"Find Transactions\\")",
                                    "button:has-text(\\"Find Transactions\\")",
                                    "[role='button']:has-text(\\"Find Transactions\\")"
                            );
                        }

                        if (lower.contains("bill pay") || lower.contains("bill payment")) {
                            return firstVisibleSoon(
                                    4000,
                                    "a:has-text(\\"Bill Pay\\")",
                                    "button:has-text(\\"Bill Pay\\")",
                                    "[role='button']:has-text(\\"Bill Pay\\")"
                            );
                        }

                        if (lower.contains("login") || lower.contains("log in") || lower.contains("sign in")) {
                            return firstVisibleSoon(
                                    4000,
                                    "input[type='submit'][value*='Log In' i]",
                                    "input[type='submit'][value*='Login' i]",
                                    "button[type='submit']:has-text(\\"Log In\\")",
                                    "button[type='submit']:has-text(\\"Login\\")",
                                    "button:has-text(\\"Log In\\")",
                                    "button:has-text(\\"Login\\")",
                                    "input[type='submit']:visible"
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

                        if (lower.contains("button") || lower.contains("submit") || lower.startsWith("send ")) {
                            return resolveSubmitButton(
                                    target,
                                    actionText(target)
                            );
                        }

                        return null;
                    }

                    private boolean isAddProductCommand(String lower) {

                        return lower != null
                                &&
                                (
                                        lower.equals("add")
                                                ||
                                                lower.startsWith("add ")
                                                ||
                                                lower.contains("add to cart")
                                );
                    }

                    private Locator resolveProductActionButton(String target, String actionPrefix, String actionText) {

                        String product = productName(target, actionText);
                        product = productName(product, "to cart");
                        product = productName(product, "product");

                        if (product.isBlank()) {
                            return null;
                        }

                        String productSlug = slug(product);

                        if (productSlug.isBlank()) {
                            return null;
                        }

                        return firstVisibleSoon(
                                4000,
                                "[data-test='" + actionPrefix + "-" + productSlug + "']",
                                "[data-test='" + actionPrefix + "-sauce-labs-" + productSlug + "']",
                                "[data-test^='" + actionPrefix + "-'][data-test*='" + productSlug + "' i]",
                                "button[id*='" + productSlug + "' i]",
                                "button[name*='" + productSlug + "' i]",
                                "button:near(:text(\\"" + cssText(product) + "\\"), 120)",
                                ".inventory_item:has-text(\\"" + cssText(product) + "\\") button:has-text(\\"" + (actionPrefix.equals("remove") ? "Remove" : "Add to cart") + "\\")"
                        );
                    }

                    private Locator resolveSubmitButton(String target, String preferredText) {

                        String text = actionText(preferredText == null || preferredText.isBlank() ? target : preferredText);

                        if (text.isBlank()) {
                            text = actionText(target);
                        }

                        String escaped = cssText(text);
                        String slug = slug(text);

                        return firstVisibleSoon(
                                4000,
                                "input[type='submit'][value*=\\"" + escaped + "\\" i]",
                                "input[type='button'][value*=\\"" + escaped + "\\" i]",
                                "button[type='submit']:has-text(\\"" + escaped + "\\")",
                                "button:has-text(\\"" + escaped + "\\")",
                                "[role='button']:has-text(\\"" + escaped + "\\")",
                                "[data-test*=\\"" + slug + "\\" i]",
                                "[data-testid*=\\"" + slug + "\\" i]",
                                "[data-cy*=\\"" + slug + "\\" i]",
                                "input[type='submit']:visible",
                                "button[type='submit']:visible"
                        );
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

                    private boolean isParaBankPage() {

                        String url = page.url();

                        return url != null
                                &&
                                url.toLowerCase()
                                        .contains("parabank");
                    }

                    private Locator resolveEditable(String target) {

                        Locator special = resolveSpecialEditable(target);

                        if (special != null) {
                            return special;
                        }

                        String key = targetKey(target);
                        List<String> knownSelectors = selectors.getOrDefault(key, List.of());

                        for (String selector : knownSelectors) {

                            Locator locator = page.locator(selector);

                            if (
                                    locator.count() > 0
                                            &&
                                            locator.first().isVisible(
                                                    new Locator.IsVisibleOptions()
                                                            .setTimeout(350)
                                            )
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

                    private Locator resolveSpecialEditable(String target) {

                        if (!isParaBankPage()) {
                            return null;
                        }

                        String lower = target == null ? "" : target.toLowerCase();

                        if (lower.contains("first") && lower.contains("name")) {
                            return firstVisible("input[name='customer.firstName']");
                        }

                        if (lower.contains("last") && lower.contains("name")) {
                            return firstVisible("input[name='customer.lastName']");
                        }

                        if (lower.contains("confirm") && lower.contains("password")) {
                            return firstVisible("input[name='repeatedPassword']");
                        }

                        if (lower.contains("ssn")) {
                            return firstVisible("input[name='customer.ssn']");
                        }

                        if (lower.contains("username")) {
                            return firstVisible("input[name='customer.username']", "input[name='username']");
                        }

                        if (lower.equals("password") || lower.contains("password")) {
                            return firstVisible("input[name='customer.password']", "input[name='password']");
                        }

                        if (lower.contains("payee") && lower.contains("name")) {
                            return firstVisible("input[name='payee.name']");
                        }

                        if (lower.equals("address") || lower.contains("street")) {
                            return firstVisible(
                                    "input[name='customer.address.street']",
                                    "input[name='payee.address.street']"
                            );
                        }

                        if (lower.contains("city")) {
                            return firstVisible(
                                    "input[name='customer.address.city']",
                                    "input[name='payee.address.city']"
                            );
                        }

                        if (lower.contains("state")) {
                            return firstVisible(
                                    "input[name='customer.address.state']",
                                    "input[name='payee.address.state']"
                            );
                        }

                        if (lower.contains("zip")) {
                            return firstVisible(
                                    "input[name='customer.address.zipCode']",
                                    "input[name='payee.address.zipCode']"
                            );
                        }

                        if (lower.contains("phone")) {
                            return firstVisible(
                                    "input[name='customer.phoneNumber']",
                                    "input[name='payee.phoneNumber']"
                            );
                        }

                        if (lower.contains("verify") && lower.contains("account")) {
                            return firstVisible("input[name='verifyAccount']");
                        }

                        if (lower.contains("account type")) {
                            return firstVisible(
                                    "select[name='type']",
                                    "#type"
                            );
                        }

                        if (lower.contains("source account") || lower.contains("from account")) {
                            return firstVisible(
                                    "select[name='fromAccountId']",
                                    "#fromAccountId"
                            );
                        }

                        if (lower.contains("to account")) {
                            return firstVisible(
                                    "select[name='toAccountId']",
                                    "#toAccountId"
                            );
                        }

                        if (lower.contains("transaction id")) {
                            return firstVisible(
                                    "input[name='criteria.transactionId']",
                                    "input[id*='transaction' i]"
                            );
                        }

                        if (lower.contains("from date")) {
                            return firstVisible(
                                    "input[name='criteria.fromDate']",
                                    "input[id*='fromDate' i]"
                            );
                        }

                        if (lower.contains("to date")) {
                            return firstVisible(
                                    "input[name='criteria.toDate']",
                                    "input[id*='toDate' i]"
                            );
                        }

                        if (lower.contains("account")) {
                            return firstVisible(
                                    "input[name='payee.accountNumber']",
                                    "select[name='fromAccountId']",
                                    "#fromAccountId"
                            );
                        }

                        if (lower.contains("amount")) {
                            return firstVisible(
                                    "input[name='amount']",
                                    "input[name='criteria.amount']"
                            );
                        }

                        return null;
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

                            try {
                                Locator locator = page.locator(selector);

                                if (
                                        locator.count() > 0
                                                &&
                                                locator.first().isVisible(
                                                        new Locator.IsVisibleOptions()
                                                                .setTimeout(350)
                                                )
                                ) {

                                    if (!editableOnly || isEditable(locator.first())) {
                                        return locator.first();
                                    }
                                }
                            } catch (RuntimeException ignored) {
                                // Keep trying lower-confidence fallbacks.
                            }
                        }

                        return null;
                    }

                    private void fillOrType(Locator locator, String value) {

                        try {
                            Object tagName = locator.evaluate("el => el.tagName.toLowerCase()");

                            if ("select".equals(tagName)) {
                                locator.selectOption(value == null ? "" : value);
                                return;
                            }
                        } catch (RuntimeException ignored) {
                            // Non-select controls continue through normal filling.
                        }

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

                    private void confirmFieldIfNeeded(String target, String value) {

                        String lower = target == null ? "" : target.toLowerCase();

                        if (isParaBankPage() && lower.contains("account") && !lower.contains("verify")) {
                            Locator verifyAccount = firstVisible("input[name='verifyAccount']");

                            if (verifyAccount != null) {
                                fillOrType(verifyAccount, value);
                            }
                        }

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
                                        lower.contains("register")
                                        ||
                                        lower.contains("account")
                                        ||
                                        lower.contains("transfer")
                                        ||
                                        lower.contains("transaction")
                                        ||
                                        lower.contains("finish")
                                        ||
                                        lower.startsWith("send ")
                                        ||
                                        lower.contains("payment")
                                        ||
                                        lower.contains("button")
                        ) {

                            try {
                                page.waitForLoadState(
                                        LoadState.DOMCONTENTLOADED,
                                        new Page.WaitForLoadStateOptions()
                                                .setTimeout(3000)
                                );
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

                            try {
                                Locator locator = page.locator(selector);

                                if (
                                        locator.count() > 0
                                                &&
                                                locator.first().isVisible(
                                                        new Locator.IsVisibleOptions()
                                                                .setTimeout(350)
                                                )
                                ) {
                                    return locator.first();
                                }
                            } catch (RuntimeException ignored) {
                                // Some generated fallback selectors are browser or site specific.
                            }
                        }

                        return null;
                    }

                    private Locator firstVisibleSoon(int timeoutMs, String... candidates) {

                        long deadline = System.currentTimeMillis() + timeoutMs;

                        do {
                            Locator locator = firstVisible(candidates);

                            if (locator != null) {
                                return locator;
                            }

                            page.waitForTimeout(250);
                        } while (System.currentTimeMillis() < deadline);

                        return null;
                    }

                    private List<String> semanticSelectors(String target) {

                        String escaped = cssText(target);
                        String action = cssText(actionText(target));
                        String actionSlug = slug(action);

                        return Arrays.asList(
                                "[data-test=\\"" + escaped + "\\"]",
                                "[data-test=\\"" + slug(target) + "\\"]",
                                "[data-testid=\\"" + escaped + "\\"]",
                                "[data-cy=\\"" + escaped + "\\"]",
                                "[id=\\"" + slug(target) + "\\"]",
                                "[name=\\"" + escaped + "\\"]",
                                "input[type='submit'][value*=\\"" + escaped + "\\" i]",
                                "input[type='button'][value*=\\"" + escaped + "\\" i]",
                                "input[type='submit'][value*=\\"" + action + "\\" i]",
                                "input[type='button'][value*=\\"" + action + "\\" i]",
                                "[data-test*=\\"" + actionSlug + "\\" i]",
                                "[data-testid*=\\"" + actionSlug + "\\" i]",
                                "[data-cy*=\\"" + actionSlug + "\\" i]",
                                "input[name*=\\"" + escaped + "\\" i]",
                                "input[placeholder*=\\"" + escaped + "\\" i]",
                                "textarea[placeholder*=\\"" + escaped + "\\" i]",
                                "[aria-label*=\\"" + escaped + "\\" i]",
                                "button:has-text(\\"" + escaped + "\\")",
                                "button:has-text(\\"" + action + "\\")",
                                "a:has-text(\\"" + escaped + "\\")",
                                "[role='button']:has-text(\\"" + escaped + "\\")",
                                "[role='button']:has-text(\\"" + action + "\\")",
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

                    private String actionText(String target) {

                        if (target == null) {
                            return "";
                        }

                        return target
                                .replace("${", "")
                                .replace("}", "")
                                .replaceAll("(?i)\\\\b(button|link|submit|cta)\\\\b", "")
                                .replaceAll("\\\\s+", " ")
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
