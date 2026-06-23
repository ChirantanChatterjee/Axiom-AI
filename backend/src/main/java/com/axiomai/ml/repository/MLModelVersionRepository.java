package com.axiomai.ml.repository;

import com.axiomai.ml.entity.MLModelVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MLModelVersionRepository
        extends JpaRepository<MLModelVersionEntity, Long> {

    Optional<MLModelVersionEntity> findTopByModelNameAndActiveTrueOrderByCreatedAtDesc(
            String modelName
    );

    Optional<MLModelVersionEntity> findTopByModelNameAndVersionOrderByCreatedAtDesc(
            String modelName,
            String version
    );

    List<MLModelVersionEntity> findByModelNameAndActiveTrue(
            String modelName
    );

    List<MLModelVersionEntity> findByModelNameOrderByCreatedAtDesc(
            String modelName
    );
}
