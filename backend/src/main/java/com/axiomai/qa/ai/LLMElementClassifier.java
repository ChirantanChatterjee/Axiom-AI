package com.axiomai.qa.ai;

import com.axiomai.ai.service.OpenAIService;
import com.axiomai.qa.models.PageElement;
import com.axiomai.security.SensitiveLogSanitizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LLMElementClassifier {

    @Autowired
    private OpenAIService openAIService;

    private final ObjectMapper mapper =
            new ObjectMapper();

    // =====================================================
    // CLASSIFY ELEMENT
    // =====================================================

    public LLMClassificationResponse classify(

            PageElement element

    ) {

        try {

            String prompt =
                    buildPrompt(element);

            String response =
                    openAIService.ask(prompt);

            return mapper.readValue(
                    response,
                    LLMClassificationResponse.class
            );

        } catch (Exception e) {

            System.out.println(
                    "[LLM ELEMENT CLASSIFIER FAILED] "
                            + SensitiveLogSanitizer.redact(
                            e.getMessage()
                    )
            );

            LLMClassificationResponse fallback =
                    new LLMClassificationResponse();

            fallback.setBusinessRole(
                    "UNKNOWN"
            );

            fallback.setConfidence(0);

            return fallback;
        }
    }

    // =====================================================
    // PROMPT
    // =====================================================

    private String buildPrompt(
            PageElement element
    ) {

        return """
                Classify this UI element.

                Return ONLY valid JSON.

                Example:
                {
                  "businessRole": "SEARCH_BUTTON",
                  "confidence": 0.95
                }

                Possible business roles:
                - SEARCH_BUTTON
                - SEARCH_FIELD
                - LOGIN_BUTTON
                - AUTH_FIELD
                - PASSWORD_FIELD
                - CHECKOUT_BUTTON
                - PRIMARY_ACTION_BUTTON
                - COOKIE_BUTTON
                - NAVIGATION_LINK
                - UNKNOWN

                UI Element:

                tag: %s
                text: %s
                aria-label: %s
                placeholder: %s
                type: %s
                """
                .formatted(
                        safe(element.getTag()),
                        safe(element.getText()),
                        safe(element.getAriaLabel()),
                        safe(element.getPlaceholder()),
                        safe(element.getType())
                );
    }

    // =====================================================
    // SAFE
    // =====================================================

    private String safe(String value) {

        return value == null
                ? ""
                : value;
    }
}
