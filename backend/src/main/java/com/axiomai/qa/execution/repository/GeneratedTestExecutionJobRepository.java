package com.axiomai.qa.execution.repository;

import com.axiomai.qa.execution.entity.GeneratedTestExecutionJobEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

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
}
