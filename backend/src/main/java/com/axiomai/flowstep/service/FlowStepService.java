package com.axiomai.flowstep.service;

import com.axiomai.flowstep.dto.CreateFlowStepRequest;
import com.axiomai.flowstep.entity.FlowStepEntity;
import com.axiomai.flowstep.repository.FlowStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FlowStepService {

    private final FlowStepRepository flowStepRepository;

    public FlowStepEntity createFlowStep(
            CreateFlowStepRequest request
    ) {

        FlowStepEntity step =
                FlowStepEntity.builder()
                        .flowId(request.getFlowId())
                        .stepOrder(request.getStepOrder())
                        .action(request.getAction())
                        .elementName(request.getElementName())
                        .locatorType(request.getLocatorType())
                        .locatorValue(request.getLocatorValue())
                        .fallbackLocator(request.getFallbackLocator())
                        .aiSemanticDescription(request.getAiSemanticDescription())
                        .inputValue(request.getInputValue())
                        .required(request.getRequired())
                        .build();

        return flowStepRepository.save(step);

    }

    public List<FlowStepEntity> getFlowSteps(
            Long flowId
    ) {

        return flowStepRepository
                .findByFlowIdOrderByStepOrderAsc(flowId);

    }

}