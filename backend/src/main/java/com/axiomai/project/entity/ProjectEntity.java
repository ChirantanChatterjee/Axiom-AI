package com.axiomai.project.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "projects")

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String projectName;

    private String baseUrl;

    private String description;
}