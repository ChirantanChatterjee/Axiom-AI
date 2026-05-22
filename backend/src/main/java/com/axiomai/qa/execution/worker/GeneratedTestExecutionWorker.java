package com.axiomai.qa.execution.worker;

import com.axiomai.qa.execution.entity.GeneratedTestExecutionJobEntity;
import com.axiomai.qa.execution.service.GeneratedTestExecutionQueueService;
import com.axiomai.qa.service.GeneratedTestExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    private final AtomicBoolean running =
            new AtomicBoolean(false);

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
                    queueService.variablesFor(job);

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
}
