package com.axiomai.qa.execution.controller;

import com.axiomai.qa.execution.service.GeneratedTestExecutionJobDto;
import com.axiomai.qa.execution.service.GeneratedTestExecutionQueueService;
import com.axiomai.workspace.WorkspaceAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/generated-test-executions")
@RequiredArgsConstructor
public class GeneratedTestExecutionJobController {

    private final GeneratedTestExecutionQueueService
            queueService;

    private final WorkspaceAccessService
            workspaceAccessService;

    @GetMapping("/{jobId}")
    public ResponseEntity<GeneratedTestExecutionJobDto> getJob(
            @PathVariable String jobId,
            @RequestHeader("X-AIF-Session") String token
    ) {

        return queueService.find(jobId)
                .map(job -> {
                    workspaceAccessService.requireAccess(
                            token,
                            job.getSessionId()
                    );

                    return queueService.toDto(job);
                })
                .map(ResponseEntity::ok)
                .orElseGet(
                        () -> ResponseEntity
                                .notFound()
                                .build()
                );
    }

    @GetMapping("/session/{sessionId}/latest")
    public ResponseEntity<GeneratedTestExecutionJobDto> getLatestForSession(
            @PathVariable String sessionId,
            @RequestHeader("X-AIF-Session") String token
    ) {

        String normalizedSessionId =
                workspaceAccessService.requireAccess(
                        token,
                        sessionId
                );

        return queueService.findLatestForSession(normalizedSessionId)
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
            @PathVariable String sessionId,
            @RequestHeader("X-AIF-Session") String token
    ) {

        String normalizedSessionId =
                workspaceAccessService.requireAccess(
                        token,
                        sessionId
                );

        return queueService.findRecentForSession(normalizedSessionId)
                .stream()
                .map(queueService::toDto)
                .toList();
    }
}
