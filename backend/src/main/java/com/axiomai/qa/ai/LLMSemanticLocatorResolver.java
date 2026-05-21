package com.axiomai.qa.ai;

import com.axiomai.ai.service.OpenAIService;
import com.axiomai.qa.models.PageElement;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component

public class LLMSemanticLocatorResolver {

    @Autowired
    private OpenAIService openAIService;

    @Autowired
    private LocalSemanticResolver
            localSemanticResolver;

    private final ObjectMapper mapper =
            new ObjectMapper();

    // =====================================================
    // MAIN RESOLUTION
    // =====================================================

    public ElementSemanticMatch resolve(

            String userIntent,
            List<PageElement> elements

    ) {

        // =================================================
        // LOCAL RESOLUTION FIRST
        // =================================================

        ElementSemanticMatch local =
                localSemanticResolver.resolve(

                        userIntent,
                        elements

                );

        if (

                local != null
                        &&
                        local.getConfidence() >= 0.70

        ) {

            System.out.println(
                    "[SEMANTIC ENGINE] USING LOCAL RESOLUTION"
            );

            return local;
        }

        // =================================================
        // FALLBACK TO OPENAI
        // =================================================

        try {

            String elementJson =
                    mapper.writeValueAsString(elements);

            String prompt = """

You are an AI QA automation engine.

Your task is to identify the BEST matching UI element
based on the user intent.

USER INTENT:
%s

AVAILABLE ELEMENTS:
%s

RULES:
1. Return ONLY valid JSON
2. Pick the BEST matching element
3. Confidence must be between 0 and 1
4. Do NOT explain outside JSON

RESPONSE FORMAT:

{
  "selector": "...",
  "reasoning": "...",
  "confidence": 0.95
}

"""
                    .formatted(
                            userIntent,
                            elementJson
                    );

            String response =
                    openAIService.ask(prompt);

            // =============================================
            // SAFETY
            // =============================================

            if (

                    response == null
                            ||
                            response.isBlank()

            ) {

                System.out.println(
                        "[SEMANTIC ENGINE] Empty OpenAI response."
                );

                return local;
            }

            if (

                    !response.trim()
                            .startsWith("{")

            ) {

                System.out.println(
                        "[SEMANTIC ENGINE] Invalid JSON response."
                );

                return local;
            }

            ElementSemanticMatch llm =
                    mapper.readValue(

                            response,

                            ElementSemanticMatch.class

                    );

            if (llm != null) {

                llm.setSource("OPENAI");

                llm.setSemanticTarget(userIntent);

                System.out.println(
                        "[SEMANTIC ENGINE] USING OPENAI RESOLUTION"
                );

                return llm;
            }

            return local;

        } catch (Exception e) {

            System.out.println(
                    "[SEMANTIC ENGINE] OpenAI resolution failed."
            );

            return local;
        }
    }
}