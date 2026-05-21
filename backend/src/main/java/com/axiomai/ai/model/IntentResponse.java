package com.axiomai.ai.model;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class IntentResponse {

    private String intent;

    private String flowName;

    private Long flowId;

    private String url;

    private String testName;

    private String rawResponse;

}