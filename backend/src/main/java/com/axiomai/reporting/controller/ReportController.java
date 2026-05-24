package com.axiomai.reporting.controller;

import com.axiomai.audit.AuditLogService;
import com.axiomai.reporting.service.ReportArtifactService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportArtifactService
            reportArtifactService;

    private AuditLogService
            auditLogService;

    @Autowired(required = false)
    public void setAuditLogService(
            AuditLogService auditLogService
    ) {

        this.auditLogService =
                auditLogService;
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<?> openReport(
            @PathVariable String fileName
    ) throws MalformedURLException {

        Path root =
                Paths.get("reports")
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

            return reportArtifactService.find(fileName)
                    .map(artifact -> {
                        auditReportAccess(
                                fileName,
                                "artifact_store"
                        );

                        return ResponseEntity.ok()
                            .contentType(
                                    MediaType.parseMediaType(
                                            artifact.getContentType()
                                    )
                            )
                            .body(artifact.getContent());
                    })
                    .orElseGet(
                            () -> {
                                auditReportMissing(fileName);

                                return ResponseEntity
                                        .notFound()
                                        .build();
                            }
                    );
        }

        Resource resource =
                new UrlResource(
                        target.toUri()
                );

        auditReportAccess(
                fileName,
                "local_filesystem"
        );

        return ResponseEntity.ok()

                .contentType(
                        MediaType.TEXT_HTML
                )

                .body(resource);
    }

    private void auditReportAccess(
            String fileName,
            String source
    ) {

        if (
                auditLogService == null
        ) {

            return;
        }

        auditLogService.recordSuccess(
                null,
                null,
                "report.access",
                "REPORT",
                fileName,
                Map.of("source", source)
        );
    }

    private void auditReportMissing(
            String fileName
    ) {

        if (
                auditLogService == null
        ) {

            return;
        }

        auditLogService.recordFailure(
                null,
                null,
                "report.access",
                "REPORT",
                fileName,
                Map.of("reason", "not_found")
        );
    }
}
