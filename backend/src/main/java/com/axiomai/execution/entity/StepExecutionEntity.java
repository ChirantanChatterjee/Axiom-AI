package com.axiomai.execution.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "step_executions")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class StepExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long flowExecutionId;

    private Integer stepOrder;

    private String action;

    private String elementName;

    private String status;

    private Long durationMs;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(length = 5000)
    private String errorMessage;

    @Column(length = 5000)
    private String screenshotPath;

    @Column(length = 1000)
    private String locatorStrategy;

    @Column(length = 5000)
    private String expectedValue;

    @Column(length = 5000)
    private String actualValue;

}