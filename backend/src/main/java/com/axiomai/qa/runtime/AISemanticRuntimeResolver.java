package com.axiomai.qa.runtime;

import com.axiomai.qa.ai.ElementSemanticMatch;
import com.axiomai.qa.ai.LLMSemanticLocatorResolver;
import com.axiomai.qa.models.FlowStep;
import com.axiomai.qa.models.PageElement;
import com.axiomai.security.SensitiveLogSanitizer;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AISemanticRuntimeResolver {

    @Autowired
    private LLMSemanticLocatorResolver semanticResolver;

    // =====================================================
    // RESOLVE USING AI
    // =====================================================

    public Locator resolve(

            Page page,
            FlowStep step,
            List<PageElement> elements

    ) {

        try {

            String semanticIntent =
                    step.getSemanticDescription();

            if (

                    semanticIntent == null
                            ||
                            semanticIntent.isBlank()

            ) {

                semanticIntent =
                        step.getTarget();
            }

            ElementSemanticMatch match =
                    semanticResolver.resolve(
                            semanticIntent,
                            elements
                    );

            if (match == null) {

                return null;
            }

            System.out.println(
                    "[AI SEMANTIC MATCH] "
                            + match.getSelector()
            );

            System.out.println(
                    "[AI CONFIDENCE] "
                            + match.getConfidence()
            );

            Locator locator =
                    page.locator(
                            match.getSelector()
                    );

            if (locator.count() > 0) {

                return locator.first();
            }

            return null;

        } catch (Exception e) {

            System.out.println(
                    "[AI SEMANTIC RUNTIME RESOLVER FAILED] "
                            + SensitiveLogSanitizer.redact(
                            e.getMessage()
                    )
            );

            return null;
        }
    }
}
