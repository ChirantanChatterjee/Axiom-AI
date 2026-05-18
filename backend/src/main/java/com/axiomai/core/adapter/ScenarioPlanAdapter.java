package com.axiomai.core.adapter;

import com.axiomai.ai.execution.PlannedAction;
import com.axiomai.ai.execution.ScenarioPlan;
import com.axiomai.core.graph.ActionNode;
import com.axiomai.core.graph.FlowGraph;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component

public class ScenarioPlanAdapter {

    public FlowGraph convert(
            ScenarioPlan scenarioPlan
    ) {

        List<ActionNode> nodes =
                new ArrayList<>();

        for (PlannedAction action : scenarioPlan.getActions()) {

            ActionNode node =

                    ActionNode.builder()

                            .nodeId(
                                    UUID.randomUUID().toString()
                            )

                            .actionType(
                                    action.getActionType()
                            )

                            .semanticTarget(
                                    action.getSemanticTarget()
                            )

                            .description(
                                    action.getDescription()
                            )

                            .metadata(
                                    action.getMetadata()
                            )

                            .build();

            nodes.add(node);
        }

        return FlowGraph.builder()

                .graphId(
                        UUID.randomUUID().toString()
                )

                .name(
                        scenarioPlan.getScenarioName()
                )

                .description(
                        scenarioPlan.getDescription()
                )

                .sourceType("AI_SCENARIO")

                .nodes(nodes)

                .build();
    }
}