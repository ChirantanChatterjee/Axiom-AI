package com.axiomai.qa.ai;

import com.axiomai.ai.service.OpenAIService;
import com.axiomai.qa.models.FlowStep;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LLMElementLocator {

    @Autowired
    private OpenAIService openAIService;

    // =====================================================
    // FIND ELEMENT USING AI SEMANTICS
    // =====================================================

    public Locator locate(

            Page page,
            FlowStep step

    ) {

        try {

            String html =
                    page.content();

            String prompt = buildPrompt(
                    html,
                    step
            );

            String selector =
                    openAIService.ask(prompt);

            System.out.println(
                    "[LLM GENERATED SELECTOR] "
                            + selector
            );

            return page.locator(selector).first();

        } catch (Exception e) {

            throw new RuntimeException(

                    "LLM element location failed for: "
                            + step.getTarget(),

                    e
            );
        }
    }

    // =====================================================
    // PROMPT BUILDER
    // =====================================================

    private String buildPrompt(

            String html,
            FlowStep step

    ) {

        return """

You are an AI UI automation engine.

Your job:
Find the BEST Playwright selector.

Target element:
%s

Semantic meaning:
%s

HTML:
%s

Rules:
- Return ONLY selector
- Prefer:
    [data-testid]
    id
    aria-label
    placeholder
    button text
- Avoid long xpath
- Output must work directly in:
    page.locator(selector)

"""
                .formatted(

                        step.getTarget(),
                        step.getSemanticDescription(),
                        html
                );
    }
}