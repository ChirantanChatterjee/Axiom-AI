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

                            .inputValue(
                                    buildInputPlaceholder(action)
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

    private String buildInputPlaceholder(
            PlannedAction action
    ) {

        if (
                action == null
                        ||
                        action.getActionType() == null
                        ||
                        !"TYPE".equalsIgnoreCase(
                                action.getActionType()
                        )
        ) {

            return null;
        }

        if (
                action.getVariableKey() != null
                        &&
                        !action.getVariableKey()
                                .isBlank()
        ) {

            return "${"
                    + action.getVariableKey()
                    + "}";
        }

        String target =
                action.getSemanticTarget() == null
                        ? ""
                        : action.getSemanticTarget()
                        .toLowerCase();

        if (
                target.contains("user")
                        ||
                        target.contains("email")
                        ||
                        target.contains("auth")
        ) {

            return "${username}";
        }

        if (
                target.contains("password")
                        ||
                        target.contains("pass")
        ) {

            return "${password}";
        }

        if (
                target.contains("search")
        ) {

            return "${search}";
        }

        return null;
    }
}
