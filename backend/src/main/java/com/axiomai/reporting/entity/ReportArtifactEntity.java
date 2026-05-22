package com.axiomai.reporting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "report_artifacts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportArtifactEntity {

    @Id
    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 128)
    private String contentType;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
