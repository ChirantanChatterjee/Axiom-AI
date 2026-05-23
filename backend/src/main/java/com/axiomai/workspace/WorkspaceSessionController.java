package com.axiomai.workspace;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspace/sessions")
@RequiredArgsConstructor
public class WorkspaceSessionController {

    private final WorkspaceCleanupService
            workspaceCleanupService;

    private final WorkspaceAccessService
            workspaceAccessService;

    @DeleteMapping("/{sessionId}")
    public WorkspaceCleanupResult deleteSession(
            @PathVariable String sessionId,
            @RequestHeader("X-AIF-Session") String token
    ) {

        String normalizedSessionId =
                workspaceAccessService.requireAccess(
                        token,
                        sessionId
                );

        WorkspaceCleanupResult result =
                workspaceCleanupService.cleanup(
                        normalizedSessionId
                );

        workspaceAccessService.deleteOwnership(
                normalizedSessionId
        );

        return result;
    }
}
