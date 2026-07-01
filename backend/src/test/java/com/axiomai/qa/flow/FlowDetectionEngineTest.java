package com.axiomai.qa.flow;

import com.axiomai.qa.models.FlowStep;
import com.axiomai.qa.models.PageElement;
import com.axiomai.qa.util.ElementClassifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowDetectionEngineTest {

    @Test
    void detectsYoutubeSearchControlsFromUppercaseTagsAndAriaLabels() {

        PageElement searchField =
                element(
                        "INPUT",
                        "",
                        "text",
                        "search_query",
                        "Search",
                        "",
                        "input[name='search_query']"
                );

        PageElement searchButton =
                element(
                        "BUTTON",
                        "",
                        "button",
                        "",
                        "",
                        "Search",
                        "button[aria-label='Search']"
                );

        PageElement voiceSearchButton =
                element(
                        "BUTTON",
                        "",
                        "button",
                        "",
                        "",
                        "Search with your voice",
                        "button[aria-label='Search with your voice']"
                );

        ElementClassifier.classify(searchField);
        ElementClassifier.classify(searchButton);
        ElementClassifier.classify(voiceSearchButton);

        List<DetectedFlow> flows =
                FlowDetectionEngine.detectFlows(
                        "https://youtube.com",
                        List.of(
                                searchField,
                                searchButton,
                                voiceSearchButton
                        )
                );

        DetectedFlow searchFlow =
                flows.stream()
                        .filter(flow -> "SEARCH".equals(flow.getFlowType()))
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                "SEARCH_FIELD",
                searchField.getBusinessRole()
        );

        assertEquals(
                "SEARCH_BUTTON",
                searchButton.getBusinessRole()
        );

        assertEquals(
                2,
                searchFlow.getSteps()
                        .size()
        );

        assertEquals(
                "button[aria-label='Search']",
                searchFlow.getSteps()
                        .get(1)
                        .getSelector()
        );
    }

    @Test
    void detectsGoogleMultiStepLoginWithoutMisclassifyingForgotEmail() {

        PageElement emailField =
                element(
                        "INPUT",
                        "",
                        "email",
                        "identifier",
                        "",
                        "",
                        "#identifierId"
                );

        PageElement forgotEmail =
                element(
                        "BUTTON",
                        "Forgot email?",
                        "button",
                        "",
                        "",
                        "",
                        "button:has-text(\"Forgot email?\")"
                );

        PageElement nextButton =
                element(
                        "BUTTON",
                        "Next",
                        "button",
                        "",
                        "",
                        "",
                        "button:has-text(\"Next\")"
                );

        ElementClassifier.classify(emailField);
        ElementClassifier.classify(forgotEmail);
        ElementClassifier.classify(nextButton);

        assertEquals(
                "AUTH_FIELD",
                emailField.getBusinessRole()
        );

        assertNotEquals(
                "AUTH_FIELD",
                forgotEmail.getBusinessRole()
        );

        assertEquals(
                "NEXT_BUTTON",
                nextButton.getBusinessRole()
        );

        DetectedFlow loginFlow =
                FlowDetectionEngine.detectFlows(
                                "https://accounts.google.com/ServiceLogin",
                                List.of(
                                        emailField,
                                        forgotEmail,
                                        nextButton
                                )
                        )
                        .stream()
                        .filter(flow -> "LOGIN".equals(flow.getFlowType()))
                        .findFirst()
                        .orElseThrow();

        List<String> targets =
                loginFlow.getSteps()
                        .stream()
                        .map(FlowStep::getTarget)
                        .toList();

        assertEquals(
                List.of(
                        "USERNAME",
                        "NEXT_BUTTON",
                        "PASSWORD",
                        "LOGIN_BUTTON"
                ),
                targets
        );

        assertEquals(
                "input[type='password']",
                loginFlow.getSteps()
                        .get(2)
                        .getSelector()
        );
    }

    @Test
    void detectsMicrosoftIdentityLoginAsTwoStepFlowAndSkipsGenericFormFlow() {

        PageElement emailField =
                element(
                        "INPUT",
                        "",
                        "email",
                        "loginfmt",
                        "Email, phone, or Skype",
                        "Enter your email, phone, or Skype.",
                        "input[aria-label='Enter your email, phone, or Skype.']"
                );

        PageElement passwordField =
                element(
                        "INPUT",
                        "",
                        "password",
                        "passwd",
                        "",
                        "",
                        "#i0118"
                );

        PageElement nextButton =
                element(
                        "INPUT",
                        "Next",
                        "submit",
                        "",
                        "",
                        "",
                        "#idSIButton9"
                );

        ElementClassifier.classify(emailField);
        ElementClassifier.classify(passwordField);
        ElementClassifier.classify(nextButton);

        List<DetectedFlow> flows =
                FlowDetectionEngine.detectFlows(
                        "https://joegill.crm4.dynamics.com",
                        List.of(
                                emailField,
                                passwordField,
                                nextButton
                        )
                );

        DetectedFlow loginFlow =
                flows.stream()
                        .filter(flow -> "LOGIN".equals(flow.getFlowType()))
                        .findFirst()
                        .orElseThrow();

        List<String> targets =
                loginFlow.getSteps()
                        .stream()
                        .map(FlowStep::getTarget)
                        .toList();

        assertEquals(
                List.of(
                        "USERNAME",
                        "NEXT_BUTTON",
                        "PASSWORD",
                        "LOGIN_BUTTON"
                ),
                targets
        );

        assertEquals(
                "#idSIButton9",
                loginFlow.getSteps()
                        .get(3)
                        .getSelector()
        );

        assertTrue(
                flows.stream()
                        .noneMatch(flow -> "FORM_SUBMISSION".equals(flow.getFlowType()))
        );
    }

    @Test
    void detectsMicrosoftPasswordPageAsLoginFlowAndSkipsGenericFormFlow() {

        PageElement backButton =
                element(
                        "BUTTON",
                        "Back",
                        "button",
                        "",
                        "",
                        "Back",
                        "button[aria-label='Back']"
                );

        PageElement usernameField =
                element(
                        "INPUT",
                        "",
                        "text",
                        "loginfmt",
                        "",
                        "",
                        "input[name='loginfmt']"
                );

        PageElement passwordField =
                element(
                        "INPUT",
                        "",
                        "password",
                        "passwd",
                        "Password",
                        "Enter the password for user@example.com",
                        "input[aria-label='Enter the password for user@example.com']"
                );

        PageElement signInButton =
                element(
                        "INPUT",
                        "Sign in",
                        "submit",
                        "",
                        "",
                        "",
                        "#idSIButton9"
                );

        ElementClassifier.classify(backButton);
        ElementClassifier.classify(usernameField);
        ElementClassifier.classify(passwordField);
        ElementClassifier.classify(signInButton);

        assertEquals(
                "AUTH_FIELD",
                usernameField.getBusinessRole()
        );

        List<DetectedFlow> flows =
                FlowDetectionEngine.detectFlows(
                        "https://login.microsoftonline.com",
                        List.of(
                                backButton,
                                usernameField,
                                passwordField,
                                signInButton
                        )
                );

        DetectedFlow loginFlow =
                flows.stream()
                        .filter(flow -> "LOGIN".equals(flow.getFlowType()))
                        .findFirst()
                        .orElseThrow();

        List<String> targets =
                loginFlow.getSteps()
                        .stream()
                        .map(FlowStep::getTarget)
                        .toList();

        assertEquals(
                List.of(
                        "USERNAME",
                        "PASSWORD",
                        "LOGIN_BUTTON"
                ),
                targets
        );

        assertTrue(
                flows.stream()
                        .noneMatch(flow -> "FORM_SUBMISSION".equals(flow.getFlowType()))
        );
    }

    @Test
    void submitInputWithContinueIsFormActionNotLoginButton() {

        PageElement origin =
                element(
                        "SELECT",
                        "Origin",
                        "",
                        "fromPort",
                        "",
                        "",
                        "select[name='fromPort']"
                );

        PageElement destination =
                element(
                        "SELECT",
                        "Destination",
                        "",
                        "toPort",
                        "",
                        "",
                        "select[name='toPort']"
                );

        PageElement continueButton =
                element(
                        "INPUT",
                        "Continue",
                        "submit",
                        "",
                        "",
                        "",
                        "input[type='submit'][value='Continue']"
                );

        ElementClassifier.classify(origin);
        ElementClassifier.classify(destination);
        ElementClassifier.classify(continueButton);

        assertEquals(
                "NEXT_BUTTON",
                continueButton.getBusinessRole()
        );

        List<DetectedFlow> flows =
                FlowDetectionEngine.detectFlows(
                        "https://travel.agileway.net/flights",
                        List.of(
                                origin,
                                destination,
                                continueButton
                        )
                );

        DetectedFlow formFlow =
                flows.stream()
                        .filter(flow -> "FORM_SUBMISSION".equals(flow.getFlowType()))
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                "Continue",
                formFlow.getSteps()
                        .get(2)
                        .getTarget()
        );
    }

    @Test
    void detectsSauceDemoInventoryFlowsBeyondLogin() {

        PageElement sort =
                element(
                        "SELECT",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "[data-test='product_sort_container']"
                );
        sort.setDataTestId("product_sort_container");

        PageElement addToCart =
                element(
                        "BUTTON",
                        "Add to cart",
                        "button",
                        "",
                        "",
                        "",
                        "[data-test='add-to-cart-sauce-labs-backpack']"
                );
        addToCart.setDataTestId("add-to-cart-sauce-labs-backpack");

        PageElement cart =
                element(
                        "A",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "[data-test='shopping-cart-link']"
                );
        cart.setDataTestId("shopping-cart-link");

        ElementClassifier.classify(sort);
        ElementClassifier.classify(addToCart);
        ElementClassifier.classify(cart);

        List<DetectedFlow> flows =
                FlowDetectionEngine.detectFlows(
                        "https://www.saucedemo.com/inventory.html",
                        List.of(
                                sort,
                                addToCart,
                                cart
                        )
                );

        assertTrue(
                flows.stream()
                        .anyMatch(flow -> "PRODUCT_SORT".equals(flow.getFlowType()))
        );

        DetectedFlow addToCartFlow =
                flows.stream()
                        .filter(flow -> "ADD_TO_CART".equals(flow.getFlowType()))
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                "add Sauce Labs Backpack to cart",
                addToCartFlow.getSteps()
                        .get(0)
                        .getTarget()
        );

        assertEquals(
                "cart",
                addToCartFlow.getSteps()
                        .get(1)
                        .getTarget()
        );

        assertTrue(
                flows.stream()
                        .anyMatch(flow -> "CART_NAVIGATION".equals(flow.getFlowType()))
        );
    }

    private PageElement element(

            String tag,
            String text,
            String type,
            String name,
            String placeholder,
            String ariaLabel,
            String selector

    ) {

        PageElement element =
                new PageElement();

        element.setTag(tag);
        element.setText(text);
        element.setType(type);
        element.setName(name);
        element.setPlaceholder(placeholder);
        element.setAriaLabel(ariaLabel);
        element.setVisible(true);
        element.setCssSelector(selector);

        return element;
    }
}
