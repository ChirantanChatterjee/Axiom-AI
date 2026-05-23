package com.axiomai.workspace;

import com.axiomai.qa.service.GeneratedProjectWriterService;
import lombok.RequiredArgsConstructor;
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

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/workspace/artifacts")
@RequiredArgsConstructor
public class WorkspaceArtifactController {

    private final GeneratedProjectWriterService
            generatedProjectWriterService;

    private final WorkspaceAccessService
            workspaceAccessService;

    @GetMapping("/{sessionId}/{fileName:.+}")
    public ResponseEntity<Resource> download(

            @PathVariable String sessionId,

            @PathVariable String fileName,

            @RequestHeader("X-AIF-Session") String token

    ) throws MalformedURLException {

        String normalizedSessionId =
                workspaceAccessService.requireAccess(
                        token,
                        sessionId
                );

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

            return ResponseEntity
                    .notFound()
                    .build();
        }

        Resource resource =
                new UrlResource(
                        target.toUri()
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
}
