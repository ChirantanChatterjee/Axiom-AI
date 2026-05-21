package com.axiomai.core.runtime;

import com.axiomai.core.graph.ActionNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component

public class RuntimeRecoveryManager {

    // =====================================================
    // HANDLE FAILURE
    // =====================================================

    public void handleFailure(

            UnifiedRuntimeContext context,

            ActionNode node,

            Exception e

    ) {

        String recoveryEvent =

                "Recovery triggered for node: "
                        + node.getNodeId()
                        + " | Action: "
                        + node.getActionType()
                        + " | Error: "
                        + e.getMessage();

        log.error(recoveryEvent);

        context.getRecoveryEvents()
                .add(recoveryEvent);
    }
}