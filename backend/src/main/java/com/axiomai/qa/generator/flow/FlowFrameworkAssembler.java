package com.axiomai.qa.generator.flow;

import com.axiomai.qa.flow.DetectedFlow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FlowFrameworkAssembler {

    @Autowired
    private FlowFeatureGenerator flowFeatureGenerator;

    @Autowired
    private FlowPageObjectGenerator flowPageObjectGenerator;

    @Autowired
    private FlowStepDefinitionGenerator flowStepDefinitionGenerator;

    @Autowired
    private FlowAssertionGenerator flowAssertionGenerator;

    // =====================================================
    // MAIN ASSEMBLER
    // =====================================================

    public void assemble(
            List<DetectedFlow> flows
    ) {

        String feature =
                flowFeatureGenerator.generate(flows);

        String pageObject =
                flowPageObjectGenerator.generate(flows);

        String steps =
                flowStepDefinitionGenerator.generate(flows);

        String assertions =
                flowAssertionGenerator.generateAssertion();

        System.out.println(feature);

        System.out.println(pageObject);

        System.out.println(steps);

        System.out.println(assertions);
    }
}