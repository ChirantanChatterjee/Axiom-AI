package com.axiomai.core.runtime;

import com.axiomai.core.graph.ActionNode;
import com.axiomai.qa.models.FlowStep;
import org.springframework.stereotype.Component;

@Component

public class LocatorResolverFacade {

    // =====================================================
    // RESOLVE ACTION NODE
    // =====================================================

    public FlowStep resolve(
            ActionNode node
    ) {

        FlowStep step =
                new FlowStep();

        step.setAction(
                node.getActionType()
        );

        step.setTarget(
                node.getSemanticTarget()
        );

        step.setSelector(
                node.getPrimaryLocator()
        );

        step.setFallbackSelectors(
                node.getFallbackLocators()
        );

        step.setBusinessRole(
                node.getBusinessRole()
        );

        step.setConfidenceScore(
                node.getConfidenceScore() != null
                        ? node.getConfidenceScore()
                        : 0.0
        );

        step.setSemanticDescription(
                node.getDescription()
        );

        return step;
    }
}