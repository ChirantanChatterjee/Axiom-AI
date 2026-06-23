package com.axiomai.ml;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLPrediction {

    private Long predictionLogId;

    private String modelName;

    private String modelVersion;

    private String predictedLabel;

    private double confidence;

    private boolean highConfidence;

    private boolean modelAvailable;

    private String predictionMode;

    @Builder.Default
    private Map<String, Object> metadata =
            new LinkedHashMap<>();

    public static MLPrediction unavailable(
            String modelName,
            String label
    ) {

        return MLPrediction.builder()
                .modelName(modelName)
                .predictedLabel(label)
                .confidence(0.0)
                .highConfidence(false)
                .modelAvailable(false)
                .predictionMode("disabled")
                .build();
    }
}
