package com.axiomai.ml;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MLModelTrainingResult {

    private String modelName;

    private String version;

    private boolean trained;

    private int trainingExampleCount;

    private String modelPath;

    private String message;
}
