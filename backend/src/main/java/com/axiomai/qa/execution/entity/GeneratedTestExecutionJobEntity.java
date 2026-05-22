package com.axiomai.qa.execution.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "generated_test_execution_jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedTestExecutionJobEntity {

    @Id
    @Column(nullable = false, length = 36)
    private String id;

    @Column(nullable = false, length = 128)
    private String sessionId;

    @Column(nullable = false, length = 128)
    private String userId;

    @Column(length = 255)
    private String tagExpression;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(columnDefinition = "text")
    private String variablesJson;

    @Column(columnDefinition = "text")
    private String output;

    @Column(columnDefinition = "text")
    private String errorMessage;

    @Column(length = 2048)
    private String reportUrl;

    private Integer exitCode;

    @Column(columnDefinition = "text")
    private String message;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant startedAt;

    private Instant finishedAt;
}
