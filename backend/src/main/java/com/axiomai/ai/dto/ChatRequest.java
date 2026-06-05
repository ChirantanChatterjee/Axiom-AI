package com.axiomai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class ChatRequest {

    private String message;

    private String sessionId;

    private String websiteUrl;

    private String domainName;

    private Boolean frameworkLocked;

    private String intent;

    private Map<String, String> variables =
            new LinkedHashMap<>();

    public ChatRequest(
            String message,
            String sessionId,
            String websiteUrl,
            String domainName,
            Boolean frameworkLocked
    ) {

        this.message =
                message;
        this.sessionId =
                sessionId;
        this.websiteUrl =
                websiteUrl;
        this.domainName =
                domainName;
        this.frameworkLocked =
                frameworkLocked;
    }

}
