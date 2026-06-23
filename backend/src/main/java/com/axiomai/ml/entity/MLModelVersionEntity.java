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
@Table(name = "ml_model_versions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLModelVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String modelName;

    @Column(nullable = false, length = 128)
    private String version;

    @Column(nullable = false, length = 2048)
    private String storagePath;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean rolledBack;

    private Integer trainingExampleCount;

    private Double modelConfidence;

    @Column(length = 64)
    private String status;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant activatedAt;

    private Instant retiredAt;
}
