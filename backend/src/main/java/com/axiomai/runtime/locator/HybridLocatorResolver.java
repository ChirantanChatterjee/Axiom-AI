package com.axiomai.runtime.locator;

import com.axiomai.ai.service.OpenAIService;
import com.axiomai.runtime.memory.LocatorMemoryStore;
import com.axiomai.runtime.session.ExecutionSessionContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HybridLocatorResolver {

    private final LocatorMemoryStore memoryStore =
            new LocatorMemoryStore();

    private final ElementInteractabilityScorer scorer =
            new ElementInteractabilityScorer();

    private final OpenAIService openAIService;

    public HybridLocatorResolver(
            OpenAIService openAIService
    ) {

        this.openAIService = openAIService;
    }

    public CandidateElement resolve(

            Page page,
            String domain,
            String semanticTarget,
            ExecutionSessionContext context
    ) {

        System.out.println(
                "[HYBRID RESOLVER] TARGET -> "
                        + semanticTarget);

        // =========================================
        // 1. MEMORY LOOKUP
        // =========================================

        String memorySelector =
                memoryStore.retrieve(
                        domain,
                        semanticTarget);

        if(memorySelector != null) {

            try {

                Locator locator =
                        page.locator(memorySelector);

                if(locator.count() > 0
                        && locator.first().isVisible()
                ) {

                    CandidateElement candidate =
                            new CandidateElement();

                    candidate.setSelector(
                            memorySelector);

                    candidate.setSemanticScore(5);

                    candidate.setVisibilityScore(5);

                    candidate.setInteractabilityScore(5);

                    candidate.calculateFinalScore();

                    System.out.println(
                            "[HYBRID RESOLVER] MEMORY HIT");

                    return candidate;
                }

            } catch (Exception ignored) {

            }
        }

        // =========================================
        // 2. BUILD CANDIDATES
        // =========================================

        List<CandidateElement> candidates =
                buildCandidates(
                        page,
                        semanticTarget);

        // =========================================
        // 3. SCORE CANDIDATES
        // =========================================

        for(CandidateElement candidate
                : candidates
        ) {

            try {

                Locator locator =
                        page.locator(
                                candidate.getSelector());

                double interactability =
                        scorer.score(locator.first());

                candidate.setInteractabilityScore(
                        interactability);

                candidate.calculateFinalScore();

            } catch (Exception ignored) {

            }
        }

        // =========================================
        // 4. SORT BEST
        // =========================================

        candidates.sort(
                Comparator.comparingDouble(
                                CandidateElement::getFinalScore)
                        .reversed());

        if(!candidates.isEmpty()) {

            CandidateElement best =
                    candidates.get(0);

            System.out.println(
                    "[HYBRID RESOLVER] BEST MATCH -> "
                            + best.getSelector());

            if(best.getFinalScore() >= 2.5) {

                memoryStore.store(
                        domain,
                        semanticTarget,
                        best.getSelector());

                return best;
            }
        }

        // =========================================
        // 5. LLM FALLBACK
        // =========================================

        if(context.isLlmDisabled()) {

            throw new RuntimeException(
                    "LLM disabled and no candidate found");
        }

        return resolveUsingLLM(
                page,
                semanticTarget);
    }

    private List<CandidateElement> buildCandidates(
            Page page,
            String semanticTarget
    ) {

        List<CandidateElement> candidates =
                new ArrayList<>();

        try {

            if(semanticTarget.contains("PASSWORD")) {

                addCandidate(
                        candidates,
                        "input[type='password']",
                        3.0);
            }

            if(semanticTarget.contains("USERNAME")) {

                addCandidate(
                        candidates,
                        "input[type='email']",
                        3.0);

                addCandidate(
                        candidates,
                        "input[type='text']",
                        2.0);
            }

            if(semanticTarget.contains("LOGIN")
                    || semanticTarget.contains("SIGN_IN")
            ) {

                addCandidate(
                        candidates,
                        "button[type='submit']",
                        2.5);

                addCandidate(
                        candidates,
                        "button",
                        1.0);
            }

        } catch (Exception ignored) {

        }

        return candidates;
    }

    private void addCandidate(

            List<CandidateElement> list,
            String selector,
            double semanticScore
    ) {

        CandidateElement candidate =
                new CandidateElement();

        candidate.setSelector(selector);

        candidate.setSemanticScore(
                semanticScore);

        list.add(candidate);
    }

    private CandidateElement resolveUsingLLM(

            Page page,
            String semanticTarget
    ) {

        System.out.println(
                "[HYBRID RESOLVER] USING LLM");

        try {

            String response =
                    openAIService.ask(
                            "Find selector for "
                                    + semanticTarget);

            CandidateElement candidate =
                    new CandidateElement();

            candidate.setSelector(response);

            candidate.setSemanticScore(2);

            candidate.setVisibilityScore(1);

            candidate.calculateFinalScore();

            return candidate;

        } catch (Exception e) {

            throw new RuntimeException(
                    "LLM locator resolution failed");
        }
    }
}