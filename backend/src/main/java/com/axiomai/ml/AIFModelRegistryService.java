package com.axiomai.ml;

import com.axiomai.ml.config.AIFMLProperties;
import com.axiomai.ml.entity.MLModelVersionEntity;
import com.axiomai.ml.repository.MLModelVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AIFModelRegistryService {

    private final MLModelVersionRepository modelVersionRepository;

    private final AIFMLProperties properties;

    public Optional<MLModelVersionEntity> activeVersion(
            String modelName
    ) {

        return modelVersionRepository
                .findTopByModelNameAndActiveTrueOrderByCreatedAtDesc(
                        modelName
                );
    }

    public List<MLModelVersionEntity> versions(
            String modelName
    ) {

        return modelVersionRepository
                .findByModelNameOrderByCreatedAtDesc(
                        modelName
                );
    }

    public String nextVersion() {

        return "v"
                + Instant.now()
                        .toEpochMilli();
    }

    public Path modelPath(
            String modelName,
            String version
    ) {

        return Path.of(
                        properties.getModelStoragePath()
                )
                .resolve(modelName)
                .resolve(version + ".model");
    }

    public Path datasetPath(
            String modelName,
            String version
    ) {

        return Path.of(
                        properties.getModelStoragePath()
                )
                .resolve("datasets")
                .resolve(modelName + "-" + version + ".jsonl");
    }

    @Transactional
    public MLModelVersionEntity activateVersion(
            String modelName,
            String version,
            String storagePath,
            int trainingExampleCount,
            String notes
    ) {

        Instant now =
                Instant.now();

        for (
                MLModelVersionEntity active
                : modelVersionRepository.findByModelNameAndActiveTrue(
                        modelName
                )
        ) {

            active.setActive(false);
            active.setRetiredAt(now);

            modelVersionRepository.save(active);
        }

        return modelVersionRepository.save(
                MLModelVersionEntity.builder()
                        .modelName(modelName)
                        .version(version)
                        .storagePath(storagePath)
                        .active(true)
                        .rolledBack(false)
                        .trainingExampleCount(trainingExampleCount)
                        .modelConfidence(null)
                        .status("ACTIVE")
                        .notes(notes)
                        .createdAt(now)
                        .activatedAt(now)
                        .build()
        );
    }

    @Transactional
    public Optional<MLModelVersionEntity> rollback(
            String modelName,
            String version
    ) {

        Optional<MLModelVersionEntity> target =
                modelVersionRepository
                        .findTopByModelNameAndVersionOrderByCreatedAtDesc(
                                modelName,
                                version
                        );

        if (
                target.isEmpty()
        ) {

            return Optional.empty();
        }

        Instant now =
                Instant.now();

        for (
                MLModelVersionEntity active
                : modelVersionRepository.findByModelNameAndActiveTrue(
                        modelName
                )
        ) {

            active.setActive(false);
            active.setRolledBack(true);
            active.setRetiredAt(now);

            modelVersionRepository.save(active);
        }

        MLModelVersionEntity entity =
                target.get();

        entity.setActive(true);
        entity.setRolledBack(false);
        entity.setStatus("ACTIVE_ROLLBACK");
        entity.setActivatedAt(now);
        entity.setRetiredAt(null);

        return Optional.of(
                modelVersionRepository.save(entity)
        );
    }
}
