package com.axiomai.workspace;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspace/sessions")
@RequiredArgsConstructor
@CrossOrigin("*")
public class WorkspaceSessionController {

    private final WorkspaceCleanupService
            workspaceCleanupService;

    @DeleteMapping("/{sessionId}")
    public WorkspaceCleanupResult deleteSession(
            @PathVariable String sessionId
    ) {

        return workspaceCleanupService.cleanup(
                sessionId
        );
    }
}
