package com.axiomai.qa.service;

import com.axiomai.qa.flow.DetectedFlow;
import com.axiomai.qa.models.GeneratedFramework;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FrameworkGeneratorService {

    // =====================================================
    // MAIN GENERATION
    // =====================================================

    public GeneratedFramework generate(
            List<DetectedFlow> flows
    ) {

        String feature =
                generateFeature(flows);

        String pageObject =
                generatePageObject(flows);

        String stepDefs =
                generateStepDefinitions(flows);

        return new GeneratedFramework(
                feature,
                pageObject,
                stepDefs
        );
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
                                        .setHeadless(false)
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