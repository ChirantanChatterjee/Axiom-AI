package com.axiomai.project.repository;

import com.axiomai.project.entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProjectRepository
        extends JpaRepository<ProjectEntity, UUID> {
}