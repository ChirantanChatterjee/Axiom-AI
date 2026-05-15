package com.axiomai.runtime.locator;

import com.microsoft.playwright.Locator;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class LocatorValidationResult {

    private boolean success;

    private Locator locator;

    private String strategyUsed;

    private String resolvedLocator;

}