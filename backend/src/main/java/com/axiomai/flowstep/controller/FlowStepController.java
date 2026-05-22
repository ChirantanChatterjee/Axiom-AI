package com.axiomai.flowstep.controller;

import com.axiomai.flowstep.dto.CreateFlowStepRequest;
import com.axiomai.flowstep.entity.FlowStepEntity;
import com.axiomai.flowstep.service.FlowStepService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flow-steps")
@RequiredArgsConstructor
public class FlowStepController {

    private final FlowStepService flowStepService;

    @PostMapping
    public FlowStepEntity createFlowStep(

            @Valid
            @RequestBody
            CreateFlowStepRequest request

    ) {

        return flowStepService.createFlowStep(request);

    }

    @GetMapping("/{flowId}")
    public List<FlowStepEntity> getFlowSteps(

            @PathVariable
            Long flowId

    ) {

        return flowStepService.getFlowSteps(flowId);

    }

}