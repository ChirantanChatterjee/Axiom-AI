package com.axiomai.workspace.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "generated_framework_archives")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedFrameworkArchiveEntity {

    @Id
    @Column(nullable = false, length = 128)
    private String sessionId;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] archive;

    @Column(nullable = false, length = 255)
    private String archiveName;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private Instant updatedAt;
}
