package com.axiomai.ml;

import com.axiomai.ml.entity.MLTrainingExampleEntity;

import java.nio.file.Path;
import java.util.List;

public interface AIFTextClassificationModelService {

    String modelName();

    List<String> supportedLabels();

    MLPrediction predict(
            String input
    );

    MLModelTrainingResult train(
            List<MLTrainingExampleEntity> examples,
            String version,
            Path modelPath
    );
}
