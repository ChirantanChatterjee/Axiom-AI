package com.axiomai.qa.flow;

import com.axiomai.qa.models.FlowStep;
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

        System.out.println(
                "ELEMENT COUNT = "
                        + elements.size()
        );

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

        PageElement nextButton = null;

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

            if (role.equals("NEXT_BUTTON")) {

                nextButton = element;
            }
        }

        if (

                authField != null
                        &&
                        passwordField != null
                        &&
                        (
                                loginButton != null
                                        ||
                                        nextButton != null
                        )

        ) {

            List<FlowStep> steps =
                    new ArrayList<>();

            steps.add(
                    typeStep(
                            "USERNAME",
                            authField
                    )
            );

            steps.add(
                    typeStep(
                            "PASSWORD",
                            passwordField
                    )
            );

            steps.add(
                    clickStep(
                            "LOGIN_BUTTON",
                            loginButton != null
                                    ? loginButton
                                    : nextButton
                    )
            );

            return new DetectedFlow(

                    "LOGIN",
                    url,
                    steps
            );
        }

        if (

                authField != null
                        &&
                        nextButton != null

        ) {

            List<FlowStep> steps =
                    new ArrayList<>();

            steps.add(
                    typeStep(
                            "USERNAME",
                            authField
                    )
            );

            steps.add(
                    clickStep(
                            "NEXT_BUTTON",
                            nextButton
                    )
            );

            steps.add(
                    syntheticPasswordStep()
            );

            steps.add(
                    syntheticGoogleSubmitStep()
            );

            return new DetectedFlow(

                    "LOGIN",
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

            if (
                    role.equals("SEARCH_BUTTON")
                            &&
                            isBetterSearchButton(
                                    element,
                                    searchButton
                            )
            ) {

                searchButton = element;
            }
        }

        if (

                searchField != null

        ) {

            List<FlowStep> steps =
                    new ArrayList<>();

            steps.add(
                    typeStep(
                            "SEARCH_TEXT",
                            searchField
                    )
            );

            if (
                    searchButton != null
            ) {

                steps.add(
                        clickStep(
                                "SEARCH_BUTTON",
                                searchButton
                        )
                );
            }

            return new DetectedFlow(

                    "SEARCH",
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

                    "FORM_SUBMISSION",
                    url,
                    steps
            );
        }

        return null;
    }

    // =====================================================
    // STEP BUILDERS
    // =====================================================

    private static FlowStep typeStep(

            String target,
            PageElement element

    ) {

        FlowStep step =
                new FlowStep();

        step.setAction("TYPE");

        step.setTarget(target);

        applyElementMetadata(
                step,
                element
        );

        return step;
    }

    private static FlowStep clickStep(

            String target,
            PageElement element

    ) {

        FlowStep step =
                new FlowStep();

        step.setAction("CLICK");

        step.setTarget(target);

        applyElementMetadata(
                step,
                element
        );

        return step;
    }

    private static FlowStep syntheticPasswordStep() {

        FlowStep step =
                new FlowStep();

        step.setAction("TYPE");

        step.setTarget("PASSWORD");

        step.setSelector(
                "input[type='password']"
        );

        step.setFallbackSelectors(
                List.of(
                        "[name='Passwd']",
                        "#password",
                        "input[autocomplete='current-password']"
                )
        );

        step.setBusinessRole(
                "PASSWORD_FIELD"
        );

        step.setConfidenceScore(80);

        step.setSemanticDescription(
                "Password field on the second authentication step"
        );

        return step;
    }

    private static FlowStep syntheticGoogleSubmitStep() {

        FlowStep step =
                new FlowStep();

        step.setAction("CLICK");

        step.setTarget("LOGIN_BUTTON");

        step.setSelector(
                "#passwordNext"
        );

        step.setFallbackSelectors(
                List.of(
                        "button:has-text(\"Next\")",
                        "[role='button']:has-text(\"Next\")",
                        "button:has-text(\"Continue\")",
                        "[role='button']:has-text(\"Continue\")"
                )
        );

        step.setBusinessRole(
                "LOGIN_BUTTON"
        );

        step.setConfidenceScore(80);

        step.setSemanticDescription(
                "Submit the second authentication step"
        );

        return step;
    }

    private static void applyElementMetadata(

            FlowStep step,
            PageElement element

    ) {

        step.setSelector(
                element.getBestSelector()
        );

        step.setFallbackSelectors(
                element.getFallbackSelectors()
        );

        step.setBusinessRole(
                element.getBusinessRole()
        );

        step.setConfidenceScore(
                element.getImportanceScore()
        );
    }

    private static boolean isBetterSearchButton(

            PageElement candidate,

            PageElement current

    ) {

        return searchButtonScore(candidate)
                >
                searchButtonScore(current);
    }

    private static int searchButtonScore(
            PageElement element
    ) {

        if (
                element == null
        ) {

            return -1;
        }

        String label =
                (
                        safe(element.getText())
                                + " "
                                + safe(element.getAriaLabel())
                                + " "
                                + safe(element.getName())
                ).trim()
                        .toLowerCase();

        if (
                label.equals("search")
        ) {

            return 3;
        }

        if (
                label.contains("search")
                        &&
                        !label.contains("voice")
        ) {

            return 2;
        }

        if (
                label.contains("search")
        ) {

            return 1;
        }

        return 0;
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
