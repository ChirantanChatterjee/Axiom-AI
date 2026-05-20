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

        sb.append("        browser = playwright.chromium()\n")
                .append("                .launch(\n")
                .append("                        new BrowserType.LaunchOptions()\n")
                .append("                                .setHeadless(false)\n")
                .append("                                .setArgs(java.util.Arrays.asList(\"--incognito\", \"--start-maximized\", \"--start-fullscreen\", \"--no-first-run\", \"--no-default-browser-check\"))\n")
                .append("                );\n\n");

        sb.append("        browserContext = browser.newContext(\n")
                .append("                new Browser.NewContextOptions()\n")
                .append("                        .setViewportSize(null)\n")
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
