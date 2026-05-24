package com.axiomai.qa.execution.service;

import com.axiomai.audit.AuditLogService;
import com.axiomai.audit.entity.AuditLogEntity;
import com.axiomai.audit.repository.AuditLogRepository;
import com.axiomai.qa.execution.entity.GeneratedTestExecutionJobEntity;
import com.axiomai.qa.execution.repository.GeneratedTestExecutionJobRepository;
import com.axiomai.qa.service.GeneratedTestExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedTestExecutionQueueAuditTest {

    @Test
    void generatedJobLifecycleCreatesAuditEntries() {

        AtomicReference<GeneratedTestExecutionJobEntity> savedJob =
                new AtomicReference<>();

        GeneratedTestExecutionJobRepository repository =
                generatedTestExecutionJobRepository(savedJob);

        List<AuditLogEntity> savedAudits =
                new ArrayList<>();

        AuditLogRepository auditRepository =
                auditRepository(savedAudits);

        GeneratedTestExecutionQueueService queueService =
                new GeneratedTestExecutionQueueService(
                        repository,
                        new ObjectMapper()
                );

        queueService.setAuditLogService(
                new AuditLogService(
                        auditRepository,
                        new ObjectMapper()
                )
        );

        GeneratedTestExecutionJobEntity job =
                queueService.enqueue(
                        "session-1",
                        "user-1",
                        "@tc_005",
                        Map.of("password", "DontStoreMe")
                );

        queueService.claimNext();

        queueService.complete(
                job.getId(),
                GeneratedTestExecutionService.GeneratedTestRunResult.builder()
                        .success(true)
                        .tagExpression("@tc_005")
                        .reportUrl("/api/reports/report.html")
                        .exitCode(0)
                        .message("Generated test execution passed.")
                        .build()
        );

        queueService.fail(
                job.getId(),
                new RuntimeException(
                        "password=DontStoreMe"
                )
        );

        List<AuditLogEntity> audits =
                savedAudits;

        List<String> actions =
                audits.stream()
                        .map(AuditLogEntity::getAction)
                        .toList();

        assertTrue(
                actions.contains("worker.job.queued")
        );
        assertTrue(
                actions.contains("worker.job.started")
        );
        assertTrue(
                actions.contains("worker.job.completed")
        );
        assertTrue(
                actions.contains("worker.job.failed")
        );

        String persistedDetails =
                audits.stream()
                        .map(AuditLogEntity::getDetailsJson)
                        .reduce(
                                "",
                                String::concat
                        );

        assertTrue(
                persistedDetails.contains("@tc_005")
        );
        assertFalse(
                persistedDetails.contains("DontStoreMe")
        );
    }

    private GeneratedTestExecutionJobRepository generatedTestExecutionJobRepository(
            AtomicReference<GeneratedTestExecutionJobEntity> savedJob
    ) {

        return (GeneratedTestExecutionJobRepository) Proxy.newProxyInstance(
                GeneratedTestExecutionJobRepository.class.getClassLoader(),
                new Class<?>[]{
                        GeneratedTestExecutionJobRepository.class
                },
                (proxy, method, args) -> {

                    if (
                            "save".equals(method.getName())
                    ) {

                        GeneratedTestExecutionJobEntity job =
                                (GeneratedTestExecutionJobEntity) args[0];

                        savedJob.set(job);

                        return job;
                    }

                    if (
                            "findQueuedJobsForUpdate".equals(method.getName())
                    ) {

                        GeneratedTestExecutionJobEntity job =
                                savedJob.get();

                        if (
                                job != null
                                        &&
                                        GeneratedTestExecutionQueueService.STATUS_QUEUED.equals(
                                                job.getStatus()
                                        )
                        ) {

                            return List.of(job);
                        }

                        return List.of();
                    }

                    if (
                            "findById".equals(method.getName())
                    ) {

                        GeneratedTestExecutionJobEntity job =
                                savedJob.get();

                        if (
                                job != null
                                        &&
                                        job.getId()
                                                .equals(args[0])
                        ) {

                            return Optional.of(job);
                        }

                        return Optional.empty();
                    }

                    if (
                            "toString".equals(method.getName())
                    ) {

                        return "StubGeneratedTestExecutionJobRepository";
                    }

                    throw new UnsupportedOperationException(
                            method.getName()
                    );
                }
        );
    }

    private AuditLogRepository auditRepository(
            List<AuditLogEntity> savedAudits
    ) {

        return (AuditLogRepository) Proxy.newProxyInstance(
                AuditLogRepository.class.getClassLoader(),
                new Class<?>[]{
                        AuditLogRepository.class
                },
                (proxy, method, args) -> {

                    if (
                            "save".equals(method.getName())
                    ) {

                        AuditLogEntity audit =
                                (AuditLogEntity) args[0];

                        savedAudits.add(audit);

                        return audit;
                    }

                    if (
                            "toString".equals(method.getName())
                    ) {

                        return "StubAuditLogRepository";
                    }

                    throw new UnsupportedOperationException(
                            method.getName()
                    );
                }
        );
    }
}
