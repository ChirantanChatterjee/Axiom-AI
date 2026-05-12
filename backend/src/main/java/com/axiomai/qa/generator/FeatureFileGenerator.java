package com.axiomai.qa.generator;

import com.axiomai.qa.models.PageElement;
import com.axiomai.qa.models.PageScanResult;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Component
public class FeatureFileGenerator {

    // =====================================================
    // MAIN GENERATOR
    // =====================================================

    public String generate(PageScanResult scanResult) {

        StringBuilder feature =
                new StringBuilder();

        String title =
                safe(scanResult.getTitle());

        String featureName =
                title.isBlank()
                        ? "Generated Feature"
                        : title;

        feature.append("Feature: ")
                .append(featureName)
                .append("\n\n");

        List<PageElement> elements =
                scanResult.getElements();

        // =================================================
        // SEARCH SCENARIO
        // =================================================

        PageElement searchField =
                findByRole(
                        elements,
                        "SEARCH_FIELD"
                );

        PageElement searchButton =
                findByRole(
                        elements,
                        "SEARCH_BUTTON"
                );

        if (searchField != null) {

            feature.append(generateSearchScenario(
                    scanResult,
                    searchField,
                    searchButton
            ));
        }

        // =================================================
        // LOGIN SCENARIO
        // =================================================

        PageElement authField =
                findByRole(
                        elements,
                        "AUTH_FIELD"
                );

        PageElement passwordField =
                findByRole(
                        elements,
                        "PASSWORD_FIELD"
                );

        PageElement loginButton =
                findByRole(
                        elements,
                        "LOGIN_BUTTON"
                );

        if (
                authField != null
                        &&
                        passwordField != null
                        &&
                        loginButton != null
        ) {

            feature.append(generateLoginScenario(
                    scanResult
            ));
        }

        // =================================================
        // GENERIC BUTTON SCENARIOS
        // =================================================

        List<PageElement> primaryButtons =
                findAllByRole(
                        elements,
                        "PRIMARY_ACTION_BUTTON"
                );

        for (PageElement button : primaryButtons) {

            feature.append(
                    generateButtonScenario(
                            scanResult,
                            button
                    )
            );
        }

        // =================================================
        // FALLBACK
        // =================================================

        if (feature.toString().trim().equals(
                "Feature: " + featureName
        )) {

            feature.append(generateFallbackScenario(
                    scanResult
            ));
        }

        return feature.toString();
    }

    // =====================================================
    // SEARCH SCENARIO
    // =====================================================

    private String generateSearchScenario(
            PageScanResult scanResult,
            PageElement searchField,
            PageElement searchButton
    ) {

        StringBuilder sb =
                new StringBuilder();

        sb.append("Scenario: Search using application\n");

        sb.append("    Given user launches ")
                .append(scanResult.getUrl())
                .append("\n");

        sb.append("    When user enters \"Playwright Java\" into search field\n");

        if (searchButton != null) {

            sb.append("    And user clicks search button\n");
        }

        sb.append("    Then search results should be displayed\n\n");

        return sb.toString();
    }

    // =====================================================
    // LOGIN SCENARIO
    // =====================================================

    private String generateLoginScenario(
            PageScanResult scanResult
    ) {

        StringBuilder sb =
                new StringBuilder();

        sb.append("Scenario: Login to application\n");

        sb.append("    Given user launches ")
                .append(scanResult.getUrl())
                .append("\n");

        sb.append("    When user enters username\n");

        sb.append("    And user enters password\n");

        sb.append("    And user clicks login button\n");

        sb.append("    Then user should login successfully\n\n");

        return sb.toString();
    }

    // =====================================================
    // BUTTON SCENARIO
    // =====================================================

    private String generateButtonScenario(
            PageScanResult scanResult,
            PageElement button
    ) {

        String buttonText =
                safe(button.getText());

        if (buttonText.isBlank()) {

            buttonText =
                    safe(button.getId());
        }

        if (buttonText.isBlank()) {

            buttonText = "button";
        }

        StringBuilder sb =
                new StringBuilder();

        sb.append("Scenario: Validate ")
                .append(buttonText)
                .append(" button\n");

        sb.append("    Given user launches ")
                .append(scanResult.getUrl())
                .append("\n");

        sb.append("    When user clicks ")
                .append(buttonText)
                .append(" button\n");

        sb.append("    Then action should complete successfully\n\n");

        return sb.toString();
    }

    // =====================================================
    // FALLBACK SCENARIO
    // =====================================================

    private String generateFallbackScenario(
            PageScanResult scanResult
    ) {

        return "Scenario: Open application\n"
                + "    Given user launches "
                + scanResult.getUrl()
                + "\n"
                + "    Then application should load successfully\n\n";
    }

    // =====================================================
    // FIND ROLE
    // =====================================================

    private PageElement findByRole(
            List<PageElement> elements,
            String role
    ) {

        for (PageElement element : elements) {

            if (
                    role.equalsIgnoreCase(
                            element.getBusinessRole()
                    )
            ) {

                return element;
            }
        }

        return null;
    }

    // =====================================================
    // FIND ALL ROLES
    // =====================================================

    private List<PageElement> findAllByRole(
            List<PageElement> elements,
            String role
    ) {

        List<PageElement> result =
                new ArrayList<>();

        for (PageElement element : elements) {

            if (
                    role.equalsIgnoreCase(
                            element.getBusinessRole()
                    )
            ) {

                result.add(element);
            }
        }

        return result;
    }

    // =====================================================
    // SAFE STRING
    // =====================================================

    private String safe(String value) {

        return value == null
                ? ""
                : value;
    }
}

