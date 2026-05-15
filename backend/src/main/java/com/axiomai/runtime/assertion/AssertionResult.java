package com.axiomai.runtime.assertion;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder

public class AssertionResult {

    private boolean success;

    private String assertionType;

    private String expectedValue;

    private String actualValue;

    private String message;

    private long durationMs;

}