package com.axiomai.execution.repository;

import com.axiomai.execution.entity.FlowExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlowExecutionRepository
        extends JpaRepository<FlowExecutionEntity, Long> {
}