package com.axiomai.ai.runtime;

import com.axiomai.ai.execution.PlannedAction;
import com.axiomai.qa.models.FlowStep;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component

public class SemanticActionMapper {

    // =====================================================
    // MAP TO FLOW STEP
    // =====================================================

    public FlowStep map(
            PlannedAction action
    ) {

        FlowStep step =
                new FlowStep();

        step.setAction(
                action.getActionType()
        );

        step.setTarget(
                action.getSemanticTarget()
        );

        step.setSemanticDescription(
                action.getDescription()
        );

        step.setFallbackSelectors(
                new ArrayList<>()
        );

        step.setSelector(null);

        return step;
    }

    // =====================================================
    // MAP ALL
    // =====================================================

    public List<FlowStep> mapAll(
            List<PlannedAction> actions
    ) {

        List<FlowStep> steps =
                new ArrayList<>();

        for (PlannedAction action : actions) {

            steps.add(
                    map(action)
            );
        }

        return steps;
    }
}