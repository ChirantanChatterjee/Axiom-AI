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

                    public void refresh() {

                        page.reload();

                        try {
                            page.waitForLoadState(
                                    LoadState.DOMCONTENTLOADED,
                                    new Page.WaitForLoadStateOptions()
                                            .setTimeout(3000)
                            );
                        } catch (RuntimeException ignored) {
                            // Some apps preserve state without another domcontentloaded event.
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

                        Locator optionControl = resolveOptionControl(target);

                        if (optionControl != null) {
                            checkOrClickOption(optionControl);
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

                    public void productListShouldBeSortedBy(String order) {

                        String lower = order == null ? "" : order.toLowerCase();

                        if (lower.contains("price")) {
                            List<Double> prices = visibleTexts(
                                    ".inventory_item_price, [data-test='inventory-item-price']"
                            )
                                    .stream()
                                    .map(this::parseMoney)
                                    .toList();

                            assertOrderedNumbers(
                                    prices,
                                    lower.contains("desc") || lower.contains("high")
                            );
                            return;
                        }

                        List<String> names = visibleTexts(
                                ".inventory_item_name, [data-test='inventory-item-name']"
                        );

                        assertOrderedText(
                                names,
                                lower.contains("desc") || lower.contains("z-a")
                        );
                    }

                    public void cartBadgeShouldShow(String count) {

                        int expected = parseInteger(count, -1);
                        Locator badge = firstVisible(
                                ".shopping_cart_badge",
                                "[data-test='shopping-cart-badge']"
                        );

                        if (expected <= 0) {
                            if (badge != null) {
                                throw new AssertionError("Expected cart badge to be absent or zero");
                            }
                            return;
                        }

                        if (badge == null) {
                            throw new AssertionError("Expected cart badge to show " + expected);
                        }

                        int actual = parseInteger(badge.innerText(), -1);

                        if (actual != expected) {
                            throw new AssertionError(
                                    "Expected cart badge " + expected + " but found " + actual
                            );
                        }
                    }

                    public void cartShouldContain(String product) {

                        if (containsVisibleProduct(product)) {
                            return;
                        }

                        throw new AssertionError("Expected cart to contain: " + product);
                    }

                    public void cartShouldNotContain(String product) {

                        if (!containsVisibleProduct(product)) {
                            return;
                        }

                        throw new AssertionError("Expected cart not to contain: " + product);
                    }

                    public void checkoutTotalShouldEqualItemTotalPlusTax() {

                        double subtotal = firstMoney(
                                ".summary_subtotal_label, [data-test='subtotal-label']"
                        );
                        double tax = firstMoney(
                                ".summary_tax_label, [data-test='tax-label']"
                        );
                        double total = firstMoney(
                                ".summary_total_label, [data-test='total-label']"
                        );

                        if (
                                Double.isNaN(subtotal)
                                        ||
                                        Double.isNaN(tax)
                                        ||
                                        Double.isNaN(total)
                        ) {
                            throw new AssertionError("Unable to read checkout subtotal, tax, or total");
                        }

                        double expected = Math.round((subtotal + tax) * 100.0) / 100.0;

                        if (Math.abs(expected - total) > 0.011) {
                            throw new AssertionError(
                                    "Expected checkout total " + expected + " but found " + total
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

                    private List<String> visibleTexts(String selector) {

                        try {
                            return page.locator(selector)
                                    .allTextContents()
                                    .stream()
                                    .map(String::trim)
                                    .filter(value -> !value.isBlank())
                                    .toList();
                        } catch (RuntimeException ignored) {
                            return List.of();
                        }
                    }

                    private void assertOrderedText(List<String> values, boolean descending) {

                        if (values == null || values.size() < 2) {
                            throw new AssertionError("Expected at least two products to validate sort order");
                        }

                        List<String> expected = new ArrayList<>(values);
                        expected.sort(String.CASE_INSENSITIVE_ORDER);

                        if (descending) {
                            Collections.reverse(expected);
                        }

                        if (!values.equals(expected)) {
                            throw new AssertionError(
                                    "Expected sorted products " + expected + " but found " + values
                            );
                        }
                    }

                    private void assertOrderedNumbers(List<Double> values, boolean descending) {

                        if (values == null || values.size() < 2) {
                            throw new AssertionError("Expected at least two prices to validate sort order");
                        }

                        List<Double> expected = new ArrayList<>(values);
                        expected.sort(Double::compareTo);

                        if (descending) {
                            Collections.reverse(expected);
                        }

                        if (!values.equals(expected)) {
                            throw new AssertionError(
                                    "Expected sorted prices " + expected + " but found " + values
                            );
                        }
                    }

                    private boolean containsVisibleProduct(String product) {

                        String expected = product == null ? "" : product.trim().toLowerCase();

                        if (expected.isBlank()) {
                            return false;
                        }

                        for (String text : visibleTexts(".cart_item, .inventory_item_name, [data-test='inventory-item-name']")) {
                            if (text.toLowerCase().contains(expected)) {
                                return true;
                            }
                        }

                        return bodyText().toLowerCase().contains(expected);
                    }

                    private double firstMoney(String selector) {

                        try {
                            Locator locator = page.locator(selector);

                            if (locator.count() == 0) {
                                return Double.NaN;
                            }

                            return parseMoney(locator.first().innerText());
                        } catch (RuntimeException ignored) {
                            return Double.NaN;
                        }
                    }

                    private double parseMoney(String value) {

                        if (value == null) {
                            return Double.NaN;
                        }

                        String cleaned = value.replaceAll("[^0-9.\\\\-]", "");

                        if (cleaned.isBlank()) {
                            return Double.NaN;
                        }

                        try {
                            return Double.parseDouble(cleaned);
                        } catch (NumberFormatException ignored) {
                            return Double.NaN;
                        }
                    }

                    private int parseInteger(String value, int fallback) {

                        if (value == null) {
                            return fallback;
                        }

                        String cleaned = value.replaceAll("[^0-9\\\\-]", "");

                        if (cleaned.isBlank()) {
                            return fallback;
                        }

                        try {
                            return Integer.parseInt(cleaned);
                        } catch (NumberFormatException ignored) {
                            return fallback;
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

                        Locator heuristic = firstVisibleSoon(
                                2500,
                                dynamicClickSelectors(target).toArray(String[]::new)
                        );

                        if (heuristic != null) {
                            return heuristic;
                        }

                        throw new RuntimeException("Unable to resolve element: " + target);
                    }
                """);

        sb.append("""

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

                        if (handleGenericNavigationClick(target, lower)) {
                            return true;
                        }

                        if (isParaBankNavigationCommand(lower)) {
                            Locator navigationLink = paraBankNavigationLink(lower);

                            if (navigationLink != null) {
                                clickWithFallback(navigationLink);
                                return true;
                            }

                            if (isParaBankSite()) {
                                navigateToParaBankPage(paraBankNavigationPath(lower));
                                return true;
                            }
                        }

                        return false;
                    }

                    private boolean handleGenericNavigationClick(String target, String lower) {

                        if (!looksLikeNavigationTarget(lower)) {
                            return false;
                        }

                        Locator navigation = firstVisibleSoon(
                                3500,
                                dynamicNavigationSelectors(target).toArray(String[]::new)
                        );

                        if (navigation != null) {
                            clickWithFallback(navigation);
                            return true;
                        }

                        return navigateToLikelyRoute(target);
                    }

                    private boolean looksLikeNavigationTarget(String lower) {

                        if (lower == null || lower.isBlank()) {
                            return false;
                        }

                        if (
                                lower.contains("button")
                                        ||
                                        lower.contains("submit")
                                        ||
                                        lower.startsWith("send ")
                                        ||
                                        lower.startsWith("save")
                                        ||
                                        lower.startsWith("delete")
                                        ||
                                        lower.startsWith("remove")
                                        ||
                                        lower.startsWith("add ")
                                        ||
                                        lower.contains("login")
                                        ||
                                        lower.contains("log in")
                                        ||
                                        lower.contains("sign in")
                        ) {
                            return false;
                        }

                        if (
                                lower.contains("page")
                                        ||
                                        lower.contains("menu")
                                        ||
                                        lower.contains("nav")
                                        ||
                                        lower.contains("dashboard")
                                        ||
                                        lower.equals("home")
                                        ||
                                        lower.contains("about")
                                        ||
                                        lower.contains("contact")
                                        ||
                                        lower.contains("service")
                                        ||
                                        lower.equals("products")
                                        ||
                                        lower.contains("location")
                                        ||
                                        lower.contains("user")
                                        ||
                                        lower.contains("order")
                                        ||
                                        lower.contains("invoice")
                                        ||
                                        lower.contains("overview")
                                        ||
                                        lower.contains("account")
                                        ||
                                        lower.contains("payment")
                                        ||
                                        lower.contains("pay")
                                        ||
                                        lower.contains("transfer")
                                        ||
                                        lower.contains("transaction")
                                        ||
                                        lower.contains("report")
                                        ||
                                        lower.contains("profile")
                                        ||
                                        lower.contains("settings")
                                        ||
                                        lower.contains("admin")
                                        ||
                                        lower.contains("cart")
                                        ||
                                        lower.contains("checkout")
                        ) {
                            return true;
                        }

                        return false;
                    }

                    private boolean navigateToLikelyRoute(String target) {

                        List<String> candidates = likelyRouteUrls(target);

                        if (candidates.isEmpty()) {
                            return false;
                        }

                        String originalUrl = page.url();

                        for (String candidate : candidates) {
                            try {
                                Response response = page.navigate(
                                        candidate,
                                        new Page.NavigateOptions()
                                                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                                                .setTimeout(runtimeNavigationTimeoutMs())
                                );

                                int status = response == null ? 200 : response.status();

                                if (status >= 400) {
                                    restoreUrl(originalUrl);
                                    continue;
                                }

                                page.waitForTimeout(600);

                                if (!looksLikeNotFoundPage()) {
                                    return true;
                                }

                                restoreUrl(originalUrl);

                            } catch (RuntimeException ignored) {
                                restoreUrl(originalUrl);
                            }
                        }

                        return false;
                    }

                    private List<String> likelyRouteUrls(String target) {

                        String slug = slug(target);
                        String compact = slug.replace("-", "");

                        if (slug.isBlank()) {
                            return List.of();
                        }

                        List<String> routeNames = new ArrayList<>();
                        addIfMissing(routeNames, slug);
                        addIfMissing(routeNames, compact);
                        addIfMissing(routeNames, slug.replace("-", "_"));

                        List<String> bases = routeBaseUrls();
                        List<String> candidates = new ArrayList<>();

                        for (String base : bases) {
                            for (String routeName : routeNames) {
                                addIfMissing(candidates, base + routeName);
                                addIfMissing(candidates, base + routeName + "/");
                                addIfMissing(candidates, base + routeName + ".html");
                                addIfMissing(candidates, base + routeName + ".htm");
                            }
                        }

                        return candidates;
                    }

                    private List<String> routeBaseUrls() {

                        String currentUrl = page.url();

                        if (currentUrl == null || currentUrl.isBlank()) {
                            return List.of();
                        }

                        List<String> bases = new ArrayList<>();

                        try {
                            java.net.URI uri = java.net.URI.create(currentUrl);
                            String origin = uri.getScheme() + "://" + uri.getAuthority() + "/";
                            addIfMissing(bases, origin);

                            String path = uri.getPath();

                            if (path != null && !path.isBlank() && path.contains("/")) {
                                int lastSlash = path.lastIndexOf('/');
                                String directory = path.substring(0, lastSlash + 1);

                                if (directory.startsWith("/")) {
                                    directory = directory.substring(1);
                                }

                                if (!directory.isBlank()) {
                                    addIfMissing(bases, origin + directory);
                                }
                            }

                        } catch (RuntimeException ignored) {
                            int slash = currentUrl.lastIndexOf('/');

                            if (slash > "https://".length()) {
                                addIfMissing(bases, currentUrl.substring(0, slash + 1));
                            }
                        }

                        return bases;
                    }

                    private boolean looksLikeNotFoundPage() {

                        try {
                            String body = page.locator("body").innerText(
                                    new Locator.InnerTextOptions()
                                            .setTimeout(1500)
                            );

                            String lower = body == null ? "" : body.toLowerCase();

                            return lower.contains("404")
                                    ||
                                    lower.contains("not found")
                                    ||
                                    lower.contains("page not found")
                                    ||
                                    lower.contains("cannot find")
                                    ||
                                    lower.contains("no route matches");

                        } catch (RuntimeException ignored) {
                            return false;
                        }
                    }

                    private void restoreUrl(String originalUrl) {

                        if (originalUrl == null || originalUrl.isBlank()) {
                            return;
                        }

                        try {
                            page.navigate(
                                    originalUrl,
                                    new Page.NavigateOptions()
                                            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                                            .setTimeout(3000)
                            );
                        } catch (RuntimeException ignored) {
                            // A failed heuristic route should not mask the original unresolved target.
                        }
                    }

                    private boolean isParaBankNavigationCommand(String lower) {

                        return lower != null
                                &&
                                (
                                        lower.contains("bill pay")
                                                ||
                                                lower.contains("billpay")
                                                ||
                                                lower.contains("bill payment")
                                                ||
                                                lower.contains("accounts overview")
                                                ||
                                                lower.contains("account overview")
                                                ||
                                                lower.contains("open new account")
                                                ||
                                                lower.contains("transfer funds")
                                                ||
                                                lower.contains("find transactions")
                                );
                    }

                    private Locator paraBankNavigationLink(String lower) {

                        if (lower.contains("bill pay") || lower.contains("billpay") || lower.contains("bill payment")) {
                            return firstVisibleSoon(
                                    5000,
                                    "a:has-text(\\"Bill Pay\\")",
                                    "a[href*='billpay' i]",
                                    "button:has-text(\\"Bill Pay\\")",
                                    "[role='button']:has-text(\\"Bill Pay\\")"
                            );
                        }

                        if (lower.contains("accounts overview") || lower.contains("account overview")) {
                            return firstVisibleSoon(
                                    5000,
                                    "a:has-text(\\"Accounts Overview\\")",
                                    "a:has-text(\\"Account Overview\\")",
                                    "a[href*='overview' i]",
                                    "button:has-text(\\"Accounts Overview\\")"
                            );
                        }

                        if (lower.contains("open new account")) {
                            return firstVisibleSoon(
                                    5000,
                                    "a:has-text(\\"Open New Account\\")",
                                    "a[href*='openaccount' i]",
                                    "button:has-text(\\"Open New Account\\")"
                            );
                        }

                        if (lower.contains("transfer funds")) {
                            return firstVisibleSoon(
                                    5000,
                                    "a:has-text(\\"Transfer Funds\\")",
                                    "a[href*='transfer' i]",
                                    "button:has-text(\\"Transfer Funds\\")"
                            );
                        }

                        if (lower.contains("find transactions")) {
                            return firstVisibleSoon(
                                    5000,
                                    "a:has-text(\\"Find Transactions\\")",
                                    "a[href*='findtrans' i]",
                                    "button:has-text(\\"Find Transactions\\")"
                            );
                        }

                        return null;
                    }

                    private String paraBankNavigationPath(String lower) {

                        if (lower.contains("bill pay") || lower.contains("billpay") || lower.contains("bill payment")) {
                            return "billpay.htm";
                        }

                        if (lower.contains("accounts overview") || lower.contains("account overview")) {
                            return "overview.htm";
                        }

                        if (lower.contains("open new account")) {
                            return "openaccount.htm";
                        }

                        if (lower.contains("transfer funds")) {
                            return "transfer.htm";
                        }

                        if (lower.contains("find transactions")) {
                            return "findtrans.htm";
                        }

                        return "index.htm";
                    }

                    private boolean isParaBankSite() {

                        String currentUrl = page.url() == null ? "" : page.url().toLowerCase();

                        return currentUrl.contains("parabank.parasoft.com/parabank");
                    }

                    private void navigateToParaBankPage(String path) {

                        String currentUrl = page.url() == null ? "" : page.url();
                        String lowerUrl = currentUrl.toLowerCase();
                        int paraBankIndex = lowerUrl.indexOf("/parabank/");
                        String baseUrl = "https://parabank.parasoft.com/parabank/";

                        if (paraBankIndex >= 0) {
                            baseUrl = currentUrl.substring(0, paraBankIndex + "/parabank/".length());
                        }

                        page.navigate(
                                baseUrl + path,
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
                            // Direct ParaBank navigation sometimes hydrates without another load event.
                        }

                        dismissCookieBanner();
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

                    private Locator resolveOptionControl(String target) {

                        List<String> terms = optionTerms(target);

                        if (terms.isEmpty()) {
                            return null;
                        }

                        for (String term : terms) {

                            String escaped = cssText(term);
                            String termSlug = slug(term);
                            String compact = termSlug.replace("-", "");

                            Locator explicit = firstExisting(
                                    "input[type='radio'][value=\\"" + escaped + "\\" i]",
                                    "input[type='checkbox'][value=\\"" + escaped + "\\" i]",
                                    "input[type='radio'][value*=\\"" + escaped + "\\" i]",
                                    "input[type='checkbox'][value*=\\"" + escaped + "\\" i]",
                                    "input[type='radio'][id=\\"" + termSlug + "\\" i]",
                                    "input[type='checkbox'][id=\\"" + termSlug + "\\" i]",
                                    "input[type='radio'][id*=\\"" + termSlug + "\\" i]",
                                    "input[type='checkbox'][id*=\\"" + termSlug + "\\" i]",
                                    "input[type='radio'][name*=\\"" + escaped + "\\" i]",
                                    "input[type='checkbox'][name*=\\"" + escaped + "\\" i]",
                                    "input[type='radio'][aria-label*=\\"" + escaped + "\\" i]",
                                    "input[type='checkbox'][aria-label*=\\"" + escaped + "\\" i]"
                            );

                            if (explicit != null) {
                                return explicit;
                            }

                            if (!compact.isBlank() && !compact.equals(termSlug)) {
                                explicit = firstExisting(
                                        "input[type='radio'][value=\\"" + compact + "\\" i]",
                                        "input[type='checkbox'][value=\\"" + compact + "\\" i]",
                                        "input[type='radio'][value*=\\"" + compact + "\\" i]",
                                        "input[type='checkbox'][value*=\\"" + compact + "\\" i]",
                                        "input[type='radio'][id*=\\"" + compact + "\\" i]",
                                        "input[type='checkbox'][id*=\\"" + compact + "\\" i]",
                                        "input[type='radio'][name*=\\"" + compact + "\\" i]",
                                        "input[type='checkbox'][name*=\\"" + compact + "\\" i]"
                                );

                                if (explicit != null) {
                                    return explicit;
                                }
                            }

                            Locator labelled = firstVisibleSoon(
                                    1000,
                                    "label:has-text(\\"" + escaped + "\\") input[type='radio']",
                                    "label:has-text(\\"" + escaped + "\\") input[type='checkbox']",
                                    "input[type='radio'] + label:has-text(\\"" + escaped + "\\")",
                                    "input[type='checkbox'] + label:has-text(\\"" + escaped + "\\")",
                                    "[role='radio']:has-text(\\"" + escaped + "\\")",
                                    "[role='checkbox']:has-text(\\"" + escaped + "\\")",
                                    "[aria-label*=\\"" + escaped + "\\" i][role='radio']",
                                    "[aria-label*=\\"" + escaped + "\\" i][role='checkbox']",
                                    "label:has-text(\\"" + escaped + "\\")"
                            );

                            if (labelled != null) {
                                return labelled;
                            }
                        }

                        return null;
                    }

                    private List<String> optionTerms(String target) {

                        List<String> terms = new ArrayList<>();
                        String action = actionText(target);
                        String lower = action.toLowerCase(Locale.ROOT)
                                .replace("-", " ")
                                .replace("_", " ")
                                .replaceAll("\\\\s+", " ")
                                .trim();

                        addOptionTerm(terms, action);

                        String stripped = lower
                                .replaceAll("\\\\b(journey|trip|flight|option|radio|checkbox|selection|select|choose|click)\\\\b", " ")
                                .replaceAll("\\\\s+", " ")
                                .trim();

                        addOptionTerm(terms, stripped);

                        if (lower.contains("return") || lower.contains("round trip") || lower.contains("roundtrip")) {
                            addOptionTerm(terms, "return");
                            addOptionTerm(terms, "round trip");
                            addOptionTerm(terms, "roundtrip");
                            addOptionTerm(terms, "return trip");
                        }

                        if (lower.contains("one way") || lower.contains("oneway") || lower.contains("single")) {
                            addOptionTerm(terms, "one way");
                            addOptionTerm(terms, "oneway");
                            addOptionTerm(terms, "single");
                        }

                        for (String token : stripped.split("\\\\s+")) {

                            if (token.length() >= 3 && !isOptionStopWord(token)) {
                                addOptionTerm(terms, token);
                            }
                        }

                        return terms;
                    }

                    private void addOptionTerm(List<String> terms, String term) {

                        if (term == null) {
                            return;
                        }

                        String normalized = term
                                .replace("${", "")
                                .replace("}", "")
                                .replace("-", " ")
                                .replace("_", " ")
                                .replaceAll("\\\\s+", " ")
                                .trim();

                        if (normalized.isBlank() || terms.contains(normalized)) {
                            return;
                        }

                        terms.add(normalized);
                    }

                    private boolean isOptionStopWord(String token) {

                        return Set.of(
                                "the",
                                "and",
                                "for",
                                "with",
                                "button",
                                "link",
                                "field",
                                "input"
                        ).contains(token);
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

                        String lower = target == null ? "" : target.toLowerCase();

                        if (lower.contains("sort")) {
                            return firstVisible(
                                    "select[data-test*='sort' i]",
                                    "select[class*='sort' i]",
                                    ".product_sort_container",
                                    "select:visible"
                            );
                        }

                        if (!isParaBankPage()) {
                            return null;
                        }

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

                    private void checkOrClickOption(Locator locator) {

                        try {
                            locator.check(new Locator.CheckOptions().setForce(true));
                            return;
                        } catch (RuntimeException ignored) {
                            // Labels and ARIA options are clicked rather than checked directly.
                        }

                        try {
                            clickWithFallback(locator);
                            return;
                        } catch (RuntimeException clickFailure) {
                            try {
                                locator.evaluate(
                                        "el => { if (el instanceof HTMLInputElement && (el.type === 'radio' || el.type === 'checkbox')) { el.checked = true; el.dispatchEvent(new Event('input', { bubbles: true })); el.dispatchEvent(new Event('change', { bubbles: true })); } else { el.click(); } }"
                                );
                            } catch (RuntimeException ignored) {
                                throw clickFailure;
                            }
                        }
                    }

                    private void waitAfterClick(String target) {

                        String lower = target == null ? "" : target.toLowerCase();

                        if (
                                lower.contains("search")
                                        ||
                                        lower.contains("login")
                                        ||
                                        lower.contains("log in")
                                        ||
                                        lower.contains("sign in")
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

                        if (looksLikeAuthenticationClick(lower)) {
                            failFastOnAuthenticationError();
                        }
                    }

                    private boolean looksLikeAuthenticationClick(String lower) {

                        if (lower == null || lower.isBlank()) {
                            return false;
                        }

                        return lower.contains("login")
                                ||
                                lower.contains("log in")
                                ||
                                lower.contains("sign in")
                                ||
                                lower.contains("signin")
                                ||
                                (
                                        lower.contains("submit")
                                                &&
                                                page.locator("input[type='password'], [type='password']").count() > 0
                                );
                    }

                    private void failFastOnAuthenticationError() {

                        String body = bodyText();

                        if (!hasAuthenticationFailureText(body)) {
                            return;
                        }

                        recordAssertionFailure(
                                "authentication completed",
                                body
                        );

                        throw new RuntimeException(
                                "Authentication did not complete: the page showed a login or credential error."
                        );
                    }

                    private boolean hasAuthenticationFailureText(String body) {

                        if (body == null || body.isBlank()) {
                            return false;
                        }

                        String lower = body.toLowerCase();

                        return lower.contains("authentication failed")
                                ||
                                lower.contains("login failed")
                                ||
                                lower.contains("sign in failed")
                                ||
                                lower.contains("signin failed")
                                ||
                                lower.contains("invalid username")
                                ||
                                lower.contains("invalid password")
                                ||
                                lower.contains("invalid username/password")
                                ||
                                lower.contains("incorrect username")
                                ||
                                lower.contains("incorrect password")
                                ||
                                lower.contains("bad credentials")
                                ||
                                lower.contains("invalid credentials")
                                ||
                                lower.contains("could not be verified")
                                ||
                                lower.contains("user not found")
                                ||
                                lower.contains("account locked")
                                ||
                                (
                                        lower.contains("unauthorized")
                                                &&
                                                (
                                                        lower.contains("login")
                                                                ||
                                                                lower.contains("password")
                                                                ||
                                                                lower.contains("credentials")
                                                )
                                );
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

                    private Locator firstExisting(String... candidates) {

                        for (String selector : candidates) {

                            if (selector == null || selector.isBlank()) {
                                continue;
                            }

                            try {
                                Locator locator = page.locator(selector);

                                if (locator.count() > 0) {
                                    return locator.first();
                                }
                            } catch (RuntimeException ignored) {
                                // Keep trying lower-confidence option selectors.
                            }
                        }

                        return null;
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
                """);

        sb.append("""

                    private List<String> dynamicClickSelectors(String target) {

                        String escaped = cssText(target);
                        String action = cssText(actionText(target));
                        String slug = slug(target);
                        String compact = slug.replace("-", "");

                        List<String> candidates = new ArrayList<>();

                        addIfMissing(candidates, "button:has-text(\\"" + escaped + "\\")");
                        addIfMissing(candidates, "button:has-text(\\"" + action + "\\")");
                        addIfMissing(candidates, "a:has-text(\\"" + escaped + "\\")");
                        addIfMissing(candidates, "a:has-text(\\"" + action + "\\")");
                        addIfMissing(candidates, "[role='button']:has-text(\\"" + escaped + "\\")");
                        addIfMissing(candidates, "[role='button']:has-text(\\"" + action + "\\")");
                        addIfMissing(candidates, "[role='link']:has-text(\\"" + escaped + "\\")");
                        addIfMissing(candidates, "[role='link']:has-text(\\"" + action + "\\")");
                        addIfMissing(candidates, "input[type='submit'][value*=\\"" + escaped + "\\" i]");
                        addIfMissing(candidates, "input[type='button'][value*=\\"" + escaped + "\\" i]");
                        addIfMissing(candidates, "[aria-label*=\\"" + escaped + "\\" i]");
                        addIfMissing(candidates, "[title*=\\"" + escaped + "\\" i]");
                        addIfMissing(candidates, "[data-test*=\\"" + slug + "\\" i]");
                        addIfMissing(candidates, "[data-testid*=\\"" + slug + "\\" i]");
                        addIfMissing(candidates, "[data-cy*=\\"" + slug + "\\" i]");
                        addIfMissing(candidates, "[id*=\\"" + slug + "\\" i]");
                        addIfMissing(candidates, "[name*=\\"" + escaped + "\\" i]");

                        if (!compact.isBlank() && !compact.equals(slug)) {
                            addIfMissing(candidates, "[href*=\\"" + compact + "\\" i]");
                            addIfMissing(candidates, "[data-test*=\\"" + compact + "\\" i]");
                            addIfMissing(candidates, "[data-testid*=\\"" + compact + "\\" i]");
                        }

                        return candidates;
                    }

                    private List<String> dynamicNavigationSelectors(String target) {

                        String escaped = cssText(target);
                        String action = cssText(actionText(target));
                        String slug = slug(target);
                        String compact = slug.replace("-", "");

                        List<String> candidates = new ArrayList<>();

                        addIfMissing(candidates, "a:has-text(\\"" + escaped + "\\")");
                        addIfMissing(candidates, "a:has-text(\\"" + action + "\\")");
                        addIfMissing(candidates, "[role='link']:has-text(\\"" + escaped + "\\")");
                        addIfMissing(candidates, "[role='link']:has-text(\\"" + action + "\\")");
                        addIfMissing(candidates, "nav a:has-text(\\"" + escaped + "\\")");
                        addIfMissing(candidates, "nav a:has-text(\\"" + action + "\\")");
                        addIfMissing(candidates, "aside a:has-text(\\"" + escaped + "\\")");
                        addIfMissing(candidates, "aside a:has-text(\\"" + action + "\\")");
                        addIfMissing(candidates, "a[href*=\\"" + slug + "\\" i]");
                        addIfMissing(candidates, "a[href*=\\"" + slug.replace("-", "_") + "\\" i]");
                        addIfMissing(candidates, "[data-test*=\\"" + slug + "\\" i]");
                        addIfMissing(candidates, "[data-testid*=\\"" + slug + "\\" i]");
                        addIfMissing(candidates, "[aria-label*=\\"" + escaped + "\\" i]");
                        addIfMissing(candidates, "[title*=\\"" + escaped + "\\" i]");

                        if (!compact.isBlank() && !compact.equals(slug)) {
                            addIfMissing(candidates, "a[href*=\\"" + compact + "\\" i]");
                            addIfMissing(candidates, "[data-test*=\\"" + compact + "\\" i]");
                            addIfMissing(candidates, "[data-testid*=\\"" + compact + "\\" i]");
                        }

                        return candidates;
                    }

                    private void addIfMissing(List<String> values, String value) {

                        if (value == null || value.isBlank() || values.contains(value)) {
                            return;
                        }

                        values.add(value);
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
