package com.axiomai.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private Long userId;

    private String email;

    private String displayName;

    private String provider;

    private String avatarUrl;

    private String role;

    private String sessionToken;
}
