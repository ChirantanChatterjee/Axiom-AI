package com.axiomai.audit.repository;

import com.axiomai.audit.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository
        extends JpaRepository<AuditLogEntity, Long> {
}
