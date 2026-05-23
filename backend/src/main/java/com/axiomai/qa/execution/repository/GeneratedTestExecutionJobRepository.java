package com.axiomai.qa.execution.repository;

import com.axiomai.qa.execution.entity.GeneratedTestExecutionJobEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GeneratedTestExecutionJobRepository
        extends JpaRepository<GeneratedTestExecutionJobEntity, String> {

    Optional<GeneratedTestExecutionJobEntity> findTopBySessionIdOrderByCreatedAtDesc(
            String sessionId
    );

    List<GeneratedTestExecutionJobEntity> findTop10BySessionIdOrderByCreatedAtDesc(
            String sessionId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select job
            from GeneratedTestExecutionJobEntity job
            where job.status = 'QUEUED'
            order by job.createdAt asc
            """)
    List<GeneratedTestExecutionJobEntity> findQueuedJobsForUpdate(
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select job
            from GeneratedTestExecutionJobEntity job
            where job.status = 'RUNNING'
              and job.updatedAt < :cutoff
            order by job.updatedAt asc
            """)
    List<GeneratedTestExecutionJobEntity> findStaleRunningJobsForUpdate(
            @Param("cutoff") Instant cutoff
    );

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Transactional
    @Query("""
            delete from GeneratedTestExecutionJobEntity job
            where job.sessionId = :sessionId
               or job.userId = :sessionId
            """)
    int deleteForWorkspaceSession(
            @Param("sessionId") String sessionId
    );
}
