package com.axiomai.ml;

import com.axiomai.ml.config.AIFMLProperties;
import com.axiomai.ml.entity.MLTrainingExampleEntity;
import com.axiomai.ml.repository.MLTrainingExampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AIFModelTrainingService {

    private final MLTrainingExampleRepository trainingExampleRepository;

    private final AIFModelRegistryService modelRegistryService;

    private final MLTrainingDatasetExporter datasetExporter;

    private final AIFMLProperties properties;

    private final List<AIFTextClassificationModelService> modelServices;

    public MLModelTrainingResult retrainModel(
            String modelName
    ) {

        if (
                !properties.isEnabled()
                        ||
                        !properties.getTraining()
                                .isEnabled()
        ) {

            return MLModelTrainingResult.builder()
                    .modelName(modelName)
                    .trained(false)
                    .message("AIF ML training is disabled.")
                    .build();
        }

        AIFTextClassificationModelService modelService =
                modelService(modelName);

        if (
                modelService == null
        ) {

            return MLModelTrainingResult.builder()
                    .modelName(modelName)
                    .trained(false)
                    .message("Unknown AIF ML model.")
                    .build();
        }

        List<MLTrainingExampleEntity> examples =
                trainingExampleRepository
                        .findByModelNameAndEligibleForTrainingTrueAndFinalAcceptedLabelIsNotNullOrderByCreatedAtAsc(
                                modelName,
                                PageRequest.of(
                                        0,
                                        Math.max(
                                                1,
                                                properties.getMaxTrainingExamples()
                                        )
                                )
                        );

        String version =
                modelRegistryService.nextVersion();

        Path modelPath =
                modelRegistryService.modelPath(
                        modelName,
                        version
                );

        datasetExporter.exportJsonl(
                examples,
                modelRegistryService.datasetPath(
                        modelName,
                        version
                )
        );

        MLModelTrainingResult result =
                modelService.train(
                        examples,
                        version,
                        modelPath
                );

        if (
                result.isTrained()
        ) {

            modelRegistryService.activateVersion(
                    modelName,
                    version,
                    modelPath.toString(),
                    result.getTrainingExampleCount(),
                    result.getMessage()
            );
        }

        return result;
    }

    public Map<String, MLModelTrainingResult> retrainAllModels() {

        Map<String, MLModelTrainingResult> results =
                new LinkedHashMap<>();

        for (
                AIFTextClassificationModelService modelService
                : modelServices
        ) {

            results.put(
                    modelService.modelName(),
                    retrainModel(
                            modelService.modelName()
                    )
            );
        }

        return results;
    }

    public void retrainModelsOverThreshold() {

        if (
                !properties.isEnabled()
                        ||
                        !properties.getTraining()
                                .isEnabled()
        ) {

            return;
        }

        for (
                AIFTextClassificationModelService modelService
                : modelServices
        ) {

            long count =
                    trainingExampleRepository
                            .countByModelNameAndEligibleForTrainingTrueAndFinalAcceptedLabelIsNotNull(
                                    modelService.modelName()
                            );

            if (
                    count >= properties.getRetrainThreshold()
            ) {

                retrainModel(
                        modelService.modelName()
                );
            }
        }
    }

    @Scheduled(fixedDelayString = "${aif.ml.retrain-interval-ms:300000}")
    public void scheduledRetraining() {

        retrainModelsOverThreshold();
    }

    public AIFTextClassificationModelService modelService(
            String modelName
    ) {

        if (
                modelName == null
        ) {

            return null;
        }

        return modelServices.stream()
                .filter(service ->
                        service.modelName()
                                .equalsIgnoreCase(
                                        modelName
                                )
                )
                .findFirst()
                .orElse(null);
    }
}
