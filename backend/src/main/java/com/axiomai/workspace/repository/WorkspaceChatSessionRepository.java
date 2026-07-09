package com.axiomai.workspace.repository;

import com.axiomai.workspace.entity.WorkspaceChatSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkspaceChatSessionRepository
        extends JpaRepository<WorkspaceChatSessionEntity, String> {

    @Query("""
            select session
            from WorkspaceChatSessionEntity session
            where session.userId = :userId
              and (session.deleted is null or session.deleted = false)
            order by session.updatedAt desc
            """)
    List<WorkspaceChatSessionEntity> findActiveByUserIdOrderByUpdatedAtDesc(
            @Param("userId") Long userId
    );
}
