package com.axiomai.workspace.repository;

import com.axiomai.workspace.entity.WorkspaceSessionVariableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Transactional
    @Query("""
            delete from WorkspaceSessionVariableEntity variable
            where variable.sessionId = :sessionId
            """)
    int deleteBySessionId(
            @Param("sessionId") String sessionId
    );
}
