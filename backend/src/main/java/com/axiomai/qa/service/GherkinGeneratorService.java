package com.axiomai.qa.service;

import com.axiomai.qa.models.*;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GherkinGeneratorService {

    public List<GeneratedFeature> generateFeatures(
            List<DetectedFlow> flows
    ) {

        List<GeneratedFeature> features =
                new ArrayList<>();

        for (DetectedFlow flow : flows) {

            switch (flow.getFlowType()) {

                case SEARCH:

                    features.add(
                            generateSearchFeature(flow)
                    );

                    break;

                case LOGIN:

                    features.add(
                            generateLoginFeature(flow)
                    );

                    break;

                default:

                    break;
            }
        }

        return features;
    }

    // =====================================================
    // SEARCH FEATURE
    // =====================================================

    private GeneratedFeature generateSearchFeature(
            DetectedFlow flow
    ) {

        String gherkin =
                """
                Feature: Search functionality

                  Scenario: User performs search
                    Given user launches "%s"
                    When user enters "Playwright Java" into search field
                    And user clicks search button
                    Then search results should be displayed
                """.formatted(
                        flow.getPageUrl()
                );

        return new GeneratedFeature(
                "Search functionality",
                "User performs search",
                gherkin
        );
    }

    // =====================================================
    // LOGIN FEATURE
    // =====================================================

    private GeneratedFeature generateLoginFeature(
            DetectedFlow flow
    ) {

        String gherkin =
                """
                Feature: Login functionality

                  Scenario: Successful login
                    Given user launches "%s"
                    When user enters username
                    And user enters password
                    And user clicks login button
                    Then dashboard should be displayed
                """.formatted(
                        flow.getPageUrl()
                );

        return new GeneratedFeature(
                "Login functionality",
                "Successful login",
                gherkin
        );
    }
}