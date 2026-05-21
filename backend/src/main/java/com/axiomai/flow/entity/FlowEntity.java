package com.axiomai.flow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "flows")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class FlowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =====================================================
    // FLOW NAME
    // =====================================================

    private String flowName;

    // =====================================================
    // DOMAIN NAME
    // =====================================================

    @Column(length = 2000)
    private String domainName;

    // =====================================================
    // BASE URL
    // =====================================================

    @Column(length = 5000)
    private String baseUrl;

    // =====================================================
    // DESCRIPTION
    // =====================================================

    @Column(length = 5000)
    private String description;

    // =====================================================
    // PROJECT
    // =====================================================

    private Long projectId;

    // =====================================================
    // CREATED
    // =====================================================

    private LocalDateTime createdAt;
}