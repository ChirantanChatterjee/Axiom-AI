package com.axiomai.workspace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "workspace_session_variables",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_workspace_session_variables_session_key",
                columnNames = {
                        "session_id",
                        "variable_key"
                }
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceSessionVariableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 128)
    private String sessionId;

    @Column(name = "variable_key", nullable = false, length = 128)
    private String variableKey;

    @Column(name = "variable_value", columnDefinition = "text")
    private String variableValue;

    @Column(nullable = false)
    private boolean sensitive;

    @Column(nullable = false, length = 64)
    private String source;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
