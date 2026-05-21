package com.axiomai.flow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class CreateFlowRequest {

    @NotNull
    private Long projectId;

    @NotBlank
    private String flowName;

    private String description;
}