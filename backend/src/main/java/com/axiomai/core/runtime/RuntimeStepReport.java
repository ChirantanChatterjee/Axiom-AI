package com.axiomai.core.runtime;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuntimeStepReport {

    private int stepOrder;

    private String nodeId;

    private String action;

    private String target;

    private String status;

    private long durationMs;

    private String screenshotPath;

    private String errorMessage;

    private LocalDateTime executedAt;
}
