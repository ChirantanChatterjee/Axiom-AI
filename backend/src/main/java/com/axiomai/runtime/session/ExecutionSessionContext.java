package com.axiomai.runtime.session;

import java.util.*;

public class ExecutionSessionContext {

    private String sessionId;

    private String currentState;

    private String previousState;

    private String previousAction;

    private boolean overlayHandled;

    private boolean llmDisabled;

    private long lastDomMutation;

    private final Set<String> failedLocators =
            new HashSet<>();

    private final Map<String, String> resolvedLocators =
            new HashMap<>();

    private final Map<String, Object> metadata =
            new HashMap<>();

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    public String getPreviousState() {
        return previousState;
    }

    public void setPreviousState(String previousState) {
        this.previousState = previousState;
    }

    public String getPreviousAction() {
        return previousAction;
    }

    public void setPreviousAction(String previousAction) {
        this.previousAction = previousAction;
    }

    public boolean isOverlayHandled() {
        return overlayHandled;
    }

    public void setOverlayHandled(boolean overlayHandled) {
        this.overlayHandled = overlayHandled;
    }

    public boolean isLlmDisabled() {
        return llmDisabled;
    }

    public void setLlmDisabled(boolean llmDisabled) {
        this.llmDisabled = llmDisabled;
    }

    public long getLastDomMutation() {
        return lastDomMutation;
    }

    public void setLastDomMutation(long lastDomMutation) {
        this.lastDomMutation = lastDomMutation;
    }

    public Set<String> getFailedLocators() {
        return failedLocators;
    }

    public Map<String, String> getResolvedLocators() {
        return resolvedLocators;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}