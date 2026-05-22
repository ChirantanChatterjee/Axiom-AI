package com.axiomai.qa.controller;

import com.axiomai.qa.service.FrameworkModificationService;
import com.axiomai.workspace.WorkspaceAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/framework/session")
@RequiredArgsConstructor
public class FrameworkModificationController {

    private final FrameworkModificationService frameworkModificationService;

    private final WorkspaceAccessService workspaceAccessService;

    @PostMapping("/{sessionId}/upload")
    public FrameworkModificationService.UploadedFrameworkResult upload(

            @PathVariable String sessionId,

            @RequestParam("file") MultipartFile file,

            @RequestHeader("X-AIF-Session") String token

    ) {

        String normalizedSessionId =
                workspaceAccessService.bindToCurrentUser(
                        token,
                        sessionId
                );

        return frameworkModificationService.uploadModifiedFramework(
                normalizedSessionId,
                file
        );
    }
}
