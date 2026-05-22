package com.axiomai.workspace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] archive;

    @Column(nullable = false, length = 255)
    private String archiveName;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private Instant updatedAt;
}
