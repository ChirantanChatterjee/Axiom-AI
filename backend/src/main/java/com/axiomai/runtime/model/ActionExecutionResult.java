package com.axiomai.runtime.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder

public class ActionExecutionResult {

    private boolean success;

    private String locatorUsed;

    private String locatorStrategy;

    private int retryCount;

    private long durationMs;

    private String errorMessage;

    private boolean healed;

    private boolean fallbackUsed;

}