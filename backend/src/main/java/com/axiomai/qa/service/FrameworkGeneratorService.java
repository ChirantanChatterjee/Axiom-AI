package com.axiomai.qa.service;

import com.axiomai.qa.flow.DetectedFlow;
import com.axiomai.qa.generator.flow.FlowFrameworkAssembler;
import com.axiomai.qa.models.GeneratedFlowFramework;
import com.axiomai.qa.models.GeneratedFramework;
import com.axiomai.qa.models.RequirementTestCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FrameworkGeneratorService {

    private final FlowFrameworkAssembler
            flowFrameworkAssembler;

    // =====================================================
    // MAIN GENERATION
    // =====================================================

    public GeneratedFramework generate(
            List<DetectedFlow> flows
    ) {

        GeneratedFlowFramework generated =
                flowFrameworkAssembler
                        .assemble(flows);

        GeneratedFramework framework =
                new GeneratedFramework(
                generated.getFeatureFile(),
                generated.getPageObject(),
                generated.getStepDefinitions()
        );

        framework.setTestCases(
                generateFlowTestCases(flows)
        );

        return framework;
    }

    private List<RequirementTestCase> generateFlowTestCases(
            List<DetectedFlow> flows
    ) {

        List<RequirementTestCase> testCases =
                new ArrayList<>();

        if (
                flows == null
                        ||
                        flows.isEmpty()
        ) {

            return testCases;
        }

        int storyIndex =
                1;

        for (
                DetectedFlow flow
                : flows
        ) {

            if (
                    flow == null
            ) {

                continue;
            }

            String story =
                    "US-%03d".formatted(storyIndex++);

            addFlowCases(
                    testCases,
                    story,
                    flow
            );
        }

        return testCases;
    }

    private void addFlowCases(
            List<RequirementTestCase> testCases,
            String story,
            DetectedFlow flow
    ) {

        String type =
                safe(flow.getFlowType())
                        .toUpperCase();

        if (
                type.contains("LOGIN")
        ) {

            addCase(testCases, story, "Login with valid credentials", "Valid username and password", "Authenticated area is opened", flow, "positive");
            addCase(testCases, story, "Login with invalid credentials", "Invalid username and password", "Login validation error appears", flow, "negative");
            addCase(testCases, story, "Login with required fields blank", "Blank username and password", "Required field validation appears", flow, "negative");
            return;
        }

        if (
                type.contains("PRODUCT_SORT")
        ) {

            addCase(testCases, story, "Sort products by name and price", "Name A-Z, Name Z-A, price low-high, price high-low", "Product order matches selected sort option", flow, "positive");
            addCase(testCases, story, "Repeat product sorting after changing option", "Multiple sequential sort values", "Product list remains stable and sorted", flow, "boundary");
            return;
        }

        if (
                type.contains("ADD_TO_CART")
        ) {

            String product =
                    productName(flow);

            addCase(testCases, story, "Add product to cart", product.isBlank() ? "Available product" : product, "Cart badge increments and cart contains product", flow, "positive");
            addCase(testCases, story, "Open cart before adding another product", "No additional product selected", "Cart does not contain unexpected products", flow, "negative");
            return;
        }

        if (
                type.contains("REMOVE_FROM_CART")
        ) {

            addCase(testCases, story, "Remove product from cart", "Product already in cart", "Cart badge decreases and product is removed", flow, "positive");
            addCase(testCases, story, "Remove already removed product", "Product removed once", "Cart remains stable without duplicate removal", flow, "negative");
            return;
        }

        if (
                type.contains("CART_NAVIGATION")
        ) {

            addCase(testCases, story, "Open cart from application header", "Cart icon/link", "Cart page is displayed", flow, "positive");
            addCase(testCases, story, "Open cart when no product is selected", "Empty cart", "Cart page remains readable with no unexpected product", flow, "negative");
            return;
        }

        if (
                type.contains("CHECKOUT")
        ) {

            addCase(testCases, story, "Continue checkout with valid customer information", "Valid first name, last name, and postal code", "Checkout overview is displayed", flow, "positive");
            addCase(testCases, story, "Continue checkout with required fields blank", "Blank checkout information", "Required field validation appears", flow, "negative");
            return;
        }

        if (
                type.contains("SEARCH")
        ) {

            addCase(testCases, story, "Search with valid text", "Known search keyword", "Relevant search results or search state appears", flow, "positive");
            addCase(testCases, story, "Search with blank or unusual text", "Blank text and special characters", "Search validation or empty result state appears", flow, "negative");
            return;
        }

        if (
                type.contains("FORM")
        ) {

            addCase(testCases, story, "Submit form with valid field values", "Valid values for all visible fields", "Form submission completes successfully", flow, "positive");
            addCase(testCases, story, "Submit form with required fields blank", "Blank required fields", "Required field validation appears", flow, "negative");
            return;
        }

        addCase(testCases, story, "Execute generated flow successfully", "Crawler-observed valid data", "Flow completes successfully", flow, "positive");
        addCase(testCases, story, "Execute generated flow with missing required data", "Missing or blank required values", "Validation error appears", flow, "negative");
    }

    private void addCase(
            List<RequirementTestCase> testCases,
            String story,
            String scenario,
            String testData,
            String expected,
            DetectedFlow flow,
            String category
    ) {

        String tcId =
                "TC-%03d".formatted(
                        testCases.size() + 1
                );

        Set<String> tags =
                new LinkedHashSet<>();

        tags.add("generated");
        tags.add("ai_requirement");
        tags.add("requirements");
        tags.add(tag(story));
        tags.add(tag(flow.getFlowType()));
        tags.add(tag(category));

        testCases.add(
                new RequirementTestCase(
                        tcId,
                        story,
                        scenario,
                        testData,
                        expected,
                        new ArrayList<>(tags)
                )
        );
    }

    private String productName(
            DetectedFlow flow
    ) {

        if (
                flow == null
                        ||
                        flow.getSteps() == null
        ) {

            return "";
        }

        for (
                com.axiomai.qa.models.FlowStep step
                : flow.getSteps()
        ) {

            String target =
                    safe(step.getTarget());

            String product =
                    target.replaceAll(
                                    "(?i)\\b(add|remove|to|cart)\\b",
                                    " "
                            )
                            .trim()
                            .replaceAll("\\s+", " ");

            if (
                    !product.isBlank()
                            &&
                            !product.equalsIgnoreCase(target)
            ) {

                return product;
            }
        }

        return "";
    }

    private String tag(
            String value
    ) {

        String tag =
                safe(value)
                        .toLowerCase()
                        .replaceAll("[^a-z0-9]+", "_")
                        .replaceAll("^_+|_+$", "");

        return tag.isBlank()
                ? "generated_flow"
                : tag;
    }

    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }

    // =====================================================
    // FEATURE FILE
    // =====================================================

    private String generateFeature(
            List<DetectedFlow> flows
    ) {

        StringBuilder builder =
                new StringBuilder();

        builder.append(
                "Feature: AI Generated Flow\n\n"
        );

        int scenarioCounter = 1;

        for (DetectedFlow flow : flows) {

            builder.append(
                    "  Scenario: Generated Flow "
                            + scenarioCounter++
                            + "\n"
            );

            builder.append(
                    "    Given user opens application\n"
            );

            builder.append(
                    "    When user performs generated actions\n"
            );

            builder.append(
                    "    Then flow should complete successfully\n\n"
            );
        }

        return builder.toString();
    }

    // =====================================================
    // PAGE OBJECT
    // =====================================================

    private String generatePageObject(
            List<DetectedFlow> flows
    ) {

        return """
package com.axiomai.generated.pages;

import com.microsoft.playwright.*;

public class GeneratedPage {

    private final Page page;

    public GeneratedPage(Page page) {

        this.page = page;
    }

    public Locator locate(String selector) {

        return page.locator(selector);
    }
}
""";
    }

    // =====================================================
    // STEP DEFINITIONS
    // =====================================================

    private String generateStepDefinitions(
            List<DetectedFlow> flows
    ) {

        return """
package com.axiomai.generated.steps;

import io.cucumber.java.en.*;

import com.microsoft.playwright.*;

import com.axiomai.generated.pages.GeneratedPage;

public class GeneratedSteps {

    private static Playwright playwright;

    private static Browser browser;

    private static Page page;

    private GeneratedPage generatedPage;

    @Given("user opens application")
    public void openApplication() {

        playwright = Playwright.create();

        browser =
                playwright.chromium()
                        .launch(
                                new BrowserType.LaunchOptions()
                                        .setHeadless(true)
                                        .setTimeout(15000)
                        );

        page =
                browser.newPage();

        generatedPage =
                new GeneratedPage(page);
    }

    @When("user performs generated actions")
    public void performActions() {

        System.out.println(
                "Executing generated flow..."
        );
    }

    @Then("flow should complete successfully")
    public void flowComplete() {

        System.out.println(
                "Generated flow completed."
        );

        browser.close();

        playwright.close();
    }
}
""";
    }
}
