package com.axiomai.ml.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "aif.ml")
public class AIFMLProperties {

    private boolean enabled =
            true;

    private Training training =
            new Training();

    private double minConfidence =
            0.75;

    private int retrainThreshold =
            100;

    private String modelStoragePath =
            "./aif-models";

    private boolean fallbackToOpenai =
            true;

    private long retrainIntervalMs =
            300_000;

    private int maxTrainingExamples =
            5_000;

    private int maxStoredInputChars =
            60_000;

    @Getter
    @Setter
    public static class Training {

        private boolean enabled =
                true;
    }
}
