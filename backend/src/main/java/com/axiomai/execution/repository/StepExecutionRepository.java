package com.axiomai.execution.repository;

import com.axiomai.execution.entity.StepExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StepExecutionRepository
        extends JpaRepository<StepExecutionEntity, Long> {

    List<StepExecutionEntity>
    findByFlowExecutionIdOrderByStepOrderAsc(
            Long flowExecutionId
    );

}