package com.axiomai.ml;

import com.axiomai.ml.config.AIFMLProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MLConfidenceDecisionService {

    private final AIFMLProperties properties;

    public boolean isHighConfidence(
            MLPrediction prediction
    ) {

        return prediction != null
                &&
                prediction.getConfidence()
                        >= properties.getMinConfidence();
    }

    public boolean shouldFallbackToOpenAI(
            MLPrediction prediction
    ) {

        return properties.isFallbackToOpenai()
                &&
                !isHighConfidence(prediction);
    }
}
