package com.axiomai.ml;

import lombok.AllArgsConstructor;
import lombok.Getter;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instances;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Getter
@AllArgsConstructor
public class WekaTextModelArtifact implements Serializable {

    private static final long serialVersionUID =
            1L;

    private String modelName;

    private String version;

    private List<String> labels;

    private Instances header;

    private FilteredClassifier classifier;

    private int trainingExampleCount;

    private Instant trainedAt;
}
