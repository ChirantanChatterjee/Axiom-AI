package com.axiomai.qa.runtime;

import com.microsoft.playwright.Locator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class OverlayActionCandidate {

    // =====================================================
    // ACTION NAME
    // =====================================================

    private String actionName;

    // =====================================================
    // ACTION LOCATOR
    // =====================================================

    private Locator locator;

    // =====================================================
    // CONFIDENCE
    // =====================================================

    private double confidence;

}