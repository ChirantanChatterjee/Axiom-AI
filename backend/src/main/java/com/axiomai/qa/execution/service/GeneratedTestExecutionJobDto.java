package com.axiomai.qa.execution.service;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class GeneratedTestExecutionJobDto {

    private String jobId;

    private String sessionId;

    private String tagExpression;

    private String status;

    private boolean success;

    private String reportUrl;

    private Integer exitCode;

    private String message;

    private String errorMessage;

    private Instant createdAt;

    private Instant updatedAt;

    private Instant startedAt;

    private Instant finishedAt;
}
