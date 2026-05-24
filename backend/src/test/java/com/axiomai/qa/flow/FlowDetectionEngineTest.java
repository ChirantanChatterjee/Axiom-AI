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
