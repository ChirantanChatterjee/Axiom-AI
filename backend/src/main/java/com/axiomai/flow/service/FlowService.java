package com.axiomai.flow.service;

import com.axiomai.flow.dto.CreateFlowRequest;
import com.axiomai.flow.entity.FlowEntity;
import com.axiomai.flow.repository.FlowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor

public class FlowService {

    private final FlowRepository flowRepository;

    // =====================================================
    // CREATE FLOW
    // =====================================================

    public FlowEntity createFlow(CreateFlowRequest request) {

        FlowEntity flow = FlowEntity.builder()
                .projectId(request.getProjectId())
                .flowName(request.getFlowName())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .build();

        return flowRepository.save(flow);
    }

    // =====================================================
    // GET ALL FLOWS
    // =====================================================

    public List<FlowEntity> getAllFlows() {
        return flowRepository.findAll();
    }

    // =====================================================
    // GET FLOWS BY PROJECT
    // =====================================================

    public List<FlowEntity> getFlowsByProject(Long projectId) {
        return flowRepository.findByProjectId(projectId);
    }
}