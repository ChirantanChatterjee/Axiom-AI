package com.axiomai.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminMetricsResponse {

    private long users;

    private long authSessions;

    private long generatedFrameworks;

    private long generatedFeatures;

    private long executionReports;

    private long uploadedFrameworks;

    private List<UserDetail> userDetails;

    private List<AuthSessionDetail> authSessionDetails;

    private List<GeneratedAssetDetail> generatedFrameworkDetails;

    private List<GeneratedAssetDetail> generatedFeatureDetails;

    private List<GeneratedAssetDetail> executionReportDetails;

    private List<GeneratedAssetDetail> uploadedFrameworkDetails;

    @Getter
    @Builder
    public static class UserDetail {

        private Long id;

        private String email;

        private String displayName;

        private String provider;

        private String role;

        private String createdAt;

        private String lastLoginAt;
    }

    @Getter
    @Builder
    public static class AuthSessionDetail {

        private Long id;

        private Long userId;

        private String userEmail;

        private String createdAt;

        private String expiresAt;

        private String status;
    }

    @Getter
    @Builder
    public static class GeneratedAssetDetail {

        private String name;

        private String sessionId;

        private String path;

        private String modifiedAt;
    }
}
