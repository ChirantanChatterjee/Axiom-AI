package com.axiomai.qa.ai;

import java.util.Comparator;
import java.util.List;

public class SelectorRanker {

    // =====================================================
    // GET BEST SELECTOR
    // =====================================================

    public static SelectorCandidate getBestSelector(
            List<SelectorCandidate> candidates
    ) {

        return candidates
                .stream()
                .max(
                        Comparator.comparingInt(
                                SelectorCandidate::getScore
                        )
                )
                .orElse(null);
    }
}