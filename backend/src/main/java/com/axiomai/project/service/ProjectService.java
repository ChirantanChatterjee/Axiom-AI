package com.axiomai.project.service;

import com.axiomai.project.dto.CreateProjectRequest;
import com.axiomai.project.entity.ProjectEntity;
import com.axiomai.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor

public class ProjectService {

    private final ProjectRepository repository;

    public ProjectEntity createProject(
            CreateProjectRequest request
    ) {

        ProjectEntity entity =
                ProjectEntity.builder()
                        .projectName(request.getProjectName())
                        .baseUrl(request.getBaseUrl())
                        .description(request.getDescription())
                        .build();

        return repository.save(entity);
    }

    public List<ProjectEntity> getAllProjects() {

        return repository.findAll();
    }
}