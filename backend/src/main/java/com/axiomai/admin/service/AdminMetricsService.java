package com.axiomai.admin.service;

import com.axiomai.admin.dto.AdminMetricsResponse;
import com.axiomai.auth.entity.AifAuthSessionEntity;
import com.axiomai.auth.entity.AifUserEntity;
import com.axiomai.auth.repository.AifAuthSessionRepository;
import com.axiomai.auth.repository.AifUserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

@Service
public class AdminMetricsService {

    private final AifUserRepository userRepository;

    private final AifAuthSessionRepository sessionRepository;

    public AdminMetricsService(

            AifUserRepository userRepository,
            AifAuthSessionRepository sessionRepository

    ) {

        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional(readOnly = true)
    public AdminMetricsResponse metrics() {

        List<AdminMetricsResponse.UserDetail> userDetails =
                userDetails();

        List<AdminMetricsResponse.AuthSessionDetail> authSessionDetails =
                authSessionDetails();

        List<AdminMetricsResponse.GeneratedAssetDetail> frameworkDetails =
                frameworkDetails();

        List<AdminMetricsResponse.GeneratedAssetDetail> featureDetails =
                fileDetails(
                        Path.of("generated-frameworks"),
                        ".feature"
                );

        List<AdminMetricsResponse.GeneratedAssetDetail> reportDetails =
                fileDetails(
                        Path.of("reports"),
                        ".html"
                );

        List<AdminMetricsResponse.GeneratedAssetDetail> uploadedFrameworkDetails =
                fileDetails(
                        Path.of("generated-frameworks"),
                        "user-uploaded-framework.marker"
                );

        return AdminMetricsResponse.builder()
                .users(userDetails.size())
                .authSessions(authSessionDetails.size())
                .generatedFrameworks(
                        frameworkDetails.size()
                )
                .generatedFeatures(
                        featureDetails.size()
                )
                .executionReports(
                        reportDetails.size()
                )
                .uploadedFrameworks(
                        uploadedFrameworkDetails.size()
                )
                .userDetails(userDetails)
                .authSessionDetails(authSessionDetails)
                .generatedFrameworkDetails(frameworkDetails)
                .generatedFeatureDetails(featureDetails)
                .executionReportDetails(reportDetails)
                .uploadedFrameworkDetails(uploadedFrameworkDetails)
                .build();
    }

    private List<AdminMetricsResponse.UserDetail> userDetails() {

        return userRepository.findAll(
                        Sort.by(
                                Sort.Direction.DESC,
                                "lastLoginAt"
                        )
                )
                .stream()
                .map(this::userDetail)
                .toList();
    }

    private AdminMetricsResponse.UserDetail userDetail(
            AifUserEntity user
    ) {

        return AdminMetricsResponse.UserDetail.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .provider(user.getProvider())
                .role(user.getRole())
                .createdAt(asString(user.getCreatedAt()))
                .lastLoginAt(asString(user.getLastLoginAt()))
                .build();
    }

    private List<AdminMetricsResponse.AuthSessionDetail> authSessionDetails() {

        return sessionRepository.findAll(
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                )
                .stream()
                .map(this::authSessionDetail)
                .toList();
    }

    private AdminMetricsResponse.AuthSessionDetail authSessionDetail(
            AifAuthSessionEntity session
    ) {

        AifUserEntity user =
                session.getUser();

        return AdminMetricsResponse.AuthSessionDetail.builder()
                .id(session.getId())
                .userId(
                        user == null
                                ? null
                                : user.getId()
                )
                .userEmail(
                        user == null
                                ? "Unknown user"
                                : user.getEmail()
                )
                .createdAt(asString(session.getCreatedAt()))
                .expiresAt(asString(session.getExpiresAt()))
                .status(
                        session.getExpiresAt() != null
                                &&
                                session.getExpiresAt()
                                        .isAfter(Instant.now())
                                ? "Active"
                                : "Expired"
                )
                .build();
    }

    private List<AdminMetricsResponse.GeneratedAssetDetail> frameworkDetails() {

        Path generatedRoot =
                Path.of("generated-frameworks");

        if (
                !Files.exists(generatedRoot)
        ) {

            return List.of();
        }

        try (
                Stream<Path> paths =
                        Files.walk(generatedRoot)
        ) {

            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName()
                            .toString()
                            .equals("pom.xml"))
                    .filter(path -> path.getParent() != null
                            &&
                            path.getParent()
                                    .getFileName()
                                    .toString()
                                    .equals("framework"))
                    .map(this::assetDetail)
                    .toList();

        } catch (IOException e) {
            return List.of();
        }
    }

    private List<AdminMetricsResponse.GeneratedAssetDetail> fileDetails(
            Path root,
            String suffix
    ) {

        if (
                !Files.exists(root)
        ) {

            return List.of();
        }

        try (
                Stream<Path> paths =
                        Files.walk(root)
        ) {

            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName()
                            .toString()
                            .endsWith(suffix))
                    .map(this::assetDetail)
                    .toList();

        } catch (IOException e) {
            return List.of();
        }
    }

    private AdminMetricsResponse.GeneratedAssetDetail assetDetail(
            Path path
    ) {

        return AdminMetricsResponse.GeneratedAssetDetail.builder()
                .name(
                        path.getFileName()
                                .toString()
                )
                .sessionId(
                        sessionId(path)
                )
                .path(
                        path.toAbsolutePath()
                                .normalize()
                                .toString()
                )
                .modifiedAt(
                        modifiedAt(path)
                )
                .build();
    }

    private String sessionId(
            Path path
    ) {

        Path normalized =
                path.toAbsolutePath()
                        .normalize();

        for (
                int i = 0;
                i < normalized.getNameCount() - 1;
                i++
        ) {

            if (
                    "generated-frameworks".equalsIgnoreCase(
                            normalized.getName(i)
                                    .toString()
                    )
                            &&
                            i + 1 < normalized.getNameCount()
            ) {

                return normalized.getName(i + 1)
                        .toString();
            }
        }

        return "";
    }

    private String modifiedAt(
            Path path
    ) {

        try {

            return Files.getLastModifiedTime(path)
                    .toInstant()
                    .toString();

        } catch (IOException e) {

            return "";
        }
    }

    private String asString(
            Instant instant
    ) {

        return instant == null
                ? ""
                : instant.toString();
    }
}
