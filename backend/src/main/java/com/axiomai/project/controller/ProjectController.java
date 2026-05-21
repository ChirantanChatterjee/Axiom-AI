package com.axiomai.project.controller;

import com.axiomai.project.dto.CreateProjectRequest;
import com.axiomai.project.entity.ProjectEntity;
import com.axiomai.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")

@RequiredArgsConstructor
@CrossOrigin("*")

public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ProjectEntity createProject(
            @Valid @RequestBody
            CreateProjectRequest request
    ) {

        return projectService.createProject(request);
    }

    @GetMapping
    public List<ProjectEntity> getAllProjects() {

        return projectService.getAllProjects();
    }
}