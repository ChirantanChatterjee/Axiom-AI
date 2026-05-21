package com.axiomai.execution.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "flow_executions")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class FlowExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long flowId;

    private String status;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Column(length = 5000)
    private String errorMessage;

}