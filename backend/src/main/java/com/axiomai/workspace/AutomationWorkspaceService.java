package com.axiomai.workspace;

import com.axiomai.qa.flow.DetectedFlow;
import com.axiomai.workspace.entity.WorkspaceSessionVariableEntity;
import com.axiomai.workspace.repository.WorkspaceSessionVariableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service

public class AutomationWorkspaceService {

    private final WorkspaceSessionVariableRepository
            variableRepository;

    public AutomationWorkspaceService() {

        this(null);
    }

    @Autowired
    public AutomationWorkspaceService(
            WorkspaceSessionVariableRepository variableRepository
    ) {

        this.variableRepository =
                variableRepository;
    }

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

    public boolean hasSession(
            String userId
    ) {

        return userId != null
                &&
                !userId.isBlank()
                &&
                sessions.containsKey(
                        sessionIdFor(userId)
                );
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

        if (
                key == null
                        ||
                        key.isBlank()
        ) {

            return;
        }

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

        persistVariable(
                session.getSessionId(),
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

        if (
                key == null
                        ||
                        key.isBlank()
        ) {

            return null;
        }

        AutomationSession session =
                getOrCreateSession(userId);

        WorkspaceVariable variable =

                session.getVariables()
                        .get(
                                key.toLowerCase()
                        );

        if (variable == null) {

            hydrateVariables(
                    session
            );

            variable =
                    session.getVariables()
                            .get(
                                    key.toLowerCase()
                            );
        }

        if (variable == null) {

            return null;
        }

        return variable.getValue();
    }

    public Map<String, WorkspaceVariable> getVariables(
            String userId
    ) {

        AutomationSession session =
                getOrCreateSession(userId);

        hydrateVariables(
                session
        );

        return session.getVariables();
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

    @Transactional
    public AutomationSession removeSession(
            String userId
    ) {

        if (
                userId == null
                        ||
                        userId.isBlank()
        ) {

            return null;
        }

        AutomationSession removed =
                sessions.remove(userId);

        deletePersistedVariables(
                userId
        );

        return removed;
    }

    private void persistVariable(
            String sessionId,
            WorkspaceVariable variable
    ) {

        if (
                variableRepository == null
                        ||
                        sessionId == null
                        ||
                        sessionId.isBlank()
                        ||
                        variable == null
                        ||
                        variable.getKey() == null
                        ||
                        variable.getKey().isBlank()
        ) {

            return;
        }

        String key =
                variable.getKey()
                        .toLowerCase();

        Instant now =
                Instant.now();

        WorkspaceSessionVariableEntity entity =
                variableRepository.findBySessionIdAndVariableKey(
                                sessionId,
                                key
                        )
                        .orElseGet(
                                () -> WorkspaceSessionVariableEntity.builder()
                                        .sessionId(sessionId)
                                        .variableKey(key)
                                        .createdAt(now)
                                        .build()
                        );

        entity.setVariableValue(
                variable.getValue()
        );
        entity.setSensitive(
                variable.isSensitive()
        );
        entity.setSource(
                variable.getSource() == null
                        ? "CHAT"
                        : variable.getSource()
        );
        entity.setUpdatedAt(now);

        variableRepository.save(entity);
    }

    private void hydrateVariables(
            AutomationSession session
    ) {

        if (
                variableRepository == null
                        ||
                        session == null
                        ||
                        session.getSessionId() == null
                        ||
                        session.getSessionId()
                                .isBlank()
        ) {

            return;
        }

        for (
                WorkspaceSessionVariableEntity entity
                : variableRepository.findBySessionId(
                        session.getSessionId()
                )
        ) {

            if (
                    entity.getVariableKey() == null
                            ||
                            entity.getVariableKey()
                                    .isBlank()
            ) {

                continue;
            }

            session.getVariables()
                    .put(
                            entity.getVariableKey()
                                    .toLowerCase(),
                            WorkspaceVariable.builder()
                                    .key(entity.getVariableKey())
                                    .value(entity.getVariableValue())
                                    .sensitive(entity.isSensitive())
                                    .source(entity.getSource())
                                    .build()
                    );
        }
    }

    private void deletePersistedVariables(
            String sessionId
    ) {

        if (
                variableRepository == null
                        ||
                        sessionId == null
                        ||
                        sessionId.isBlank()
        ) {

            return;
        }

        variableRepository.deleteBySessionId(
                sessionId
        );
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
