package com.axiomai.workspace;

import com.axiomai.qa.flow.DetectedFlow;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service

public class AutomationWorkspaceService {

    // =====================================================
    // IN MEMORY WORKSPACES
    // =====================================================

    private final Map<String, AutomationSession>
            sessions =
            new ConcurrentHashMap<>();

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
                                sessionIdFor(userId)
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

    private String sessionIdFor(
            String userId
    ) {

        if (
                userId == null
                        ||
                        userId.isBlank()
        ) {

            return UUID.randomUUID()
                    .toString();
        }

        return userId.trim()
                .replaceAll(
                        "[^A-Za-z0-9._-]",
                        "-"
                );
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

    public void putVariables(

            String userId,

            Map<String, String> variables

    ) {

        if (
                variables == null
                        ||
                        variables.isEmpty()
        ) {

            return;
        }

        for (
                Map.Entry<String, String> entry
                : variables.entrySet()
        ) {

            putVariable(
                    userId,
                    entry.getKey(),
                    entry.getValue(),
                    isSensitive(entry.getKey())
            );
        }
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

    public Map<String, WorkspaceVariable> getVariables(
            String userId
    ) {

        return getOrCreateSession(userId)
                .getVariables();
    }

    public Map<String, String> getVariableValues(
            String userId
    ) {

        Map<String, String> values =
                new HashMap<>();

        for (
                WorkspaceVariable variable
                : getVariables(userId).values()
        ) {

            values.put(
                    variable.getKey()
                            .toLowerCase(),
                    variable.getValue()
            );
        }

        return values;
    }

    // =====================================================
    // FIND FLOW
    // =====================================================

    public DetectedFlow findFlow(

            String userId,

            String requestedName

    ) {

        AutomationSession session =
                getOrCreateSession(userId);

        if (
                session.getDetectedFlows() == null
                        ||
                        session.getDetectedFlows()
                                .isEmpty()
        ) {

            return null;
        }

        if (
                requestedName != null
                        &&
                        !requestedName.isBlank()
        ) {

            String target =
                    normalizeFlowName(requestedName);

            for (
                    DetectedFlow flow
                    : session.getDetectedFlows()
            ) {

                String flowType =
                        flow.getFlowType() == null
                                ? ""
                                : normalizeFlowName(
                                        flow.getFlowType()
                                );

                if (
                        !flowType.isBlank()
                                &&
                                (
                                        flowType.contains(target)
                                                ||
                                                target.contains(flowType)
                                )
                ) {

                    return flow;
                }
            }
        }

        return session.getDetectedFlows()
                .get(0);
    }

    private String normalizeFlowName(
            String value
    ) {

        return value == null
                ? ""
                : value.toLowerCase()
                        .replaceAll(
                                "[^a-z0-9]+",
                                ""
                        );
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

    public GeneratedArtifact getLatestArtifact(

            String userId,

            String type

    ) {

        AutomationSession session =
                getOrCreateSession(userId);

        List<GeneratedArtifact> artifacts =
                session.getArtifacts();

        for (
                int i = artifacts.size() - 1;
                i >= 0;
                i--
        ) {

            GeneratedArtifact artifact =
                    artifacts.get(i);

            if (
                    type == null
                            ||
                            type.equalsIgnoreCase(
                                    artifact.getType()
                            )
            ) {

                return artifact;
            }
        }

        return null;
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

    private boolean isSensitive(
            String key
    ) {

        if (
                key == null
        ) {

            return false;
        }

        String lower =
                key.toLowerCase();

        return lower.contains("password")
                ||
                lower.contains("token")
                ||
                lower.contains("secret")
                ||
                lower.contains("otp");
    }
}
