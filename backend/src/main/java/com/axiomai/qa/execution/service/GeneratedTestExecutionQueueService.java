package com.axiomai.qa.execution.service;

import com.axiomai.audit.AuditLogService;
import com.axiomai.qa.execution.entity.GeneratedTestExecutionJobEntity;
import com.axiomai.qa.execution.repository.GeneratedTestExecutionJobRepository;
import com.axiomai.qa.service.GeneratedTestExecutionService;
import com.axiomai.security.SensitiveLogSanitizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GeneratedTestExecutionQueueService {

    public static final String STATUS_QUEUED =
            "QUEUED";

    public static final String STATUS_RUNNING =
            "RUNNING";

    public static final String STATUS_PASSED =
            "PASSED";

    public static final String STATUS_FAILED =
            "FAILED";

    private static final int MAX_STORED_OUTPUT_CHARS =
            120_000;

    private final GeneratedTestExecutionJobRepository
            repository;

    private final ObjectMapper
            objectMapper;

    private AuditLogService
            auditLogService;

    @Autowired(required = false)
    public void setAuditLogService(
            AuditLogService auditLogService
    ) {

        this.auditLogService =
                auditLogService;
    }

    @Transactional
    public GeneratedTestExecutionJobEntity enqueue(
            String sessionId,
            String userId,
            String tagExpression,
            Map<String, String> variables
    ) {

        Instant now =
                Instant.now();

        GeneratedTestExecutionJobEntity job =
                GeneratedTestExecutionJobEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .sessionId(sessionId)
                        .userId(userId)
                        .tagExpression(tagExpression)
                        .status(STATUS_QUEUED)
                        .variablesJson(
                                serializeVariables(variables)
                        )
                        .message(
                                "Generated test execution is queued."
                        )
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

        GeneratedTestExecutionJobEntity savedJob =
                repository.save(job);

        auditJob(
                savedJob,
                "worker.job.queued",
                AuditLogService.OUTCOME_SUCCESS,
                null
        );

        return savedJob;
    }

    @Transactional
    public Optional<GeneratedTestExecutionJobEntity> claimNext() {

        List<GeneratedTestExecutionJobEntity> jobs =
                repository.findQueuedJobsForUpdate(
                        PageRequest.of(
                                0,
                                1
                        )
                );

        if (
                jobs.isEmpty()
        ) {

            return Optional.empty();
        }

        GeneratedTestExecutionJobEntity job =
                jobs.get(0);

        Instant now =
                Instant.now();

        job.setStatus(STATUS_RUNNING);
        job.setStartedAt(now);
        job.setUpdatedAt(now);
        job.setMessage(
                "Generated test execution is running."
        );

        GeneratedTestExecutionJobEntity savedJob =
                repository.save(job);

        auditJob(
                savedJob,
                "worker.job.started",
                AuditLogService.OUTCOME_SUCCESS,
                null
        );

        return Optional.of(savedJob);
    }

    @Transactional(readOnly = true)
    public Optional<GeneratedTestExecutionJobEntity> find(
            String jobId
    ) {

        return repository.findById(jobId);
    }

    @Transactional(readOnly = true)
    public Optional<GeneratedTestExecutionJobEntity> findLatestForSession(
            String sessionId
    ) {

        return repository.findTopBySessionIdOrderByCreatedAtDesc(
                sessionId
        );
    }

    @Transactional(readOnly = true)
    public List<GeneratedTestExecutionJobEntity> findRecentForSession(
            String sessionId
    ) {

        return repository.findTop10BySessionIdOrderByCreatedAtDesc(
                sessionId
        );
    }

    @Transactional
    public GeneratedTestExecutionJobEntity complete(
            String jobId,
            GeneratedTestExecutionService.GeneratedTestRunResult result
    ) {

        GeneratedTestExecutionJobEntity job =
                repository.findById(jobId)
                        .orElse(null);

        if (
                job == null
        ) {

            return null;
        }

        Instant now =
                Instant.now();

        job.setStatus(
                result.isSuccess()
                        ? STATUS_PASSED
                        : STATUS_FAILED
        );
        job.setReportUrl(
                result.getReportUrl()
        );
        job.setExitCode(
                result.getExitCode()
        );
        job.setMessage(
                result.getMessage()
        );
        job.setOutput(
                trimOutput(
                        result.getOutput()
                )
        );
        job.setUpdatedAt(now);
        job.setFinishedAt(now);

        GeneratedTestExecutionJobEntity savedJob =
                repository.save(job);

        auditJob(
                savedJob,
                result.isSuccess()
                        ? "worker.job.completed"
                        : "worker.job.failed",
                result.isSuccess()
                        ? AuditLogService.OUTCOME_SUCCESS
                        : AuditLogService.OUTCOME_FAILURE,
                null
        );

        return savedJob;
    }

    @Transactional
    public GeneratedTestExecutionJobEntity fail(
            String jobId,
            Exception exception
    ) {

        GeneratedTestExecutionJobEntity job =
                repository.findById(jobId)
                        .orElse(null);

        if (
                job == null
        ) {

            return null;
        }

        Instant now =
                Instant.now();

        job.setStatus(STATUS_FAILED);
        job.setMessage(
                "Generated test execution failed."
        );
        job.setErrorMessage(
                exception.getMessage()
        );
        job.setUpdatedAt(now);
        job.setFinishedAt(now);

        GeneratedTestExecutionJobEntity savedJob =
                repository.save(job);

        auditJob(
                savedJob,
                "worker.job.failed",
                AuditLogService.OUTCOME_FAILURE,
                exception
        );

        return savedJob;
    }

    @Transactional
    public int failStaleRunningJobs(
            Instant cutoff,
            String reason
    ) {

        List<GeneratedTestExecutionJobEntity> jobs =
                repository.findStaleRunningJobsForUpdate(cutoff);

        if (
                jobs.isEmpty()
        ) {

            return 0;
        }

        Instant now =
                Instant.now();

        for (
                GeneratedTestExecutionJobEntity job
                : jobs
        ) {

            job.setStatus(STATUS_FAILED);
            job.setMessage(
                    "Generated test execution was interrupted."
            );
            job.setErrorMessage(reason);
            job.setUpdatedAt(now);
            job.setFinishedAt(now);
        }

        repository.saveAll(jobs);

        for (
                GeneratedTestExecutionJobEntity job
                : jobs
        ) {

            auditJob(
                    job,
                    "worker.job.stale_recovered",
                    AuditLogService.OUTCOME_FAILURE,
                    null
            );
        }

        return jobs.size();
    }

    @Transactional
    public int deleteForSession(
            String sessionId
    ) {

        if (
                sessionId == null
                        ||
                        sessionId.isBlank()
        ) {

            return 0;
        }

        return repository.deleteForWorkspaceSession(
                sessionId.trim()
        );
    }

    public Map<String, String> variablesFor(
            GeneratedTestExecutionJobEntity job
    ) {

        if (
                job.getVariablesJson() == null
                        ||
                        job.getVariablesJson().isBlank()
        ) {

            return new LinkedHashMap<>();
        }

        try {

            return objectMapper.readValue(
                    job.getVariablesJson(),
                    new TypeReference<>() {
                    }
            );

        } catch (JsonProcessingException e) {

            throw new IllegalStateException(
                    "Unable to read generated test execution variables for job "
                            + job.getId(),
                    e
            );
        }
    }

    public GeneratedTestExecutionJobDto toDto(
            GeneratedTestExecutionJobEntity job
    ) {

        return GeneratedTestExecutionJobDto.builder()
                .jobId(job.getId())
                .sessionId(job.getSessionId())
                .tagExpression(job.getTagExpression())
                .status(job.getStatus())
                .success(
                        STATUS_PASSED.equals(job.getStatus())
                )
                .reportUrl(job.getReportUrl())
                .exitCode(job.getExitCode())
                .message(job.getMessage())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .startedAt(job.getStartedAt())
                .finishedAt(job.getFinishedAt())
                .build();
    }

    private String serializeVariables(
            Map<String, String> variables
    ) {

        try {

            return objectMapper.writeValueAsString(
                    variables == null
                            ? Map.of()
                            : variables
            );

        } catch (JsonProcessingException e) {

            throw new IllegalStateException(
                    "Unable to serialize generated test execution variables.",
                    e
            );
        }
    }

    private String trimOutput(
            String output
    ) {

        if (
                output == null
                        ||
                        output.length() <= MAX_STORED_OUTPUT_CHARS
        ) {

            return output;
        }

        return output.substring(
                output.length() - MAX_STORED_OUTPUT_CHARS
        );
    }

    private void auditJob(
            GeneratedTestExecutionJobEntity job,
            String action,
            String outcome,
            Exception exception
    ) {

        if (
                auditLogService == null
                        ||
                        job == null
        ) {

            return;
        }

        Map<String, Object> details =
                new LinkedHashMap<>();

        putIfPresent(
                details,
                "status",
                job.getStatus()
        );
        putIfPresent(
                details,
                "tagExpression",
                job.getTagExpression()
        );
        putIfPresent(
                details,
                "message",
                job.getMessage()
        );
        putIfPresent(
                details,
                "reportUrl",
                job.getReportUrl()
        );

        if (
                job.getExitCode() != null
        ) {

            details.put(
                    "exitCode",
                    job.getExitCode()
            );
        }

        if (
                exception != null
        ) {

            putIfPresent(
                    details,
                    "errorType",
                    exception.getClass()
                            .getSimpleName()
            );
            putIfPresent(
                    details,
                    "errorMessage",
                    SensitiveLogSanitizer.redact(
                            exception.getMessage()
                    )
            );

        } else if (
                job.getErrorMessage() != null
                        &&
                        !job.getErrorMessage()
                                .isBlank()
        ) {

            putIfPresent(
                    details,
                    "errorMessage",
                    SensitiveLogSanitizer.redact(
                            job.getErrorMessage()
                    )
            );
        }

        auditLogService.record(
                job.getUserId(),
                job.getSessionId(),
                action,
                "GENERATED_TEST_EXECUTION_JOB",
                job.getId(),
                outcome,
                details
        );
    }

    private void putIfPresent(
            Map<String, Object> details,
            String key,
            String value
    ) {

        if (
                value != null
                        &&
                        !value.isBlank()
        ) {

            details.put(
                    key,
                    value
            );
        }
    }
}
