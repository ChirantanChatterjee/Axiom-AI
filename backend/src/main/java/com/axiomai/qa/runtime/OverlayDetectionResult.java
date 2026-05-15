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

public class OverlayDetectionResult {

    // =====================================================
    // OVERLAY FOUND
    // =====================================================

    private boolean overlayFound;

    // =====================================================
    // OVERLAY LOCATOR
    // =====================================================

    private Locator overlay;

    // =====================================================
    // OVERLAY TYPE
    // =====================================================

    private String overlayType;

    // =====================================================
    // DETECTION REASON
    // =====================================================

    private String detectionReason;

}