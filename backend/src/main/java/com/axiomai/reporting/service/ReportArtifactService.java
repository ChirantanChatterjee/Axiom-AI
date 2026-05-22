package com.axiomai.reporting.service;

import com.axiomai.reporting.entity.ReportArtifactEntity;
import com.axiomai.reporting.repository.ReportArtifactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReportArtifactService {

    private static final String HTML_CONTENT_TYPE =
            "text/html";

    private final ReportArtifactRepository
            repository;

    @Transactional
    public void saveHtmlReport(
            String fileName,
            String content
    ) {

        Instant now =
                Instant.now();

        ReportArtifactEntity artifact =
                repository.findById(fileName)
                        .orElseGet(
                                () -> ReportArtifactEntity.builder()
                                        .fileName(fileName)
                                        .createdAt(now)
                                        .build()
                        );

        artifact.setContentType(HTML_CONTENT_TYPE);
        artifact.setContent(content);
        artifact.setSizeBytes(
                content.getBytes(StandardCharsets.UTF_8).length
        );
        artifact.setUpdatedAt(now);

        repository.save(artifact);
    }

    @Transactional(readOnly = true)
    public Optional<ReportArtifactEntity> find(
            String fileName
    ) {

        return repository.findById(fileName);
    }
}
