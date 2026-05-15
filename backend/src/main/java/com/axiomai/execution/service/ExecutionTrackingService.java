package com.axiomai.execution.service;

import com.axiomai.execution.entity.FlowExecutionEntity;
import com.axiomai.execution.entity.StepExecutionEntity;
import com.axiomai.execution.repository.FlowExecutionRepository;
import com.axiomai.execution.repository.StepExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor

public class ExecutionTrackingService {

    private final FlowExecutionRepository
            flowExecutionRepository;

    private final StepExecutionRepository
            stepExecutionRepository;

    public FlowExecutionEntity startExecution(
            Long flowId
    ) {

        FlowExecutionEntity execution =
                FlowExecutionEntity.builder()
                        .flowId(flowId)
                        .status("RUNNING")
                        .startedAt(LocalDateTime.now())
                        .build();

        return flowExecutionRepository.save(execution);

    }

    public void completeExecution(
            FlowExecutionEntity execution
    ) {

        execution.setStatus("PASSED");

        execution.setCompletedAt(
                LocalDateTime.now()
        );

        flowExecutionRepository.save(execution);

    }

    public void failExecution(
            FlowExecutionEntity execution,
            Exception exception
    ) {

        execution.setStatus("FAILED");

        execution.setCompletedAt(
                LocalDateTime.now()
        );

        execution.setErrorMessage(
                exception.getMessage()
        );

        flowExecutionRepository.save(execution);

    }

    public void saveStepExecution(
            StepExecutionEntity stepExecution
    ) {

        stepExecutionRepository.save(
                stepExecution
        );

    }

    public List<StepExecutionEntity>
    getStepExecutions(
            Long executionId
    ) {

        return stepExecutionRepository
                .findByFlowExecutionIdOrderByStepOrderAsc(
                        executionId
                );

    }

}