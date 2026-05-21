package com.axiomai.flowstep.repository;

import com.axiomai.flowstep.entity.FlowStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlowStepRepository
        extends JpaRepository<FlowStepEntity, Long> {

    List<FlowStepEntity> findByFlowIdOrderByStepOrderAsc(Long flowId);

}