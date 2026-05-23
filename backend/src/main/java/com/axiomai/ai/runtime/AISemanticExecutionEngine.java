package com.axiomai.ai.runtime;

import com.axiomai.qa.ai.ElementSemanticMatch;
import com.axiomai.qa.ai.LLMSemanticLocatorResolver;
import com.axiomai.qa.ai.LocalSemanticResolver;
import com.axiomai.qa.models.FlowStep;
import com.axiomai.qa.models.PageElement;
import com.axiomai.security.SensitiveLogSanitizer;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AISemanticExecutionEngine {

    private final RuntimePageElementExtractor
            elementExtractor;

    private final LocalSemanticResolver
            localSemanticResolver;

    private final LLMSemanticLocatorResolver
            llmSemanticResolver;

    // =====================================================
    // RESOLVE
    // =====================================================

    public Locator resolve(

            Page page,
            FlowStep step

    ) {

        try {

            // =============================================
            // RUNTIME EXTRACTION
            // =============================================

            List<PageElement> elements =
                    elementExtractor.extract(
                            page
                    );

            System.out.println(
                    "[RUNTIME ELEMENTS] "
                            + elements.size()
            );

            System.out.println(
                    "[SEMANTIC TARGET] "
                            + step.getTarget()
            );

            // =============================================
            // LOCAL SEMANTIC RESOLUTION
            // =============================================

            ElementSemanticMatch match = null;

            try {

                match =
                        localSemanticResolver.resolve(
                                step.getTarget(),
                                elements
                        );

                if (match != null) {

                    System.out.println(
                            "[SEMANTIC ENGINE] USING LOCAL RESOLUTION"
                    );
                }

            } catch (Exception e) {

                System.out.println(
                        "[SEMANTIC ENGINE] LOCAL RESOLUTION FAILED"
                );
            }

            // =============================================
            // LLM FALLBACK
            // =============================================

            if (match == null) {

                try {

                    System.out.println(
                            "[SEMANTIC ENGINE] USING LLM FALLBACK"
                    );

                    match =
                            llmSemanticResolver.resolve(
                                    step.getTarget(),
                                    elements
                            );

                } catch (Exception e) {

                    System.out.println(
                            "[SEMANTIC ENGINE] LLM FAILED"
                    );
                }
            }

            // =============================================
            // NO MATCH
            // =============================================

            if (match == null) {

                System.out.println(
                        "[SEMANTIC MATCH] NONE"
                );

                return null;
            }

            // =============================================
            // CONFIDENCE CHECK
            // =============================================

            System.out.println(
                    "[SEMANTIC MATCH] "
                            + match.getSelector()
            );

            System.out.println(
                    "[SEMANTIC CONFIDENCE] "
                            + match.getConfidence()
            );

            if (match.getConfidence() < 0.75) {

                System.out.println(
                        "[SEMANTIC MATCH] LOW CONFIDENCE"
                );

                return null;
            }

            // =============================================
            // CREATE LOCATOR
            // =============================================

            Locator locator =
                    page.locator(
                            match.getSelector()
                    );

            // =============================================
            // LOCATOR VALIDATION
            // =============================================

            if (locator.count() > 0) {

                Locator resolved =
                        locator.first();

                try {

                    // =====================================
                    // VISIBLE
                    // =====================================

                    boolean visible =
                            resolved.isVisible();

                    if (!visible) {

                        System.out.println(
                                "[LOCATOR VALIDATION] ELEMENT NOT VISIBLE"
                        );

                        return null;
                    }

                    // =====================================
                    // ENABLED
                    // =====================================

                    boolean enabled =
                            resolved.isEnabled();

                    if (!enabled) {

                        System.out.println(
                                "[LOCATOR VALIDATION] ELEMENT DISABLED"
                        );

                        return null;
                    }

                    // =====================================
                    // ARIA HIDDEN
                    // =====================================

                    String ariaHidden =
                            resolved.getAttribute(
                                    "aria-hidden"
                            );

                    if ("true".equalsIgnoreCase(ariaHidden)) {

                        System.out.println(
                                "[LOCATOR VALIDATION] aria-hidden=true"
                        );

                        return null;
                    }

                    // =====================================
                    // TABINDEX
                    // =====================================

                    String tabindex =
                            resolved.getAttribute(
                                    "tabindex"
                            );

                    if ("-1".equals(tabindex)) {

                        System.out.println(
                                "[LOCATOR VALIDATION] tabindex=-1"
                        );

                        return null;
                    }

                    // =====================================
                    // SUCCESS
                    // =====================================

                    System.out.println(
                            "[LOCATOR VALIDATION] SUCCESS"
                    );

                    return resolved;

                } catch (Exception e) {

                    System.out.println(
                            "[LOCATOR VALIDATION] FAILED"
                    );

                    System.out.println(
                            SensitiveLogSanitizer.redact(
                                    e.getMessage()
                            )
                    );

                    return null;
                }
            }

            System.out.println(
                    "[LOCATOR VALIDATION] NO ELEMENT FOUND"
            );

            return null;

        } catch (Exception e) {

            System.out.println(
                    "[SEMANTIC ENGINE] FAILED"
            );

            System.out.println(
                    SensitiveLogSanitizer.redact(
                            e.getMessage()
                    )
            );

            return null;
        }
    }
}
