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
        // DETECT COMMERCE / WORKFLOW FLOWS
        // =================================================

        DetectedFlow sortFlow =
                detectProductSortFlow(
                        url,
                        elements
                );

        if (sortFlow != null) {

            flows.add(sortFlow);
        }

        DetectedFlow addToCartFlow =
                detectAddToCartFlow(
                        url,
                        elements
                );

        if (addToCartFlow != null) {

            flows.add(addToCartFlow);
        }

        DetectedFlow removeFromCartFlow =
                detectRemoveFromCartFlow(
                        url,
                        elements
                );

        if (removeFromCartFlow != null) {

            flows.add(removeFromCartFlow);
        }

        DetectedFlow cartFlow =
                detectCartNavigationFlow(
                        url,
                        elements
                );

        if (cartFlow != null) {

            flows.add(cartFlow);
        }

        DetectedFlow checkoutFlow =
                detectCheckoutFlow(
                        url,
                        elements
                );

        if (checkoutFlow != null) {

            flows.add(checkoutFlow);
        }

        // =================================================
        // DETECT FORM FLOW
        // =================================================

        DetectedFlow formFlow =
                loginFlow == null
                        ? detectFormFlow(
                        url,
                        elements
                )
                        : null;

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
                        nextButton != null
                        &&
                        loginButton == null
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
                    typeStep(
                            "PASSWORD",
                            passwordField
                    )
            );

            steps.add(
                    clickStep(
                            "LOGIN_BUTTON",
                            nextButton
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
                        passwordField != null
                        &&
                        loginButton != null

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
                            loginButton
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
                    syntheticSecondStepSubmitStep(
                            nextButton
                    )
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
    // PRODUCT SORT FLOW
    // =====================================================

    private static DetectedFlow detectProductSortFlow(

            String url,
            List<PageElement> elements

    ) {

        PageElement sortControl =
                firstByRole(
                        elements,
                        "SORT_SELECT"
                );

        if (
                sortControl == null
        ) {

            return null;
        }

        List<FlowStep> steps =
                new ArrayList<>();

        steps.add(
                typeStep(
                        actionTarget(
                                sortControl,
                                "SORT"
                        ),
                        sortControl
                )
        );

        return new DetectedFlow(

                "PRODUCT_SORT",
                url,
                steps
        );
    }

    // =====================================================
    // ADD TO CART FLOW
    // =====================================================

    private static DetectedFlow detectAddToCartFlow(

            String url,
            List<PageElement> elements

    ) {

        PageElement addButton =
                firstByRole(
                        elements,
                        "ADD_TO_CART_BUTTON"
                );

        if (
                addButton == null
        ) {

            return null;
        }

        List<FlowStep> steps =
                new ArrayList<>();

        steps.add(
                clickStep(
                        actionTarget(
                                addButton,
                                "ADD_TO_CART_BUTTON"
                        ),
                        addButton
                )
        );

        PageElement cartLink =
                firstByRole(
                        elements,
                        "CART_LINK"
                );

        if (
                cartLink != null
        ) {

            steps.add(
                    clickStep(
                            actionTarget(
                                    cartLink,
                                    "CART_LINK"
                            ),
                            cartLink
                    )
            );
        }

        return new DetectedFlow(

                "ADD_TO_CART",
                url,
                steps
        );
    }

    // =====================================================
    // REMOVE FROM CART FLOW
    // =====================================================

    private static DetectedFlow detectRemoveFromCartFlow(

            String url,
            List<PageElement> elements

    ) {

        PageElement removeButton =
                firstByRole(
                        elements,
                        "REMOVE_FROM_CART_BUTTON"
                );

        if (
                removeButton == null
        ) {

            return null;
        }

        List<FlowStep> steps =
                new ArrayList<>();

        steps.add(
                clickStep(
                        actionTarget(
                                removeButton,
                                "REMOVE_FROM_CART_BUTTON"
                        ),
                        removeButton
                )
        );

        return new DetectedFlow(

                "REMOVE_FROM_CART",
                url,
                steps
        );
    }

    // =====================================================
    // CART NAVIGATION FLOW
    // =====================================================

    private static DetectedFlow detectCartNavigationFlow(

            String url,
            List<PageElement> elements

    ) {

        PageElement cartLink =
                firstByRole(
                        elements,
                        "CART_LINK"
                );

        if (
                cartLink == null
        ) {

            return null;
        }

        List<FlowStep> steps =
                new ArrayList<>();

        steps.add(
                clickStep(
                        actionTarget(
                                cartLink,
                                "CART_LINK"
                        ),
                        cartLink
                )
        );

        return new DetectedFlow(

                "CART_NAVIGATION",
                url,
                steps
        );
    }

    // =====================================================
    // CHECKOUT FLOW
    // =====================================================

    private static DetectedFlow detectCheckoutFlow(

            String url,
            List<PageElement> elements

    ) {

        PageElement checkoutButton =
                firstByRole(
                        elements,
                        "CHECKOUT_BUTTON"
                );

        PageElement continueButton =
                firstByRole(
                        elements,
                        "NEXT_BUTTON"
                );

        List<PageElement> checkoutFields =
                elements.stream()
                        .filter(element ->
                                hasRole(
                                        element,
                                        "FIRST_NAME_FIELD"
                                )
                                        ||
                                        hasRole(
                                                element,
                                                "LAST_NAME_FIELD"
                                        )
                                        ||
                                        hasRole(
                                                element,
                                                "POSTAL_CODE_FIELD"
                                        )
                        )
                        .toList();

        if (
                checkoutButton == null
                        &&
                        checkoutFields.isEmpty()
        ) {

            return null;
        }

        List<FlowStep> steps =
                new ArrayList<>();

        if (
                checkoutButton != null
        ) {

            steps.add(
                    clickStep(
                            actionTarget(
                                    checkoutButton,
                                    "CHECKOUT_BUTTON"
                            ),
                            checkoutButton
                    )
            );
        }

        for (
                PageElement field
                : checkoutFields
        ) {

            steps.add(
                    typeStep(
                            actionTarget(
                                    field,
                                    field.getBusinessRole()
                            ),
                            field
                    )
            );
        }

        if (
                continueButton != null
                        &&
                        !checkoutFields.isEmpty()
        ) {

            steps.add(
                    clickStep(
                            actionTarget(
                                    continueButton,
                                    "CONTINUE"
                            ),
                            continueButton
                    )
            );
        }

        return new DetectedFlow(

                checkoutFields.isEmpty()
                        ? "CHECKOUT_START"
                        : "CHECKOUT_INFORMATION",
                url,
                steps
        );
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
                    isFormInputRole(role)
            ) {

                textInputs.add(element);
            }

            if (

                    role.equals(
                            "PRIMARY_ACTION_BUTTON"
                    )
                            ||
                            role.equals(
                                    "NEXT_BUTTON"
                            )
                            ||
                            role.equals(
                                    "SEARCH_BUTTON"
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

            submitStep.setTarget(
                    actionTarget(
                            submitButton,
                            "SUBMIT_BUTTON"
                    )
            );

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

    private static FlowStep syntheticSecondStepSubmitStep(
            PageElement nextButton
    ) {

        FlowStep step =
                new FlowStep();

        step.setAction("CLICK");

        step.setTarget("LOGIN_BUTTON");

        String nextSelector =
                nextButton == null
                        ? ""
                        : safe(nextButton.getBestSelector());

        String primarySelector =
                nextSelector.toLowerCase()
                        .contains("idsibutton9")
                        ? "#idSIButton9"
                        : "#passwordNext";

        step.setSelector(
                primarySelector
        );

        List<String> fallbacks =
                new ArrayList<>();

        if (
                !nextSelector.isBlank()
                        &&
                        !nextSelector.equals(primarySelector)
        ) {

            fallbacks.add(nextSelector);
        }

        fallbacks.add("#idSIButton9");
        fallbacks.add("#passwordNext");
        fallbacks.add("input[type='submit'][value*='Sign in' i]");
        fallbacks.add("input[type='submit'][value*='Next' i]");
        fallbacks.add("button:has-text(\"Sign in\")");
        fallbacks.add("[role='button']:has-text(\"Sign in\")");
        fallbacks.add("button:has-text(\"Next\")");
        fallbacks.add("[role='button']:has-text(\"Next\")");
        fallbacks.add("button:has-text(\"Continue\")");
        fallbacks.add("[role='button']:has-text(\"Continue\")");

        step.setFallbackSelectors(fallbacks);

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

    private static String actionTarget(
            PageElement element,
            String fallback
    ) {

        String role =
                safe(element.getBusinessRole());

        if (
                role.equals("ADD_TO_CART_BUTTON")
        ) {

            return productActionLabel(
                    "add",
                    "to cart",
                    element,
                    "add to cart"
            );
        }

        if (
                role.equals("REMOVE_FROM_CART_BUTTON")
        ) {

            return productActionLabel(
                    "remove",
                    "",
                    element,
                    "remove"
            );
        }

        if (
                role.equals("CART_LINK")
        ) {

            return "cart";
        }

        if (
                role.equals("CHECKOUT_BUTTON")
        ) {

            return "checkout";
        }

        if (
                role.equals("CONTINUE_SHOPPING_BUTTON")
        ) {

            return "continue shopping";
        }

        if (
                role.equals("FINISH_BUTTON")
        ) {

            return "finish";
        }

        if (
                role.equals("CANCEL_BUTTON")
        ) {

            return "cancel";
        }

        if (
                role.equals("SORT_SELECT")
        ) {

            return "sort";
        }

        if (
                role.equals("FIRST_NAME_FIELD")
        ) {

            return "first name";
        }

        if (
                role.equals("LAST_NAME_FIELD")
        ) {

            return "last name";
        }

        if (
                role.equals("POSTAL_CODE_FIELD")
        ) {

            return "postal code";
        }

        String label =
                firstNonBlank(
                        element.getText(),
                        element.getAriaLabel(),
                        element.getName(),
                        element.getDataTestId()
                );

        if (
                !isGenericActionLabel(label)
        ) {

            return label.trim();
        }

        String selector =
                safe(element.getBestSelector())
                        .toLowerCase();

        if (
                selector.contains("continue")
                        ||
                        selector.contains("next")
        ) {

            return "Continue";
        }

        if (
                selector.contains("search")
        ) {

            return "Search";
        }

        if (
                role.equals("NEXT_BUTTON")
        ) {

            return "Continue";
        }

        if (
                role.equals("SEARCH_BUTTON")
        ) {

            return "Search";
        }

        return fallback;
    }

    private static PageElement firstByRole(
            List<PageElement> elements,
            String role
    ) {

        if (
                elements == null
                        ||
                        role == null
        ) {

            return null;
        }

        return elements.stream()
                .filter(element -> hasRole(element, role))
                .findFirst()
                .orElse(null);
    }

    private static boolean hasRole(
            PageElement element,
            String role
    ) {

        return element != null
                &&
                safe(element.getBusinessRole())
                        .equals(role);
    }

    private static boolean isFormInputRole(
            String role
    ) {

        return role.equals("TEXT_INPUT")
                ||
                role.equals("AUTH_FIELD")
                ||
                role.equals("PASSWORD_FIELD")
                ||
                role.endsWith("_FIELD");
    }

    private static String productActionLabel(
            String prefix,
            String suffix,
            PageElement element,
            String fallback
    ) {

        String product =
                productNameFromElement(element);

        if (
                product.isBlank()
        ) {

            return fallback;
        }

        StringBuilder label =
                new StringBuilder(prefix)
                        .append(" ")
                        .append(product);

        if (
                suffix != null
                        &&
                        !suffix.isBlank()
        ) {

            label.append(" ")
                    .append(suffix);
        }

        return label.toString();
    }

    private static String productNameFromElement(
            PageElement element
    ) {

        String raw =
                firstNonBlank(
                        element.getDataTestId(),
                        element.getName(),
                        element.getText(),
                        element.getAriaLabel()
                );

        String normalized =
                safe(raw)
                        .toLowerCase()
                        .replace("add-to-cart-", "")
                        .replace("remove-", "")
                        .replace("add to cart", "")
                        .replace("remove", "")
                        .replaceAll("[^a-z0-9]+", " ")
                        .trim();

        if (
                normalized.isBlank()
                        ||
                        normalized.equals("cart")
        ) {

            return "";
        }

        StringBuilder title =
                new StringBuilder();

        for (
                String part
                : normalized.split("\\s+")
        ) {

            if (
                    part.isBlank()
            ) {

                continue;
            }

            if (
                    title.length() > 0
            ) {

                title.append(" ");
            }

            title.append(
                    Character.toUpperCase(
                            part.charAt(0)
                    )
            );

            if (
                    part.length() > 1
            ) {

                title.append(
                        part.substring(1)
                );
            }
        }

        return title.toString();
    }

    private static String firstNonBlank(
            String... values
    ) {

        if (
                values == null
        ) {

            return "";
        }

        for (String value : values) {

            if (
                    value != null
                            &&
                            !value.isBlank()
            ) {

                return value;
            }
        }

        return "";
    }

    private static boolean isGenericActionLabel(
            String label
    ) {

        String normalized =
                safe(label)
                        .trim()
                        .toLowerCase()
                        .replaceAll("[^a-z0-9]+", " ")
                        .trim();

        return normalized.isBlank()
                ||
                normalized.equals("submit")
                ||
                normalized.equals("commit")
                ||
                normalized.equals("button")
                ||
                normalized.equals("primary action button")
                ||
                normalized.equals("submit button");
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
