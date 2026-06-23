package com.axiomai.ml.repository;

import com.axiomai.ml.entity.MLTrainingExampleEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MLTrainingExampleRepository
        extends JpaRepository<MLTrainingExampleEntity, Long> {

    long countByModelNameAndEligibleForTrainingTrueAndFinalAcceptedLabelIsNotNull(
            String modelName
    );

    List<MLTrainingExampleEntity> findByModelNameAndEligibleForTrainingTrueAndFinalAcceptedLabelIsNotNullOrderByCreatedAtAsc(
            String modelName,
            Pageable pageable
    );

    List<MLTrainingExampleEntity> findTop200ByModelNameAndEligibleForTrainingTrueAndFinalAcceptedLabelIsNotNullOrderByCreatedAtDesc(
            String modelName
    );
}
