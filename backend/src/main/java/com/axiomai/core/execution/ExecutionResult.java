package com.axiomai.core.execution;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ExecutionResult {

    private boolean success;

    private String executionId;

    private String flowName;

    private String message;

    @Builder.Default
    private List<String> executedNodes =
            new ArrayList<>();

    @Builder.Default
    private List<String> failedNodes =
            new ArrayList<>();

    private String reportPath;

    private LocalDateTime executedAt;
}