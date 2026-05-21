package com.axiomai.reporting.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin("*")
public class ReportController {

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> openReport(
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
                        MediaType.TEXT_HTML
                )

                .body(resource);
    }
}
