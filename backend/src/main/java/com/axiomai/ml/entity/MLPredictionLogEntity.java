package com.axiomai.ml.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "ml_prediction_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLPredictionLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String modelName;

    @Column(length = 128)
    private String modelVersion;

    @Column(nullable = false, length = 128)
    private String inputHash;

    @Column(length = 128)
    private String predictedLabel;

    private Double confidence;

    @Column(nullable = false)
    private boolean openAiFallbackUsed;

    @Column(length = 128)
    private String finalAcceptedLabel;

    @Column(length = 64)
    private String sourceType;

    @Column(columnDefinition = "text")
    private String metadataJson;

    @Column(nullable = false)
    private Instant createdAt;
}
