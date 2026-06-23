package com.axiomai.ml;

import com.axiomai.ml.config.AIFMLProperties;
import com.axiomai.ml.entity.MLTrainingExampleEntity;
import com.axiomai.ml.repository.MLTrainingExampleRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AIFTrainingDataService {

    private final MLTrainingExampleRepository trainingExampleRepository;

    private final MLFeatureExtractor featureExtractor;

    private final AIFMLProperties properties;

    @Transactional
    public Optional<MLTrainingExampleEntity> recordTrainingExample(
            String modelName,
            String inputText,
            String predictedLabel,
            String finalAcceptedLabel,
            Double confidence,
            MLSourceType sourceType,
            Map<String, ?> metadata,
            boolean validatedOutcome
    ) {

        if (
                !properties.isEnabled()
                        ||
                        !properties.getTraining()
                                .isEnabled()
                        ||
                        !validatedOutcome
                        ||
                        isBlank(modelName)
                        ||
                        isBlank(finalAcceptedLabel)
                        ||
                        isBlank(inputText)
        ) {

            return Optional.empty();
        }

        MLTrainingExampleEntity saved =
                trainingExampleRepository.save(
                        MLTrainingExampleEntity.builder()
                                .modelName(modelName)
                                .inputText(
                                        featureExtractor.normalizeInput(
                                                inputText
                                        )
                                )
                                .predictedLabel(
                                        blankToNull(predictedLabel)
                                )
                                .finalAcceptedLabel(
                                        finalAcceptedLabel
                                                .trim()
                                                .toUpperCase()
                                )
                                .confidence(confidence)
                                .sourceType(
                                        sourceType == null
                                                ? MLSourceType.EXECUTION_HISTORY.name()
                                                : sourceType.name()
                                )
                                .metadataJson(
                                        featureExtractor.metadataJson(
                                                metadata
                                        )
                                )
                                .eligibleForTraining(true)
                                .createdAt(Instant.now())
                                .build()
                );

        return Optional.of(saved);
    }

    public boolean recordIntentOutcome(
            String userCommand,
            String predictedLabel,
            String finalIntent,
            Double confidence,
            boolean successful,
            Map<String, ?> metadata
    ) {

        String acceptedLabel =
                toIntentTrainingLabel(finalIntent);

        if (
                acceptedLabel == null
        ) {

            return false;
        }

        return recordTrainingExample(
                AIFMLModelNames.INTENT_CLASSIFICATION,
                userCommand,
                predictedLabel,
                acceptedLabel,
                confidence,
                MLSourceType.USER_CHAT,
                metadata,
                successful
        )
                .isPresent();
    }

    public boolean recordRepairOutcome(
            String failureContext,
            AIFRepairMLContext mlContext,
            String finalRepairStrategy,
            boolean successfulRepair,
            Map<String, ?> metadata
    ) {

        if (
                !successfulRepair
                        ||
                        mlContext == null
        ) {

            return false;
        }

        boolean saved =
                false;

        MLPrediction failurePrediction =
                mlContext.getFailurePrediction();

        if (
                failurePrediction != null
                        &&
                        failurePrediction.getPredictedLabel() != null
        ) {

            saved =
                    recordTrainingExample(
                    AIFMLModelNames.FAILURE_CLASSIFICATION,
                    failureContext,
                    failurePrediction.getPredictedLabel(),
                    failurePrediction.getPredictedLabel(),
                    failurePrediction.getConfidence(),
                    MLSourceType.FAILED_TEST,
                    metadata,
                    true
            )
                            .isPresent()
                    ||
                    saved;
        }

        MLPrediction repairPrediction =
                mlContext.getRepairPrediction();

        saved =
                recordTrainingExample(
                AIFMLModelNames.REPAIR_RECOMMENDATION,
                failureContext,
                repairPrediction == null
                        ? null
                        : repairPrediction.getPredictedLabel(),
                finalRepairStrategy,
                repairPrediction == null
                        ? null
                        : repairPrediction.getConfidence(),
                MLSourceType.REPAIR_RESULT,
                metadata,
                true
        )
                        .isPresent()
                ||
                saved;

        return saved;
    }

    public void recordLocatorRecovery(
            String locatorContext,
            MLPrediction prediction,
            String finalStrategy,
            boolean successfulRecovery,
            Map<String, ?> metadata
    ) {

        recordTrainingExample(
                AIFMLModelNames.LOCATOR_RECOVERY,
                locatorContext,
                prediction == null
                        ? null
                        : prediction.getPredictedLabel(),
                finalStrategy,
                prediction == null
                        ? null
                        : prediction.getConfidence(),
                MLSourceType.LOCATOR_RECOVERY,
                metadata,
                successfulRecovery
        );
    }

    public List<SimilarRepairExample> similarRepairExamples(
            String failureContext,
            int limit
    ) {

        if (
                isBlank(failureContext)
                        ||
                        limit <= 0
        ) {

            return List.of();
        }

        return trainingExampleRepository
                .findTop200ByModelNameAndEligibleForTrainingTrueAndFinalAcceptedLabelIsNotNullOrderByCreatedAtDesc(
                        AIFMLModelNames.REPAIR_RECOMMENDATION
                )
                .stream()
                .map(example -> SimilarRepairExample.builder()
                        .id(example.getId())
                        .finalRepairStrategy(
                                example.getFinalAcceptedLabel()
                        )
                        .metadataJson(
                                example.getMetadataJson()
                        )
                        .createdAt(
                                example.getCreatedAt()
                        )
                        .similarity(
                                featureExtractor.tokenOverlap(
                                        failureContext,
                                        example.getInputText()
                                )
                        )
                        .build()
                )
                .filter(example -> example.getSimilarity() > 0.0)
                .sorted(
                        Comparator.comparingDouble(
                                        SimilarRepairExample::getSimilarity
                                )
                                .reversed()
                )
                .limit(limit)
                .toList();
    }

    private String toIntentTrainingLabel(
            String intent
    ) {

        if (
                isBlank(intent)
        ) {

            return IntentClassificationLabel.UNKNOWN.name();
        }

        String normalized =
                intent.trim()
                        .toUpperCase();

        return switch (normalized) {
            case "GENERATE_FRAMEWORK",
                 "GENERATE_FEATURE" ->
                    IntentClassificationLabel.GENERATE_FRAMEWORK.name();
            case "EXECUTE_FLOW",
                 "EXECUTE_FEATURE",
                 "EXECUTE_GENERATED_TESTS",
                 "AI_EXECUTION" ->
                    IntentClassificationLabel.EXECUTE_FLOW.name();
            case "SHOW_REPORT" ->
                    IntentClassificationLabel.SHOW_REPORT.name();
            case "SHOW_DB" ->
                    IntentClassificationLabel.SHOW_DB.name();
            case "REPAIR_GENERATED_TESTS" ->
                    IntentClassificationLabel.REPAIR_TEST.name();
            default ->
                    IntentClassificationLabel.UNKNOWN.name();
        };
    }

    private boolean isBlank(
            String value
    ) {

        return value == null
                ||
                value.isBlank();
    }

    private String blankToNull(
            String value
    ) {

        return isBlank(value)
                ? null
                : value.trim()
                .toUpperCase();
    }

    @Getter
    @Builder
    public static class SimilarRepairExample {

        private Long id;

        private String finalRepairStrategy;

        private String metadataJson;

        private Instant createdAt;

        private double similarity;

        public Map<String, Object> promptSummary() {

            Map<String, Object> summary =
                    new LinkedHashMap<>();

            summary.put("id", id);
            summary.put("finalRepairStrategy", finalRepairStrategy);
            summary.put("similarity", similarity);
            summary.put("metadataJson", metadataJson);
            summary.put("createdAt", createdAt);

            return summary;
        }
    }
}
