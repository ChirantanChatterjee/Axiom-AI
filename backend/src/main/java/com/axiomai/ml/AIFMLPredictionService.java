package com.axiomai.ml;

import com.axiomai.ml.config.AIFMLProperties;
import com.axiomai.ml.entity.MLPredictionLogEntity;
import com.axiomai.ml.repository.MLPredictionLogRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AIFMLPredictionService {

    private final IntentClassificationModelService intentClassificationModelService;

    private final FailureClassificationModelService failureClassificationModelService;

    private final RepairRecommendationModelService repairRecommendationModelService;

    private final LocatorRecoveryModelService locatorRecoveryModelService;

    private final AIFTrainingDataService trainingDataService;

    private final MLPredictionLogRepository predictionLogRepository;

    private final MLFeatureExtractor featureExtractor;

    private final AIFMLProperties properties;

    @PostConstruct
    public void registerLocatorRankingHooks() {

        com.axiomai.runtime.locator.SmartLocatorResolver
                .setMlSelectorRanker(this::rankLocatorSelectors);
        com.axiomai.runtime.locator.SmartLocatorResolver
                .setMlLocatorSuccessRecorder(this::recordSuccessfulLocatorRecovery);

        com.axiomai.qa.runtime.SmartLocatorResolver
                .setMlSelectorRanker(this::rankLocatorSelectors);
        com.axiomai.qa.runtime.SmartLocatorResolver
                .setMlLocatorSuccessRecorder(this::recordSuccessfulLocatorRecovery);

        com.axiomai.qa.runtime.SelectorFallbackEngine
                .setMlSelectorRanker(this::rankLocatorSelectors);
        com.axiomai.qa.runtime.SelectorFallbackEngine
                .setMlLocatorSuccessRecorder(this::recordSuccessfulLocatorRecovery);
    }

    public MLPrediction predictIntent(
            String command
    ) {

        return logPrediction(
                intentClassificationModelService.predict(command),
                command,
                MLSourceType.USER_CHAT,
                Map.of()
        );
    }

    public MLPrediction classifyFailure(
            String failureContext
    ) {

        return logPrediction(
                failureClassificationModelService.predict(
                        failureContext
                ),
                failureContext,
                MLSourceType.FAILED_TEST,
                Map.of()
        );
    }

    public MLPrediction recommendRepair(
            String failureContext
    ) {

        return logPrediction(
                repairRecommendationModelService.predict(
                        failureContext
                ),
                failureContext,
                MLSourceType.REPAIR_RESULT,
                Map.of()
        );
    }

    public MLPrediction predictLocatorRecovery(
            String locatorContext
    ) {

        return logPrediction(
                locatorRecoveryModelService.predict(
                        locatorContext
                ),
                locatorContext,
                MLSourceType.LOCATOR_RECOVERY,
                Map.of()
        );
    }

    public AIFRepairMLContext analyzeRepairContext(
            String failureContext
    ) {

        MLPrediction failurePrediction =
                classifyFailure(failureContext);

        String repairInput =
                failureContext
                        + System.lineSeparator()
                        + "Predicted failure type: "
                        + failurePrediction.getPredictedLabel();

        MLPrediction repairPrediction =
                recommendRepair(repairInput);

        List<AIFTrainingDataService.SimilarRepairExample> similarRepairs =
                trainingDataService.similarRepairExamples(
                        failureContext,
                        5
                );

        return AIFRepairMLContext.builder()
                .failurePrediction(failurePrediction)
                .repairPrediction(repairPrediction)
                .similarRepairs(similarRepairs)
                .build();
    }

    public List<String> rankLocatorSelectors(
            List<String> selectors
    ) {

        if (
                selectors == null
                        ||
                        selectors.size() <= 1
                        ||
                        !properties.isEnabled()
        ) {

            return selectors;
        }

        String context =
                String.join(
                        System.lineSeparator(),
                        selectors
                );

        MLPrediction prediction =
                predictLocatorRecovery(context);

        if (
                !prediction.isHighConfidence()
        ) {

            return selectors;
        }

        return locatorRecoveryModelService.rankSelectors(
                selectors,
                prediction
        );
    }

    public void recordSuccessfulLocatorRecovery(
            String resolvedSelector,
            List<String> candidates
    ) {

        if (
                resolvedSelector == null
                        ||
                        resolvedSelector.isBlank()
                        ||
                        candidates == null
                        ||
                        candidates.isEmpty()
                        ||
                        !properties.isEnabled()
        ) {

            return;
        }

        String context =
                "resolvedSelector="
                        + resolvedSelector
                        + System.lineSeparator()
                        + "candidates="
                        + String.join(
                        System.lineSeparator(),
                        candidates
                );

        MLPrediction prediction =
                predictLocatorRecovery(context);

        trainingDataService.recordLocatorRecovery(
                context,
                prediction,
                locatorRecoveryModelService.finalStrategyForSelector(
                        resolvedSelector
                ),
                true,
                Map.of(
                        "resolvedSelector",
                        resolvedSelector,
                        "candidateCount",
                        candidates.size()
                )
        );
    }

    @Transactional
    public MLPrediction logPrediction(
            MLPrediction prediction,
            String input,
            MLSourceType sourceType,
            Map<String, ?> metadata
    ) {

        if (
                prediction == null
                        ||
                        !properties.isEnabled()
        ) {

            return prediction;
        }

        try {

            MLPredictionLogEntity saved =
                    predictionLogRepository.save(
                            MLPredictionLogEntity.builder()
                                    .modelName(
                                            prediction.getModelName()
                                    )
                                    .modelVersion(
                                            prediction.getModelVersion()
                                    )
                                    .inputHash(
                                            featureExtractor.inputHash(
                                                    input
                                            )
                                    )
                                    .predictedLabel(
                                            prediction.getPredictedLabel()
                                    )
                                    .confidence(
                                            prediction.getConfidence()
                                    )
                                    .openAiFallbackUsed(false)
                                    .sourceType(
                                            sourceType == null
                                                    ? null
                                                    : sourceType.name()
                                    )
                                    .metadataJson(
                                            featureExtractor.metadataJson(
                                                    metadata
                                            )
                                    )
                                    .createdAt(Instant.now())
                                    .build()
                    );

            prediction.setPredictionLogId(
                    saved.getId()
            );

        } catch (RuntimeException ignored) {

            return prediction;
        }

        return prediction;
    }

    @Transactional
    public void completePrediction(
            MLPrediction prediction,
            boolean openAiFallbackUsed,
            String finalAcceptedLabel
    ) {

        if (
                prediction == null
                        ||
                        prediction.getPredictionLogId() == null
        ) {

            return;
        }

        predictionLogRepository.findById(
                        prediction.getPredictionLogId()
                )
                .ifPresent(log -> {

                    log.setOpenAiFallbackUsed(
                            openAiFallbackUsed
                    );
                    log.setFinalAcceptedLabel(
                            finalAcceptedLabel
                    );

                    predictionLogRepository.save(log);
                });
    }

    public Map<String, Object> predictionSummary(
            MLPrediction prediction
    ) {

        Map<String, Object> summary =
                new LinkedHashMap<>();

        if (
                prediction == null
        ) {

            return summary;
        }

        summary.put("modelName", prediction.getModelName());
        summary.put("modelVersion", prediction.getModelVersion());
        summary.put("predictedLabel", prediction.getPredictedLabel());
        summary.put("confidence", prediction.getConfidence());
        summary.put("highConfidence", prediction.isHighConfidence());
        summary.put("predictionMode", prediction.getPredictionMode());

        return summary;
    }
}
