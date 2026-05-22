package com.axiomai.flow.controller;

import com.axiomai.flow.dto.CreateFlowRequest;
import com.axiomai.flow.entity.FlowEntity;
import com.axiomai.flow.service.FlowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flows")

@RequiredArgsConstructor

public class FlowController {

    private final FlowService flowService;

    // =====================================================
    // CREATE FLOW
    // =====================================================

    @PostMapping

    public FlowEntity createFlow(
            @Valid @RequestBody
            CreateFlowRequest request
    ) {

        return flowService.createFlow(request);
    }

    // =====================================================
    // GET ALL FLOWS
    // =====================================================

    @GetMapping

    public List<FlowEntity> getAllFlows() {
        return flowService.getAllFlows();
    }

    // =====================================================
    // GET FLOWS BY PROJECT
    // =====================================================

    @GetMapping("/project/{projectId}")

    public List<FlowEntity> getFlowsByProject(
            @PathVariable Long projectId
    ) {

        return flowService.getFlowsByProject(projectId);
    }
}