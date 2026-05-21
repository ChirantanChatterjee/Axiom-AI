package com.axiomai.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OAuthLoginRequest {

    private String email;

    private String displayName;

    private String provider;

    private String providerUserId;

    private String avatarUrl;
}
