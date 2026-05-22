package com.axiomai.qa.execution.controller;

import com.axiomai.qa.execution.service.GeneratedTestExecutionJobDto;
import com.axiomai.qa.execution.service.GeneratedTestExecutionQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/generated-test-executions")
@CrossOrigin("*")
@RequiredArgsConstructor
public class GeneratedTestExecutionJobController {

    private final GeneratedTestExecutionQueueService
            queueService;

    @GetMapping("/{jobId}")
    public ResponseEntity<GeneratedTestExecutionJobDto> getJob(
            @PathVariable String jobId
    ) {

        return queueService.find(jobId)
                .map(queueService::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(
                        () -> ResponseEntity
                                .notFound()
                                .build()
                );
    }

    @GetMapping("/session/{sessionId}/latest")
    public ResponseEntity<GeneratedTestExecutionJobDto> getLatestForSession(
            @PathVariable String sessionId
    ) {

        return queueService.findLatestForSession(sessionId)
                .map(queueService::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(
                        () -> ResponseEntity
                                .notFound()
                                .build()
                );
    }

    @GetMapping("/session/{sessionId}")
    public List<GeneratedTestExecutionJobDto> getRecentForSession(
            @PathVariable String sessionId
    ) {

        return queueService.findRecentForSession(sessionId)
                .stream()
                .map(queueService::toDto)
                .toList();
    }
}
