package com.axiomai.project.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class CreateProjectRequest {

    private String projectName;

    private String baseUrl;

    private String description;
}