package com.axiomai.reporting.repository;

import com.axiomai.reporting.entity.ReportArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportArtifactRepository
        extends JpaRepository<ReportArtifactEntity, String> {
}
