package com.axiomai.workspace;

import com.axiomai.qa.flow.DetectedFlow;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service

public class AutomationWorkspaceService {

    // =====================================================
    // IN MEMORY WORKSPACES
    // =====================================================

    private final Map<String, AutomationSession>
            sessions =
            new HashMap<>();

    // =====================================================
    // GET OR CREATE SESSION
    // =====================================================

    public AutomationSession getOrCreateSession(
            String userId
    ) {

        if (sessions.containsKey(userId)) {

            return sessions.get(userId);
        }

        AutomationSession session =

                AutomationSession.builder()

                        .sessionId(
                                UUID.randomUUID().toString()
                        )

                        .userId(userId)

                        .createdAt(
                                LocalDateTime.now()
                        )

                        .updatedAt(
                                LocalDateTime.now()
                        )

                        .build();

        sessions.put(
                userId,
                session
        );

        return session;
    }

    // =====================================================
    // WEBSITE
    // =====================================================

    public void setWebsite(

            String userId,
            String website

    ) {

        AutomationSession session =
                getOrCreateSession(userId);

        session.setWebsiteUrl(website);

        session.setUpdatedAt(
                LocalDateTime.now()
        );
    }

    // =====================================================
    // DOMAIN
    // =====================================================

    public void setDomain(

            String userId,
            String domain

    ) {

        AutomationSession session =
                getOrCreateSession(userId);

        session.setDomainName(domain);

        session.setUpdatedAt(
                LocalDateTime.now()
        );
    }

    // =====================================================
    // FLOWS
    // =====================================================

    public void storeFlows(

            String userId,
            List<DetectedFlow> flows

    ) {

        AutomationSession session =
                getOrCreateSession(userId);

        session.setDetectedFlows(flows);

        session.setUpdatedAt(
                LocalDateTime.now()
        );
    }

    // =====================================================
    // ACTIVE FLOW
    // =====================================================

    public void setActiveFlow(

            String userId,
            String flowName

    ) {

        AutomationSession session =
                getOrCreateSession(userId);

        session.setActiveFlowName(flowName);

        session.setUpdatedAt(
                LocalDateTime.now()
        );
    }

    // =====================================================
    // GENERATED FEATURE
    // =====================================================

    public void storeFeature(

            String userId,

            String featureName,

            String content

    ) {

        AutomationSession session =
                getOrCreateSession(userId);

        session.getGeneratedFeatures()
                .put(
                        featureName,
                        content
                );

        session.setActiveFeature(featureName);

        session.setUpdatedAt(
                LocalDateTime.now()
        );
    }

    // =====================================================
    // VARIABLES
    // =====================================================

    public void putVariable(

            String userId,

            String key,

            String value,

            boolean sensitive

    ) {

        AutomationSession session =
                getOrCreateSession(userId);

        WorkspaceVariable variable =

                WorkspaceVariable.builder()

                        .key(key)

                        .value(value)

                        .sensitive(sensitive)

                        .source("CHAT")

                        .build();

        session.getVariables()
                .put(
                        key.toLowerCase(),
                        variable
                );

        session.setUpdatedAt(
                LocalDateTime.now()
        );
    }

    // =====================================================
    // GET VARIABLE
    // =====================================================

    public String getVariable(

            String userId,
            String key

    ) {

        AutomationSession session =
                getOrCreateSession(userId);

        WorkspaceVariable variable =

                session.getVariables()
                        .get(
                                key.toLowerCase()
                        );

        if (variable == null) {

            return null;
        }

        return variable.getValue();
    }

    // =====================================================
    // ARTIFACTS
    // =====================================================

    public void addArtifact(

            String userId,
            GeneratedArtifact artifact

    ) {

        AutomationSession session =
                getOrCreateSession(userId);

        session.getArtifacts()
                .add(artifact);

        session.setUpdatedAt(
                LocalDateTime.now()
        );
    }

    // =====================================================
    // REPORT
    // =====================================================

    public void setLatestReport(

            String userId,
            String reportPath

    ) {

        AutomationSession session =
                getOrCreateSession(userId);

        session.setLatestReportPath(
                reportPath
        );

        session.setUpdatedAt(
                LocalDateTime.now()
        );
    }

    // =====================================================
    // EXECUTION HISTORY
    // =====================================================

    public void addExecutionHistory(

            String userId,
            String entry

    ) {

        AutomationSession session =
                getOrCreateSession(userId);

        session.getExecutionHistory()
                .add(entry);

        session.setUpdatedAt(
                LocalDateTime.now()
        );
    }

    // =====================================================
    // GET SESSION
    // =====================================================

    public AutomationSession getSession(
            String userId
    ) {

        return getOrCreateSession(userId);
    }
}