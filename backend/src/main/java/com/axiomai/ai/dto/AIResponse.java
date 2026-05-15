package com.axiomai.ai.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class AIResponse {

    private boolean success;

    private String message;

    private Object data;

}