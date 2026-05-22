package com.axiomai.auth.controller;

import com.axiomai.auth.dto.AuthRequest;
import com.axiomai.auth.dto.AuthResponse;
import com.axiomai.auth.dto.OAuthLoginRequest;
import com.axiomai.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public AuthResponse signup(
            @RequestBody AuthRequest request
    ) {

        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @RequestBody AuthRequest request
    ) {

        return authService.login(request);
    }

    @PostMapping("/oauth-login")
    public AuthResponse oauthLogin(
            @RequestBody OAuthLoginRequest request
    ) {

        return authService.oauthLogin(request);
    }

    @GetMapping("/me")
    public AuthResponse profile(
            @RequestHeader("X-AIF-Session") String token
    ) {

        return authService.profile(token);
    }
}
