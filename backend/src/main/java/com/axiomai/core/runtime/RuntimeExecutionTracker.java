package com.axiomai.core.runtime;

import com.axiomai.core.execution.ExecutionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor

public class RuntimeExecutionTracker {

    // =====================================================
    // START
    // =====================================================

    public UnifiedRuntimeContext startExecution(
            String flowName
    ) {

        return UnifiedRuntimeContext.builder()

                .executionId(
                        UUID.randomUUID().toString()
                )

                .flowName(flowName)

                .status("RUNNING")

                .startedAt(
                        LocalDateTime.now()
                )

                .build();
    }

    // =====================================================
    // COMPLETE
    // =====================================================

    public ExecutionResult completeExecution(
            UnifiedRuntimeContext context
    ) {

        context.setStatus("COMPLETED");

        context.setCompletedAt(
                LocalDateTime.now()
        );

        return ExecutionResult.builder()

                .success(true)

                .executionId(
                        context.getExecutionId()
                )

                .flowName(
                        context.getFlowName()
                )

                .executedNodes(
                        context.getExecutedNodes()
                )

                .failedNodes(
                        context.getFailedNodes()
                )

                .message(
                        "Execution completed successfully."
                )

                .executedAt(
                        context.getCompletedAt()
                )

                .build();
    }

    // =====================================================
    // FAIL
    // =====================================================

    public ExecutionResult failExecution(

            UnifiedRuntimeContext context,

            Exception e

    ) {

        context.setStatus("FAILED");

        context.setCompletedAt(
                LocalDateTime.now()
        );

        return ExecutionResult.builder()

                .success(false)

                .executionId(
                        context.getExecutionId()
                )

                .flowName(
                        context.getFlowName()
                )

                .executedNodes(
                        context.getExecutedNodes()
                )

                .failedNodes(
                        context.getFailedNodes()
                )

                .message(
                        e.getMessage()
                )

                .executedAt(
                        context.getCompletedAt()
                )

                .build();
    }
}