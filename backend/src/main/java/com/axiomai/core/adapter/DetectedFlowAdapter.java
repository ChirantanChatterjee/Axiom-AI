package com.axiomai.core.adapter;

import com.axiomai.core.graph.ActionNode;
import com.axiomai.core.graph.FlowGraph;
import com.axiomai.qa.flow.DetectedFlow;
import com.axiomai.qa.models.FlowStep;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component

public class DetectedFlowAdapter {

    public FlowGraph convert(
            DetectedFlow detectedFlow
    ) {

        List<ActionNode> nodes =
                new ArrayList<>();

        for (FlowStep step : detectedFlow.getSteps()) {

            ActionNode node =

                    ActionNode.builder()

                            .nodeId(
                                    UUID.randomUUID().toString()
                            )

                            .actionType(
                                    step.getAction()
                            )

                            .semanticTarget(
                                    step.getTarget()
                            )

                            .primaryLocator(
                                    step.getSelector()
                            )

                            .fallbackLocators(
                                    step.getFallbackSelectors()
                            )

                            .businessRole(
                                    step.getBusinessRole()
                            )

                            .confidenceScore(
                                    step.getConfidenceScore()
                            )

                            .description(
                                    step.getSemanticDescription()
                            )

                            .build();

            nodes.add(node);
        }

        return FlowGraph.builder()

                .graphId(
                        UUID.randomUUID().toString()
                )

                .name(
                        detectedFlow.getFlowType()
                )

                .sourceType("DETECTED_FLOW")

                .baseUrl(
                        detectedFlow.getPageUrl()
                )

                .nodes(nodes)

                .build();
    }
}