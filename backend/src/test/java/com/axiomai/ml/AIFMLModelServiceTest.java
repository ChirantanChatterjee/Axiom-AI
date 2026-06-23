package com.axiomai.ml;

import com.axiomai.ml.config.AIFMLProperties;
import com.axiomai.ml.entity.MLModelVersionEntity;
import com.axiomai.ml.entity.MLTrainingExampleEntity;
import com.axiomai.ml.repository.MLModelVersionRepository;
import com.axiomai.ml.repository.MLTrainingExampleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIFMLModelServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void heuristicFailureClassifierIdentifiesLocatorFailure() {

        FailureClassificationModelService service =
                new FailureClassificationModelService(
                        registryWithNoActiveModel(),
                        featureExtractor(),
                        properties(),
                        confidenceDecisionService()
                );

        MLPrediction prediction =
                service.predict(
                        "Runtime failed: Unable to resolve element: login button. No valid locator found."
                );

        assertEquals(
                FailureClassificationLabel.LOCATOR_FAILURE.name(),
                prediction.getPredictedLabel()
        );

        assertTrue(
                prediction.isHighConfidence()
        );
    }

    @Test
    void trainingDataIsRedactedBeforePersistence() {

        List<MLTrainingExampleEntity> savedExamples =
                new ArrayList<>();

        MLTrainingExampleRepository repository =
                trainingRepository(
                        List.of(),
                        savedExamples
                );

        AIFTrainingDataService service =
                new AIFTrainingDataService(
                        repository,
                        featureExtractor(),
                        properties()
                );

        Optional<MLTrainingExampleEntity> saved =
                service.recordTrainingExample(
                        AIFMLModelNames.INTENT_CLASSIFICATION,
                        "generate framework where email is user@example.com and password is secret123 Authorization: Bearer abc.def",
                        null,
                        IntentClassificationLabel.GENERATE_FRAMEWORK.name(),
                        null,
                        MLSourceType.USER_CHAT,
                        java.util.Map.of(
                                "apiKey",
                                "sk-real",
                                "note",
                                "safe"
                        ),
                        true
                );

        assertTrue(
                saved.isPresent()
        );

        assertFalse(
                saved.get()
                        .getInputText()
                        .contains("user@example.com")
        );

        assertFalse(
                saved.get()
                        .getInputText()
                        .contains("secret123")
        );

        assertFalse(
                saved.get()
                        .getInputText()
                        .contains("abc.def")
        );

        assertFalse(
                saved.get()
                        .getMetadataJson()
                        .contains("sk-real")
        );
    }

    @Test
    void retrainModelWritesWekaModelFileAndActivatesVersion() {

        AIFMLProperties properties =
                properties();

        properties.setModelStoragePath(
                tempDir.toString()
        );

        MLTrainingExampleRepository trainingRepository =
                trainingRepository(
                        List.of(
                        example(
                                "generate a framework for example.com",
                                IntentClassificationLabel.GENERATE_FRAMEWORK.name()
                        ),
                        example(
                                "run the generated tests",
                                IntentClassificationLabel.EXECUTE_FLOW.name()
                        ),
                        example(
                                "show latest report",
                                IntentClassificationLabel.SHOW_REPORT.name()
                        )
                ),
                        new ArrayList<>()
                );

        MLModelVersionRepository versionRepository =
                versionRepository(
                        new ArrayList<>()
                );

        AIFModelRegistryService registryService =
                new AIFModelRegistryService(
                        versionRepository,
                        properties
                );

        MLFeatureExtractor featureExtractor =
                new MLFeatureExtractor(
                        new SecretRedactionService(),
                        properties
                );

        IntentClassificationModelService intentModel =
                new IntentClassificationModelService(
                        registryService,
                        featureExtractor,
                        properties,
                        new MLConfidenceDecisionService(properties)
                );

        AIFModelTrainingService trainingService =
                new AIFModelTrainingService(
                        trainingRepository,
                        registryService,
                        new MLTrainingDatasetExporter(),
                        properties,
                        List.of(intentModel)
                );

        MLModelTrainingResult result =
                trainingService.retrainModel(
                        AIFMLModelNames.INTENT_CLASSIFICATION
                );

        assertTrue(
                result.isTrained()
        );

        assertEquals(
                3,
                result.getTrainingExampleCount()
        );

        assertTrue(
                Files.exists(
                        Path.of(
                                result.getModelPath()
                        )
                )
        );
    }

    private AIFModelRegistryService registryWithNoActiveModel() {

        return new AIFModelRegistryService(
                versionRepository(
                        new ArrayList<>()
                ),
                properties()
        );
    }

    private MLFeatureExtractor featureExtractor() {

        return new MLFeatureExtractor(
                new SecretRedactionService(),
                properties()
        );
    }

    private MLConfidenceDecisionService confidenceDecisionService() {

        return new MLConfidenceDecisionService(
                properties()
        );
    }

    private AIFMLProperties properties() {

        AIFMLProperties properties =
                new AIFMLProperties();

        properties.setModelStoragePath(
                tempDir == null
                        ? "./target/aif-model-test"
                        : tempDir.toString()
        );

        return properties;
    }

    private MLTrainingExampleEntity example(
            String input,
            String label
    ) {

        return MLTrainingExampleEntity.builder()
                .modelName(
                        AIFMLModelNames.INTENT_CLASSIFICATION
                )
                .inputText(input)
                .finalAcceptedLabel(label)
                .sourceType(
                        MLSourceType.USER_CHAT.name()
                )
                .eligibleForTraining(true)
                .createdAt(Instant.now())
                .build();
    }

    private MLTrainingExampleRepository trainingRepository(
            List<MLTrainingExampleEntity> trainingExamples,
            List<MLTrainingExampleEntity> savedExamples
    ) {

        return (MLTrainingExampleRepository) Proxy.newProxyInstance(
                MLTrainingExampleRepository.class.getClassLoader(),
                new Class<?>[]{
                        MLTrainingExampleRepository.class
                },
                (proxy, method, args) -> {

                    return switch (method.getName()) {
                        case "save" -> {
                            MLTrainingExampleEntity entity =
                                    (MLTrainingExampleEntity) args[0];

                            if (
                                    entity.getId() == null
                            ) {

                                entity.setId(
                                        (long) savedExamples.size() + 1
                                );
                            }

                            savedExamples.add(entity);

                            yield entity;
                        }
                        case "findByModelNameAndEligibleForTrainingTrueAndFinalAcceptedLabelIsNotNullOrderByCreatedAtAsc" ->
                                trainingExamples;
                        case "findTop200ByModelNameAndEligibleForTrainingTrueAndFinalAcceptedLabelIsNotNullOrderByCreatedAtDesc" ->
                                trainingExamples.stream()
                                        .sorted(
                                                Comparator.comparing(
                                                                MLTrainingExampleEntity::getCreatedAt
                                                        )
                                                        .reversed()
                                        )
                                        .limit(200)
                                        .toList();
                        case "countByModelNameAndEligibleForTrainingTrueAndFinalAcceptedLabelIsNotNull" ->
                                (long) trainingExamples.size();
                        default ->
                                defaultValue(
                                        method.getReturnType()
                                );
                    };
                }
        );
    }

    private MLModelVersionRepository versionRepository(
            List<MLModelVersionEntity> versions
    ) {

        return (MLModelVersionRepository) Proxy.newProxyInstance(
                MLModelVersionRepository.class.getClassLoader(),
                new Class<?>[]{
                        MLModelVersionRepository.class
                },
                (proxy, method, args) -> {

                    return switch (method.getName()) {
                        case "save" -> {
                            MLModelVersionEntity entity =
                                    (MLModelVersionEntity) args[0];

                            if (
                                    entity.getId() == null
                            ) {

                                entity.setId(
                                        (long) versions.size() + 1
                                );

                                versions.add(entity);
                            }

                            yield entity;
                        }
                        case "findTopByModelNameAndActiveTrueOrderByCreatedAtDesc" ->
                                versions.stream()
                                        .filter(MLModelVersionEntity::isActive)
                                        .filter(version ->
                                                version.getModelName()
                                                        .equals(args[0])
                                        )
                                        .findFirst();
                        case "findByModelNameAndActiveTrue" ->
                                versions.stream()
                                        .filter(MLModelVersionEntity::isActive)
                                        .filter(version ->
                                                version.getModelName()
                                                        .equals(args[0])
                                        )
                                        .toList();
                        case "findTopByModelNameAndVersionOrderByCreatedAtDesc" ->
                                versions.stream()
                                        .filter(version ->
                                                version.getModelName()
                                                        .equals(args[0])
                                                        &&
                                                        version.getVersion()
                                                                .equals(args[1])
                                        )
                                        .findFirst();
                        case "findByModelNameOrderByCreatedAtDesc" ->
                                versions.stream()
                                        .filter(version ->
                                                version.getModelName()
                                                        .equals(args[0])
                                        )
                                        .sorted(
                                                Comparator.comparing(
                                                                MLModelVersionEntity::getCreatedAt
                                                        )
                                                        .reversed()
                                        )
                                        .toList();
                        default ->
                                defaultValue(
                                        method.getReturnType()
                                );
                    };
                }
        );
    }

    private Object defaultValue(
            Class<?> returnType
    ) {

        if (
                returnType.equals(Boolean.TYPE)
        ) {

            return false;
        }

        if (
                returnType.equals(Long.TYPE)
        ) {

            return 0L;
        }

        if (
                returnType.equals(Integer.TYPE)
        ) {

            return 0;
        }

        if (
                Optional.class.equals(returnType)
        ) {

            return Optional.empty();
        }

        if (
                List.class.equals(returnType)
        ) {

            return List.of();
        }

        return null;
    }
}
