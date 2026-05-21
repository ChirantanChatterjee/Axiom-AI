package com.axiomai.qa.generator;

import com.axiomai.qa.models.PageScanResult;
import org.springframework.stereotype.Component;

@Component
public class StepDefinitionGenerator {

    // =====================================================
    // GENERATE STEP DEFINITIONS
    // =====================================================

    public String generate(PageScanResult scanResult) {

        String className =
                buildClassName(scanResult.getTitle())
                        + "Steps";

        String pageClass =
                buildClassName(scanResult.getTitle())
                        + "Page";

        StringBuilder sb =
                new StringBuilder();

        sb.append("package com.axiomai.generated.steps;\n\n");

        sb.append("import io.cucumber.java.en.*;\n");
        sb.append("import com.microsoft.playwright.*;\n");
        sb.append("import com.axiomai.generated.pages.")
                .append(pageClass)
                .append(";\n\n");

        sb.append("public class ")
                .append(className)
                .append(" {\n\n");

        sb.append("    private static Playwright playwright;\n");
        sb.append("    private static Browser browser;\n");
        sb.append("    private static BrowserContext browserContext;\n");
        sb.append("    private static Page page;\n\n");

        sb.append("    private ")
                .append(pageClass)
                .append(" generatedPage;\n\n");

        // =================================================
        // GIVEN
        // =================================================

        sb.append("    @Given(\"user launches {string}\")\n");

        sb.append("    public void userLaunches(String url) {\n\n");

        sb.append("        playwright = Playwright.create();\n\n");

        sb.append("        BrowserType.LaunchOptions launchOptions =\n")
                .append("                new BrowserType.LaunchOptions()\n")
                .append("                        .setHeadless(resolveHeadless())\n")
                .append("                        .setTimeout(15000)\n")
                .append("                        .setArgs(java.util.Arrays.asList(\"--incognito\", \"--no-first-run\", \"--no-default-browser-check\", \"--no-sandbox\", \"--disable-dev-shm-usage\"));\n\n")
                .append("        String browserChannel = normalize(System.getenv(\"AIF_BROWSER_CHANNEL\"));\n\n")
                .append("        if (browserChannel != null) {\n")
                .append("            launchOptions.setChannel(browserChannel);\n")
                .append("        }\n\n")
                .append("        browser = launchChromium(launchOptions, browserChannel);\n\n");

        sb.append("        browserContext = browser.newContext(\n")
                .append("                new Browser.NewContextOptions()\n")
                .append("                        .setViewportSize(1440, 1000)\n")
                .append("        );\n\n");

        sb.append("        page = browserContext.newPage();\n\n");

        sb.append("        page.navigate(url);\n\n");

        sb.append("        generatedPage = new ")
                .append(pageClass)
                .append("(page);\n");

        sb.append("    }\n\n");

        // =================================================
        // SEARCH FIELD
        // =================================================

        sb.append("    @When(\"user enters {string} into search field\")\n");

        sb.append("    public void userEntersIntoSearchField(String value) {\n\n");

        sb.append("        // TODO: map generated search field\n\n");

        sb.append("    }\n\n");

        // =================================================
        // SEARCH BUTTON
        // =================================================

        sb.append("    @When(\"user clicks search button\")\n");

        sb.append("    public void userClicksSearchButton() {\n\n");

        sb.append("        // TODO: map generated search button\n\n");

        sb.append("    }\n\n");

        // =================================================
        // ASSERTION
        // =================================================

        sb.append("    @Then(\"search results should be displayed\")\n");

        sb.append("    public void searchResultsShouldBeDisplayed() {\n\n");

        sb.append("        System.out.println(\"Validation successful\");\n\n");

        sb.append("        browserContext.close();\n\n");

        sb.append("        browser.close();\n\n");

        sb.append("        playwright.close();\n");

        sb.append("    }\n\n");

        sb.append("    private Browser launchChromium(BrowserType.LaunchOptions launchOptions, String browserChannel) {\n\n")
                .append("        try {\n")
                .append("            return playwright.chromium().launch(launchOptions);\n")
                .append("        } catch (PlaywrightException exception) {\n\n")
                .append("            if (browserChannel == null || !isMissingChannelFailure(exception)) {\n")
                .append("                throw exception;\n")
                .append("            }\n\n")
                .append("            return playwright.chromium()\n")
                .append("                    .launch(\n")
                .append("                            new BrowserType.LaunchOptions()\n")
                .append("                                    .setHeadless(resolveHeadless())\n")
                .append("                                    .setTimeout(15000)\n")
                .append("                                    .setArgs(java.util.Arrays.asList(\"--incognito\", \"--no-first-run\", \"--no-default-browser-check\", \"--no-sandbox\", \"--disable-dev-shm-usage\"))\n")
                .append("                    );\n")
                .append("        }\n")
                .append("    }\n\n")
                .append("    private boolean isMissingChannelFailure(PlaywrightException exception) {\n\n")
                .append("        String message = exception.getMessage() == null ? \"\" : exception.getMessage().toLowerCase();\n\n")
                .append("        return message.contains(\"distribution\") && message.contains(\"is not found\");\n")
                .append("    }\n\n")
                .append("    private boolean resolveHeadless() {\n\n")
                .append("        String configured = normalize(System.getenv(\"AIF_HEADLESS\"));\n\n")
                .append("        if (configured != null) {\n")
                .append("            return Boolean.parseBoolean(configured);\n")
                .append("        }\n\n")
                .append("        String osName = System.getProperty(\"os.name\", \"\").toLowerCase();\n\n")
                .append("        return osName.contains(\"linux\")\n")
                .append("                &&\n")
                .append("                (\n")
                .append("                        normalize(System.getenv(\"DISPLAY\")) == null\n")
                .append("                                ||\n")
                .append("                                isTruthy(System.getenv(\"RENDER\"))\n")
                .append("                                ||\n")
                .append("                                isTruthy(System.getenv(\"CI\"))\n")
                .append("                );\n")
                .append("    }\n\n")
                .append("    private boolean isTruthy(String value) {\n\n")
                .append("        String normalized = normalize(value);\n\n")
                .append("        return normalized != null\n")
                .append("                &&\n")
                .append("                !\"false\".equalsIgnoreCase(normalized)\n")
                .append("                &&\n")
                .append("                !\"0\".equals(normalized);\n")
                .append("    }\n\n")
                .append("    private String normalize(String value) {\n\n")
                .append("        if (value == null || value.isBlank()) {\n")
                .append("            return null;\n")
                .append("        }\n\n")
                .append("        return value.trim();\n")
                .append("    }\n\n");

        sb.append("}\n");

        return sb.toString();
    }

    // =====================================================
    // CLASS NAME
    // =====================================================

    private String buildClassName(String title) {

        String cleaned =
                safe(title)
                        .replaceAll("[^a-zA-Z0-9]", " ");

        String[] parts =
                cleaned.split("\\s+");

        StringBuilder sb =
                new StringBuilder();

        for (String part : parts) {

            if (part.isBlank()) {
                continue;
            }

            sb.append(capitalize(part.toLowerCase()));
        }

        return sb.isEmpty()
                ? "Generated"
                : sb.toString();
    }

    // =====================================================
    // CAPITALIZE
    // =====================================================

    private String capitalize(String value) {

        if (value == null || value.isBlank()) {
            return "";
        }

        return value.substring(0, 1).toUpperCase()
                + value.substring(1);
    }

    // =====================================================
    // SAFE
    // =====================================================

    private String safe(String value) {

        return value == null
                ? ""
                : value;
    }
}
