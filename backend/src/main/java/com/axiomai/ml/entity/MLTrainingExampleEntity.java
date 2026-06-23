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
@Table(name = "ml_training_examples")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLTrainingExampleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String modelName;

    @Column(columnDefinition = "text")
    private String inputText;

    @Column(length = 128)
    private String predictedLabel;

    @Column(length = 128)
    private String finalAcceptedLabel;

    private Double confidence;

    @Column(nullable = false, length = 64)
    private String sourceType;

    @Column(columnDefinition = "text")
    private String metadataJson;

    @Column(nullable = false)
    private boolean eligibleForTraining;

    @Column(length = 128)
    private String usedInModelVersion;

    @Column(nullable = false)
    private Instant createdAt;
}
