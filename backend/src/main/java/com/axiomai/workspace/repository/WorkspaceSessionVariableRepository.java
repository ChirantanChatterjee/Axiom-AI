package com.axiomai.workspace.repository;

import com.axiomai.workspace.entity.WorkspaceSessionVariableEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceSessionVariableRepository
        extends JpaRepository<WorkspaceSessionVariableEntity, Long> {

    List<WorkspaceSessionVariableEntity> findBySessionId(
            String sessionId
    );

    Optional<WorkspaceSessionVariableEntity> findBySessionIdAndVariableKey(
            String sessionId,
            String variableKey
    );

    void deleteBySessionId(
            String sessionId
    );
}
