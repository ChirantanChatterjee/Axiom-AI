package com.axiomai.qa.flow;

import com.axiomai.qa.models.PageElement;

import java.util.ArrayList;
import java.util.List;

public class FlowDetectionEngine {

    // =====================================================
    // MAIN FLOW DETECTION
    // =====================================================

    public static List<DetectedFlow> detectFlows(

            String url,
            List<PageElement> elements

    ) {

        List<DetectedFlow> flows =
                new ArrayList<>();

        // =================================================
        // DETECT LOGIN FLOW
        // =================================================

        DetectedFlow loginFlow =
                detectLoginFlow(
                        url,
                        elements
                );

        if (loginFlow != null) {

            flows.add(loginFlow);
        }

        // =================================================
        // DETECT SEARCH FLOW
        // =================================================

        DetectedFlow searchFlow =
                detectSearchFlow(
                        url,
                        elements
                );

        if (searchFlow != null) {

            flows.add(searchFlow);
        }

        // =================================================
        // DETECT FORM FLOW
        // =================================================

        DetectedFlow formFlow =
                detectFormFlow(
                        url,
                        elements
                );

        if (formFlow != null) {

            flows.add(formFlow);
        }

        return flows;
    }

    // =====================================================
    // LOGIN FLOW
    // =====================================================

    private static DetectedFlow detectLoginFlow(

            String url,
            List<PageElement> elements

    ) {

        PageElement authField = null;

        PageElement passwordField = null;

        PageElement loginButton = null;

        for (PageElement element : elements) {

            String role =
                    safe(
                            element.getBusinessRole()
                    );

            if (role.equals("AUTH_FIELD")) {

                authField = element;
            }

            if (role.equals("PASSWORD_FIELD")) {

                passwordField = element;
            }

            if (role.equals("LOGIN_BUTTON")) {

                loginButton = element;
            }
        }

        if (

                authField != null
                        &&
                        passwordField != null
                        &&
                        loginButton != null

        ) {

            List<FlowStep> steps =
                    new ArrayList<>();

            // =============================================
            // USERNAME STEP
            // =============================================

            FlowStep usernameStep =
                    new FlowStep();

            usernameStep.setAction("TYPE");

            usernameStep.setTarget("USERNAME");

            usernameStep.setSelector(
                    authField.getBestSelector()
            );

            usernameStep.setFallbackSelectors(
                    authField.getFallbackSelectors()
            );

            usernameStep.setBusinessRole(
                    authField.getBusinessRole()
            );

            usernameStep.setConfidenceScore(
                    authField.getImportanceScore()
            );

            steps.add(usernameStep);

            // =============================================
            // PASSWORD STEP
            // =============================================

            FlowStep passwordStep =
                    new FlowStep();

            passwordStep.setAction("TYPE");

            passwordStep.setTarget("PASSWORD");

            passwordStep.setSelector(
                    passwordField.getBestSelector()
            );

            passwordStep.setFallbackSelectors(
                    passwordField.getFallbackSelectors()
            );

            passwordStep.setBusinessRole(
                    passwordField.getBusinessRole()
            );

            passwordStep.setConfidenceScore(
                    passwordField.getImportanceScore()
            );

            steps.add(passwordStep);

            // =============================================
            // LOGIN BUTTON STEP
            // =============================================

            FlowStep loginStep =
                    new FlowStep();

            loginStep.setAction("CLICK");

            loginStep.setTarget("LOGIN_BUTTON");

            loginStep.setSelector(
                    loginButton.getBestSelector()
            );

            loginStep.setFallbackSelectors(
                    loginButton.getFallbackSelectors()
            );

            loginStep.setBusinessRole(
                    loginButton.getBusinessRole()
            );

            loginStep.setConfidenceScore(
                    loginButton.getImportanceScore()
            );

            steps.add(loginStep);

            return new DetectedFlow(

                    FlowType.LOGIN,
                    url,
                    steps
            );
        }

        return null;
    }

    // =====================================================
    // SEARCH FLOW
    // =====================================================

    private static DetectedFlow detectSearchFlow(

            String url,
            List<PageElement> elements

    ) {

        PageElement searchField = null;

        PageElement searchButton = null;

        for (PageElement element : elements) {

            String role =
                    safe(
                            element.getBusinessRole()
                    );

            if (role.equals("SEARCH_FIELD")) {

                searchField = element;
            }

            if (role.equals("SEARCH_BUTTON")) {

                searchButton = element;
            }
        }

        if (

                searchField != null
                        &&
                        searchButton != null

        ) {

            List<FlowStep> steps =
                    new ArrayList<>();

            // =============================================
            // SEARCH INPUT STEP
            // =============================================

            FlowStep searchInputStep =
                    new FlowStep();

            searchInputStep.setAction("TYPE");

            searchInputStep.setTarget("SEARCH_TEXT");

            searchInputStep.setSelector(
                    searchField.getBestSelector()
            );

            searchInputStep.setFallbackSelectors(
                    searchField.getFallbackSelectors()
            );

            searchInputStep.setBusinessRole(
                    searchField.getBusinessRole()
            );

            searchInputStep.setConfidenceScore(
                    searchField.getImportanceScore()
            );

            steps.add(searchInputStep);

            // =============================================
            // SEARCH BUTTON STEP
            // =============================================

            FlowStep searchButtonStep =
                    new FlowStep();

            searchButtonStep.setAction("CLICK");

            searchButtonStep.setTarget("SEARCH_BUTTON");

            searchButtonStep.setSelector(
                    searchButton.getBestSelector()
            );

            searchButtonStep.setFallbackSelectors(
                    searchButton.getFallbackSelectors()
            );

            searchButtonStep.setBusinessRole(
                    searchButton.getBusinessRole()
            );

            searchButtonStep.setConfidenceScore(
                    searchButton.getImportanceScore()
            );

            steps.add(searchButtonStep);

            return new DetectedFlow(

                    FlowType.SEARCH,
                    url,
                    steps
            );
        }

        return null;
    }

    // =====================================================
    // FORM FLOW
    // =====================================================

    private static DetectedFlow detectFormFlow(

            String url,
            List<PageElement> elements

    ) {

        List<PageElement> textInputs =
                new ArrayList<>();

        PageElement submitButton = null;

        for (PageElement element : elements) {

            String role =
                    safe(
                            element.getBusinessRole()
                    );

            if (

                    role.equals("TEXT_INPUT")
                            ||

                            role.equals("AUTH_FIELD")
                            ||

                            role.equals("PASSWORD_FIELD")

            ) {

                textInputs.add(element);
            }

            if (

                    role.equals(
                            "PRIMARY_ACTION_BUTTON"
                    )

            ) {

                submitButton = element;
            }
        }

        if (

                textInputs.size() >= 2
                        &&
                        submitButton != null

        ) {

            List<FlowStep> steps =
                    new ArrayList<>();

            // =============================================
            // INPUT STEPS
            // =============================================

            for (PageElement input : textInputs) {

                FlowStep inputStep =
                        new FlowStep();

                inputStep.setAction("TYPE");

                inputStep.setTarget(
                        input.getBusinessRole()
                );

                inputStep.setSelector(
                        input.getBestSelector()
                );

                inputStep.setFallbackSelectors(
                        input.getFallbackSelectors()
                );

                inputStep.setBusinessRole(
                        input.getBusinessRole()
                );

                inputStep.setConfidenceScore(
                        input.getImportanceScore()
                );

                steps.add(inputStep);
            }

            // =============================================
            // SUBMIT STEP
            // =============================================

            FlowStep submitStep =
                    new FlowStep();

            submitStep.setAction("CLICK");

            submitStep.setTarget("SUBMIT_BUTTON");

            submitStep.setSelector(
                    submitButton.getBestSelector()
            );

            submitStep.setFallbackSelectors(
                    submitButton.getFallbackSelectors()
            );

            submitStep.setBusinessRole(
                    submitButton.getBusinessRole()
            );

            submitStep.setConfidenceScore(
                    submitButton.getImportanceScore()
            );

            steps.add(submitStep);

            return new DetectedFlow(

                    FlowType.FORM_SUBMISSION,
                    url,
                    steps
            );
        }

        return null;
    }

    // =====================================================
    // SAFE STRING
    // =====================================================

    private static String safe(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }
}