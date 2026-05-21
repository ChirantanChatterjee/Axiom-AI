package com.axiomai.ai.runtime;

public class LLMCircuitBreaker {

    private int failureCount;
    private boolean disabled;

    public boolean canUseLLM() {
        return !disabled;
    }

    public void recordFailure() {
        failureCount++;

        if (failureCount >= 2) {
            disabled = true;
        }
    }

    public void reset() {
        failureCount = 0;
        disabled = false;
    }
}
