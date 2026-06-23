package com.axiomai.ml;

import com.axiomai.ml.config.AIFMLProperties;
import com.axiomai.ml.entity.MLModelVersionEntity;
import com.axiomai.ml.entity.MLTrainingExampleEntity;
import lombok.RequiredArgsConstructor;
import weka.classifiers.bayes.NaiveBayesMultinomial;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RequiredArgsConstructor
public abstract class AbstractWekaTextClassificationModelService<L extends Enum<L>>
        implements AIFTextClassificationModelService {

    protected final AIFModelRegistryService modelRegistryService;

    protected final MLFeatureExtractor featureExtractor;

    protected final AIFMLProperties properties;

    protected final MLConfidenceDecisionService confidenceDecisionService;

    protected abstract Class<L> labelClass();

    protected abstract L defaultLabel();

    protected abstract MLPrediction heuristicPrediction(
            String input
    );

    @Override
    public List<String> supportedLabels() {

        return Arrays.stream(
                        labelClass().getEnumConstants()
                )
                .map(Enum::name)
                .toList();
    }

    @Override
    public MLPrediction predict(
            String input
    ) {

        if (
                !properties.isEnabled()
        ) {

            return MLPrediction.unavailable(
                    modelName(),
                    defaultLabel().name()
            );
        }

        try {

            MLModelVersionEntity version =
                    modelRegistryService.activeVersion(
                            modelName()
                    )
                    .orElse(null);

            if (
                    version == null
                            ||
                            version.getStoragePath() == null
                            ||
                            !Files.exists(
                                    Path.of(
                                            version.getStoragePath()
                                    )
                            )
            ) {

                return withHighConfidenceFlag(
                        heuristicPrediction(input)
                );
            }

            WekaTextModelArtifact artifact =
                    (WekaTextModelArtifact) SerializationHelper.read(
                            version.getStoragePath()
                    );

            MLPrediction prediction =
                    predictWithArtifact(
                            artifact,
                            input
                    );

            prediction.setModelVersion(
                    version.getVersion()
            );

            prediction.setModelAvailable(true);
            prediction.setPredictionMode("weka");

            return withHighConfidenceFlag(prediction);

        } catch (Exception e) {

            MLPrediction prediction =
                    heuristicPrediction(input);

            prediction.getMetadata()
                    .put(
                            "wekaFallbackReason",
                            e.getClass()
                                    .getSimpleName()
                    );

            return withHighConfidenceFlag(prediction);
        }
    }

    @Override
    public MLModelTrainingResult train(
            List<MLTrainingExampleEntity> examples,
            String version,
            Path modelPath
    ) {

        List<MLTrainingExampleEntity> usable =
                examples == null
                        ? List.of()
                        : examples.stream()
                        .filter(this::hasValidAcceptedLabel)
                        .toList();

        if (
                usable.isEmpty()
        ) {

            return MLModelTrainingResult.builder()
                    .modelName(modelName())
                    .version(version)
                    .trained(false)
                    .trainingExampleCount(0)
                    .modelPath(
                            modelPath == null
                                    ? null
                                    : modelPath.toString()
                    )
                    .message("No eligible training examples are available.")
                    .build();
        }

        try {

            Files.createDirectories(
                    modelPath.getParent()
            );

            Instances trainingData =
                    trainingInstances(usable);

            StringToWordVector vectorizer =
                    new StringToWordVector();

            vectorizer.setLowerCaseTokens(true);
            vectorizer.setWordsToKeep(3_000);
            vectorizer.setTFTransform(true);
            vectorizer.setIDFTransform(true);

            FilteredClassifier classifier =
                    new FilteredClassifier();

            classifier.setFilter(vectorizer);
            classifier.setClassifier(
                    new NaiveBayesMultinomial()
            );
            classifier.buildClassifier(trainingData);

            WekaTextModelArtifact artifact =
                    new WekaTextModelArtifact(
                            modelName(),
                            version,
                            supportedLabels(),
                            new Instances(
                                    trainingData,
                                    0
                            ),
                            classifier,
                            usable.size(),
                            Instant.now()
                    );

            SerializationHelper.write(
                    modelPath.toString(),
                    artifact
            );

            return MLModelTrainingResult.builder()
                    .modelName(modelName())
                    .version(version)
                    .trained(true)
                    .trainingExampleCount(usable.size())
                    .modelPath(
                            modelPath.toString()
                    )
                    .message("Trained Weka text classification model.")
                    .build();

        } catch (Exception e) {

            return MLModelTrainingResult.builder()
                    .modelName(modelName())
                    .version(version)
                    .trained(false)
                    .trainingExampleCount(usable.size())
                    .modelPath(
                            modelPath == null
                                    ? null
                                    : modelPath.toString()
                    )
                    .message(
                            "Training failed: "
                                    + e.getMessage()
                    )
                    .build();
        }
    }

    protected MLPrediction prediction(
            L label,
            double confidence,
            String mode
    ) {

        return MLPrediction.builder()
                .modelName(modelName())
                .predictedLabel(label.name())
                .confidence(
                        clampConfidence(confidence)
                )
                .modelAvailable(
                        "weka".equals(mode)
                )
                .predictionMode(mode)
                .metadata(
                        new LinkedHashMap<>()
                )
                .build();
    }

    protected String lower(
            String input
    ) {

        return featureExtractor.normalizeInput(input)
                .toLowerCase(Locale.ROOT);
    }

    protected boolean containsAny(
            String input,
            String... needles
    ) {

        String lower =
                lower(input);

        for (
                String needle
                : needles
        ) {

            if (
                    lower.contains(
                            needle.toLowerCase(Locale.ROOT)
                    )
            ) {

                return true;
            }
        }

        return false;
    }

    private MLPrediction predictWithArtifact(
            WekaTextModelArtifact artifact,
            String input
    ) throws Exception {

        Instances header =
                new Instances(
                        artifact.getHeader(),
                        0
                );

        Instance instance =
                new DenseInstance(
                        header.numAttributes()
                );

        instance.setDataset(header);
        instance.setValue(
                header.attribute("text"),
                featureExtractor.normalizeInput(input)
        );

        double[] distribution =
                artifact.getClassifier()
                        .distributionForInstance(instance);

        int index =
                maxIndex(distribution);

        String label =
                header.classAttribute()
                        .value(index);

        L predicted =
                parseLabel(label);

        return prediction(
                predicted == null
                        ? defaultLabel()
                        : predicted,
                distribution.length == 0
                        ? 0.0
                        : distribution[index],
                "weka"
        );
    }

    private Instances trainingInstances(
            List<MLTrainingExampleEntity> examples
    ) {

        ArrayList<Attribute> attributes =
                new ArrayList<>();

        attributes.add(
                new Attribute(
                        "text",
                        (List<String>) null
                )
        );

        attributes.add(
                new Attribute(
                        "label",
                        supportedLabels()
                )
        );

        Instances data =
                new Instances(
                        modelName() + "_training",
                        attributes,
                        examples.size()
                );

        data.setClassIndex(1);

        for (
                MLTrainingExampleEntity example
                : examples
        ) {

            Instance instance =
                    new DenseInstance(
                            data.numAttributes()
                    );

            instance.setDataset(data);
            instance.setValue(
                    data.attribute("text"),
                    featureExtractor.normalizeInput(
                            example.getInputText()
                    )
            );
            instance.setClassValue(
                    parseLabel(
                            example.getFinalAcceptedLabel()
                    )
                            .name()
            );

            data.add(instance);
        }

        return data;
    }

    private MLPrediction withHighConfidenceFlag(
            MLPrediction prediction
    ) {

        prediction.setHighConfidence(
                confidenceDecisionService.isHighConfidence(
                        prediction
                )
        );

        return prediction;
    }

    private boolean hasValidAcceptedLabel(
            MLTrainingExampleEntity example
    ) {

        return example != null
                &&
                example.isEligibleForTraining()
                &&
                example.getFinalAcceptedLabel() != null
                &&
                parseLabel(
                        example.getFinalAcceptedLabel()
                ) != null;
    }

    private L parseLabel(
            String label
    ) {

        if (
                label == null
                        ||
                        label.isBlank()
        ) {

            return null;
        }

        for (
                L candidate
                : labelClass().getEnumConstants()
        ) {

            if (
                    candidate.name()
                            .equalsIgnoreCase(
                                    label.trim()
                            )
            ) {

                return candidate;
            }
        }

        return null;
    }

    private int maxIndex(
            double[] values
    ) {

        if (
                values == null
                        ||
                        values.length == 0
        ) {

            return 0;
        }

        return java.util.stream.IntStream.range(
                        0,
                        values.length
                )
                .boxed()
                .max(
                        Comparator.comparingDouble(
                                index -> values[index]
                        )
                )
                .orElse(0);
    }

    private double clampConfidence(
            double confidence
    ) {

        if (
                Double.isNaN(confidence)
                        ||
                        Double.isInfinite(confidence)
        ) {

            return 0.0;
        }

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        confidence
                )
        );
    }
}
