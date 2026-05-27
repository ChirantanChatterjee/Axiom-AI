package com.axiomai.workspace.repository;

import com.axiomai.workspace.entity.WorkspaceChatSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkspaceChatSessionRepository
        extends JpaRepository<WorkspaceChatSessionEntity, String> {

    List<WorkspaceChatSessionEntity> findByUserIdOrderByUpdatedAtDesc(
            Long userId
    );
}
