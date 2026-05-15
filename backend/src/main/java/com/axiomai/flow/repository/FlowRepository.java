package com.axiomai.flow.repository;

import com.axiomai.flow.entity.FlowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository

public interface FlowRepository
        extends JpaRepository<FlowEntity, Long> {

    /*
     * ==========================================
     * FIND BY PROJECT
     * ==========================================
     */

    List<FlowEntity> findByProjectId(
            Long projectId
    );

    /*
     * ==========================================
     * FIND BY EXACT NAME
     * ==========================================
     */

    Optional<FlowEntity>
    findByFlowNameIgnoreCase(
            String flowName
    );

    /*
     * ==========================================
     * FIND PARTIAL FLOW NAME
     * LATEST FLOW FIRST
     * ==========================================
     */

    Optional<FlowEntity>
    findTopByFlowNameContainingIgnoreCaseOrderByIdDesc(
            String flowName
    );

    /*
     * ==========================================
     * FIND DOMAIN
     * LATEST FLOW FIRST
     * ==========================================
     */

    Optional<FlowEntity>
    findTopByDomainNameContainingIgnoreCaseOrderByIdDesc(
            String domainName
    );
}