package com.axiomai.workspace;

import com.axiomai.audit.AuditLogService;
import com.axiomai.qa.service.GeneratedProjectWriterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/workspace/artifacts")
@RequiredArgsConstructor
public class WorkspaceArtifactController {

    private final GeneratedProjectWriterService
            generatedProjectWriterService;

    private final WorkspaceAccessService
            workspaceAccessService;

    private AuditLogService
            auditLogService;

    @Autowired(required = false)
    public void setAuditLogService(
            AuditLogService auditLogService
    ) {

        this.auditLogService =
                auditLogService;
    }

    @GetMapping("/{sessionId}/{fileName:.+}")
    public ResponseEntity<Resource> download(

            @PathVariable String sessionId,

            @PathVariable String fileName,

            @RequestHeader("X-AIF-Session") String token

    ) throws MalformedURLException {

        String normalizedSessionId;

        try {

            normalizedSessionId =
                    workspaceAccessService.requireAccess(
                            token,
                            sessionId
                    );

        } catch (ResponseStatusException e) {

            auditArtifactDenied(
                    token,
                    sessionId,
                    fileName,
                    e.getStatusCode()
                            .value()
            );

            throw e;
        }

        Path root =
                generatedProjectWriterService
                        .getWorkspaceRoot(normalizedSessionId)
                        .toAbsolutePath()
                        .normalize();

        Path target =
                root.resolve(fileName)
                        .normalize();

        if (
                !target.startsWith(root)
                        ||
                        !Files.exists(target)
        ) {

            auditArtifactUnavailable(
                    token,
                    normalizedSessionId,
                    fileName,
                    target.startsWith(root)
                            ? "missing"
                            : "path_escape"
            );

            return ResponseEntity
                    .notFound()
                    .build();
        }

        Resource resource =
                new UrlResource(
                        target.toUri()
                );

        auditArtifactDownloaded(
                token,
                normalizedSessionId,
                fileName
        );

        return ResponseEntity.ok()

                .contentType(
                        MediaType.APPLICATION_OCTET_STREAM
                )

                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + target.getFileName()
                                + "\""
                )

                .body(resource);
    }

    private void auditArtifactDownloaded(
            String token,
            String sessionId,
            String fileName
    ) {

        if (
                auditLogService == null
        ) {

            return;
        }

        auditLogService.recordSuccess(
                safeCurrentUserId(token),
                sessionId,
                "artifact.download",
                "ARTIFACT",
                artifactId(sessionId, fileName),
                Map.of("fileName", fileName)
        );
    }

    private void auditArtifactDenied(
            String token,
            String sessionId,
            String fileName,
            int status
    ) {

        if (
                auditLogService == null
        ) {

            return;
        }

        auditLogService.recordDenied(
                safeCurrentUserId(token),
                sessionId,
                "artifact.download",
                "ARTIFACT",
                artifactId(sessionId, fileName),
                Map.of(
                        "fileName",
                        fileName,
                        "status",
                        status
                )
        );
    }

    private void auditArtifactUnavailable(
            String token,
            String sessionId,
            String fileName,
            String reason
    ) {

        if (
                auditLogService == null
        ) {

            return;
        }

        auditLogService.recordFailure(
                safeCurrentUserId(token),
                sessionId,
                "artifact.download",
                "ARTIFACT",
                artifactId(sessionId, fileName),
                Map.of(
                        "fileName",
                        fileName,
                        "reason",
                        reason
                )
        );
    }

    private String safeCurrentUserId(
            String token
    ) {

        try {

            return workspaceAccessService.currentUserId(token);

        } catch (RuntimeException e) {

            return null;
        }
    }

    private String artifactId(
            String sessionId,
            String fileName
    ) {

        return sessionId
                + "/"
                + fileName;
    }
}
