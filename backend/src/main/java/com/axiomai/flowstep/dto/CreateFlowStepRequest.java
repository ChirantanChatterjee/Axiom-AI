package com.axiomai.flowstep.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFlowStepRequest {

    private Long flowId;

    private Integer stepOrder;

    private String action;

    private String elementName;

    private String locatorType;

    private String locatorValue;

    private String fallbackLocator;

    private String aiSemanticDescription;

    private String inputValue;

    private Boolean required;

}