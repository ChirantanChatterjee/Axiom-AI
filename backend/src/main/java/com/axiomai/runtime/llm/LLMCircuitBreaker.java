package com.axiomai.runtime.llm;

public class LLMCircuitBreaker {

    private int failureCount = 0;

    private boolean disabled = false;

    private long disabledUntil = 0;

    private static final int MAX_FAILURES = 2;

    private static final long COOLDOWN_MS = 120000;

    public boolean canUseLLM() {

        if (disabled &&
                System.currentTimeMillis() > disabledUntil) {

            reset();
        }

        return !disabled;
    }

    public void recordFailure() {

        failureCount++;

        if (failureCount >= MAX_FAILURES) {

            disabled = true;

            disabledUntil =
                    System.currentTimeMillis()
                            + COOLDOWN_MS;

            System.out.println(
                    "[LLM CIRCUIT BREAKER] DISABLED");
        }
    }

    public void recordSuccess() {
        reset();
    }

    public void reset() {

        failureCount = 0;

        disabled = false;

        disabledUntil = 0;
    }
}