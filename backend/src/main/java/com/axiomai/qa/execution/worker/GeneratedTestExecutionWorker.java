package com.axiomai.qa.execution.worker;

import com.axiomai.qa.execution.entity.GeneratedTestExecutionJobEntity;
import com.axiomai.qa.execution.service.GeneratedTestExecutionQueueService;
import com.axiomai.qa.service.GeneratedTestExecutionService;
import com.axiomai.workspace.AutomationWorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(
        name = "aif.worker.enabled",
        havingValue = "true"
)
@RequiredArgsConstructor
@Slf4j
public class GeneratedTestExecutionWorker {

    private final GeneratedTestExecutionQueueService
            queueService;

    private final GeneratedTestExecutionService
            generatedTestExecutionService;

    private final AutomationWorkspaceService
            automationWorkspaceService;

    private final AtomicBoolean running =
            new AtomicBoolean(false);

    private final AtomicBoolean recoveryChecked =
            new AtomicBoolean(false);

    @Value("${aif.worker.recover-stale-running-jobs:${AIF_WORKER_RECOVER_STALE_RUNNING_JOBS:true}}")
    private boolean recoverStaleRunningJobs;

    @Value("${aif.worker.stale-running-job-seconds:${AIF_WORKER_STALE_RUNNING_JOB_SECONDS:30}}")
    private long staleRunningJobSeconds;

    @Scheduled(fixedDelayString = "${aif.worker.poll-delay-ms:5000}")
    public void poll() {

        if (
                !running.compareAndSet(
                        false,
                        true
                )
        ) {

            return;
        }

        try {

            recoverStaleRunningJobs();

            queueService.claimNext()
                    .ifPresent(this::execute);

        } finally {

            running.set(false);
        }
    }

    private void execute(
            GeneratedTestExecutionJobEntity job
    ) {

        log.info(
                "Starting generated test execution job {} for session {} with tags {}",
                job.getId(),
                job.getSessionId(),
                job.getTagExpression()
        );

        try {

            Map<String, String> variables =
                    variablesFor(job);

            GeneratedTestExecutionService.GeneratedTestRunResult result =
                    generatedTestExecutionService.runTests(
                            job.getSessionId(),
                            job.getTagExpression(),
                            variables
                    );

            queueService.complete(
                    job.getId(),
                    result
            );

            log.info(
                    "Finished generated test execution job {} with exit code {}",
                    job.getId(),
                    result.getExitCode()
            );

        } catch (Exception e) {

            queueService.fail(
                    job.getId(),
                    e
            );

            log.warn(
                    "Generated test execution job {} failed: {}",
                    job.getId(),
                    e.getMessage(),
                    e
            );
        }
    }

    private void recoverStaleRunningJobs() {

        if (
                !recoverStaleRunningJobs
                        ||
                        !recoveryChecked.compareAndSet(
                                false,
                                true
                        )
        ) {

            return;
        }

        long staleSeconds =
                Math.max(
                        0,
                        staleRunningJobSeconds
                );

        int recoveredJobs =
                queueService.failStaleRunningJobs(
                        Instant.now()
                                .minusSeconds(staleSeconds),
                        "The worker restarted or stopped while this job was running. Please run the generated tests again."
                );

        if (
                recoveredJobs > 0
        ) {

            log.warn(
                    "Marked {} stale generated test execution job(s) as interrupted after worker startup.",
                    recoveredJobs
            );
        }
    }

    private Map<String, String> variablesFor(
            GeneratedTestExecutionJobEntity job
    ) {

        Map<String, String> variables =
                new LinkedHashMap<>(
                        automationWorkspaceService.getVariableValues(
                                job.getSessionId()
                        )
                );

        variables.putAll(
                queueService.variablesFor(job)
        );

        return variables;
    }
}
