package com.axiomai.core.memory;

import com.axiomai.core.graph.FlowGraph;
import com.axiomai.core.session.ExecutionSession;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service

public class ExecutionMemoryService {

    // =====================================================
    // SESSION STORE
    // =====================================================

    private final Map<String, ExecutionSession>
            sessions = new ConcurrentHashMap<>();

    // =====================================================
    // GET OR CREATE
    // =====================================================

    public ExecutionSession getOrCreateSession(
            String userId
    ) {

        return sessions.computeIfAbsent(

                userId,

                id -> ExecutionSession.builder()

                        .sessionId(
                                UUID.randomUUID().toString()
                        )

                        .userId(userId)

                        .status("ACTIVE")

                        .createdAt(
                                LocalDateTime.now()
                        )

                        .updatedAt(
                                LocalDateTime.now()
                        )

                        .build()
        );
    }

    // =====================================================
    // STORE FLOW GRAPH
    // =====================================================

    public void storeFlowGraph(

            String userId,

            FlowGraph flowGraph

    ) {

        ExecutionSession session =
                getOrCreateSession(userId);

        session.setCurrentFlowGraph(
                flowGraph
        );

        session.setActiveFlowName(
                flowGraph.getName()
        );

        session.setActiveUrl(
                flowGraph.getBaseUrl()
        );

        session.setUpdatedAt(
                LocalDateTime.now()
        );
    }

    // =====================================================
    // UPDATE LAST INTENT
    // =====================================================

    public void updateIntent(

            String userId,

            String intent

    ) {

        ExecutionSession session =
                getOrCreateSession(userId);

        session.setLastIntent(intent);

        session.setUpdatedAt(
                LocalDateTime.now()
        );
    }

    // =====================================================
    // STORE REPORT
    // =====================================================

    public void storeReport(

            String userId,

            String reportPath

    ) {

        ExecutionSession session =
                getOrCreateSession(userId);

        session.setLastReportPath(
                reportPath
        );

        session.setUpdatedAt(
                LocalDateTime.now()
        );
    }

    // =====================================================
    // GET ACTIVE FLOW
    // =====================================================

    public FlowGraph getActiveFlowGraph(
            String userId
    ) {

        return getOrCreateSession(userId)
                .getCurrentFlowGraph();
    }

    // =====================================================
    // GET SESSION
    // =====================================================

    public ExecutionSession getSession(
            String userId
    ) {

        return getOrCreateSession(userId);
    }
}