package com.axiomai.ml.repository;

import com.axiomai.ml.entity.MLPredictionLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MLPredictionLogRepository
        extends JpaRepository<MLPredictionLogEntity, Long> {
}
