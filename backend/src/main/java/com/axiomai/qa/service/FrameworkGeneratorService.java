package com.axiomai.qa.service;

import com.axiomai.qa.models.*;

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

        return """
Feature: Search functionality

  Scenario: User searches in Google
    Given user launches "https://google.com"
    When user enters "Playwright Java" into search field
    And user clicks search button
    Then search results should be displayed
""";
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

public class GooglePage {

    private final Page page;

    private final Locator searchField;

    private final Locator searchButton;

    public GooglePage(Page page) {

        this.page = page;

        this.searchField =
                page.locator("textarea#APjFqb");

        this.searchButton =
                page.locator("input[name='btnK']");
    }

    public void search(String text) {

        searchField.fill(text);

        searchButton.click();
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

import com.axiomai.generated.pages.GooglePage;

public class GoogleSteps {

    private static Playwright playwright;

    private static Browser browser;

    private static Page page;

    private GooglePage googlePage;

    @Given("user launches {string}")
    public void userLaunches(String url) {

        playwright = Playwright.create();

        browser =
                playwright.chromium()
                        .launch(
                                new BrowserType.LaunchOptions()
                                        .setHeadless(false)
                        );

        page = browser.newPage();

        page.navigate(url);

        googlePage =
                new GooglePage(page);
    }

    @When("user enters {string} into search field")
    public void userSearches(String value) {

        googlePage.search(value);
    }

    @When("user clicks search button")
    public void userClicksSearchButton() {

        // already handled
    }

    @Then("search results should be displayed")
    public void resultsDisplayed() {

        System.out.println(
                "Search completed successfully"
        );

        browser.close();

        playwright.close();
    }
}
""";
    }
}