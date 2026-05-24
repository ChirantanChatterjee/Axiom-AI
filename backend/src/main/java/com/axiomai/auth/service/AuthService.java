package com.axiomai.auth.service;

import com.axiomai.audit.AuditLogService;
import com.axiomai.auth.dto.AuthRequest;
import com.axiomai.auth.dto.AuthResponse;
import com.axiomai.auth.dto.OAuthLoginRequest;
import com.axiomai.auth.entity.AifAuthSessionEntity;
import com.axiomai.auth.entity.AifUserEntity;
import com.axiomai.auth.repository.AifAuthSessionRepository;
import com.axiomai.auth.repository.AifUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final AifUserRepository userRepository;

    private final AifAuthSessionRepository sessionRepository;

    private final PasswordHashService passwordHashService;

    private AuditLogService auditLogService;

    @Value("${aif.admin.emails:}")
    private String adminEmails;

    @Autowired(required = false)
    public void setAuditLogService(
            AuditLogService auditLogService
    ) {

        this.auditLogService =
                auditLogService;
    }

    @Transactional
    public AuthResponse signup(
            AuthRequest request
    ) {

        String email =
                normalizeEmail(request.getEmail());

        String password =
                requirePassword(request.getPassword());

        if (
                userRepository.existsByEmailIgnoreCase(email)
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account already exists for this email."
            );
        }

        Instant now =
                Instant.now();

        AifUserEntity user =
                AifUserEntity.builder()
                        .email(email)
                        .displayName(displayName(request, email))
                        .passwordHash(passwordHashService.hash(password))
                        .provider("email")
                        .role(roleFor(email))
                        .createdAt(now)
                        .lastLoginAt(now)
                        .build();

        AifUserEntity savedUser =
                userRepository.save(user);

        AuthResponse response =
                createSession(savedUser);

        auditSuccess(
                savedUser,
                "auth.signup",
                Map.of("provider", "email")
        );

        return response;
    }

    @Transactional
    public AuthResponse oauthLogin(
            OAuthLoginRequest request
    ) {

        String email =
                normalizeEmail(request.getEmail());

        Instant now =
                Instant.now();

        AifUserEntity user =
                userRepository.findByEmailIgnoreCase(email)
                        .orElseGet(() -> AifUserEntity.builder()
                                .email(email)
                                .displayName(displayName(request, email))
                                .passwordHash(
                                        passwordHashService.hash(
                                                UUID.randomUUID()
                                                        .toString()
                                        )
                                )
                                .provider(provider(request.getProvider()))
                                .role(roleFor(email))
                                .createdAt(now)
                                .lastLoginAt(now)
                                .build());

        user.setDisplayName(
                displayName(request, email)
        );

        user.setProvider(
                provider(request.getProvider())
        );

        user.setProviderUserId(
                blankToNull(
                        request.getProviderUserId()
                )
        );

        user.setAvatarUrl(
                blankToNull(
                        request.getAvatarUrl()
                )
        );

        if (
                user.getRole() == null
                        ||
                        user.getRole().isBlank()
        ) {

            user.setRole(
                    roleFor(email)
            );
        }

        user.setLastLoginAt(now);

        AifUserEntity savedUser =
                userRepository.save(user);

        AuthResponse response =
                createSession(savedUser);

        auditSuccess(
                savedUser,
                "auth.login",
                Map.of(
                        "provider",
                        savedUser.getProvider() == null
                                ? "oauth"
                                : savedUser.getProvider()
                )
        );

        return response;
    }

    @Transactional
    public AuthResponse login(
            AuthRequest request
    ) {

        String email =
                normalizeEmail(request.getEmail());

        String password =
                requirePassword(request.getPassword());

        AifUserEntity user =
                userRepository.findByEmailIgnoreCase(email)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid email or password."
                        ));

        if (
                !passwordHashService.verify(
                        password,
                        user.getPasswordHash()
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password."
            );
        }

        user.setLastLoginAt(
                Instant.now()
        );

        AifUserEntity savedUser =
                userRepository.save(user);

        AuthResponse response =
                createSession(savedUser);

        auditSuccess(
                savedUser,
                "auth.login",
                Map.of("provider", "email")
        );

        return response;
    }

    @Transactional(readOnly = true)
    public AuthResponse profile(
            String token
    ) {

        return response(
                sessionFor(token)
                        .getUser(),
                token
        );
    }

    @Transactional(readOnly = true)
    public AifUserEntity requireUser(
            String token
    ) {

        return sessionFor(token)
                .getUser();
    }

    @Transactional(readOnly = true)
    public AifUserEntity requireAdmin(
            String token
    ) {

        AifUserEntity user =
                requireUser(token);

        if (
                !"ADMIN".equalsIgnoreCase(
                        roleFor(user)
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Admin access is required."
            );
        }

        return user;
    }

    private AifAuthSessionEntity sessionFor(
            String token
    ) {

        if (
                token == null
                        ||
                        token.isBlank()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Missing session token."
            );
        }

        AifAuthSessionEntity session =
                sessionRepository.findByToken(token)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid session token."
                        ));

        if (
                session.getExpiresAt()
                        .isBefore(Instant.now())
        ) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Session expired."
            );
        }

        return session;
    }

    private AuthResponse createSession(
            AifUserEntity user
    ) {

        String token =
                UUID.randomUUID().toString()
                        + "-"
                        + UUID.randomUUID();

        AifAuthSessionEntity session =
                AifAuthSessionEntity.builder()
                        .token(token)
                        .user(user)
                        .createdAt(Instant.now())
                        .expiresAt(
                                Instant.now()
                                        .plus(
                                                30,
                                                ChronoUnit.DAYS
                                        )
                        )
                        .build();

        sessionRepository.save(session);

        return response(
                user,
                token
        );
    }

    private AuthResponse response(
            AifUserEntity user,
            String token
    ) {

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .provider(user.getProvider())
                .avatarUrl(user.getAvatarUrl())
                .role(roleFor(user))
                .sessionToken(token)
                .build();
    }

    private void auditSuccess(
            AifUserEntity user,
            String action,
            Map<String, ?> details
    ) {

        if (
                auditLogService == null
                        ||
                        user == null
        ) {

            return;
        }

        auditLogService.recordSuccess(
                String.valueOf(user.getId()),
                null,
                action,
                "USER",
                String.valueOf(user.getId()),
                details
        );
    }

    private String normalizeEmail(
            String email
    ) {

        String normalized =
                email == null
                        ? ""
                        : email.trim()
                                .toLowerCase(Locale.ROOT);

        if (
                !EMAIL_PATTERN.matcher(normalized)
                        .matches()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Enter a valid email address."
            );
        }

        return normalized;
    }

    private String requirePassword(
            String password
    ) {

        if (
                password == null
                        ||
                        password.length() < 8
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must be at least 8 characters."
            );
        }

        return password;
    }

    private String displayName(
            AuthRequest request,
            String email
    ) {

        if (
                request.getDisplayName() != null
                        &&
                        !request.getDisplayName()
                                .trim()
                                .isBlank()
        ) {

            return request.getDisplayName()
                    .trim();
        }

        return email.split("@")[0];
    }

    private String displayName(
            OAuthLoginRequest request,
            String email
    ) {

        if (
                request.getDisplayName() != null
                        &&
                        !request.getDisplayName()
                                .trim()
                                .isBlank()
        ) {

            return request.getDisplayName()
                    .trim();
        }

        return email.split("@")[0];
    }

    private String provider(
            String value
    ) {

        if (
                value == null
                        ||
                        value.isBlank()
        ) {

            return "oauth";
        }

        return value.trim()
                .toLowerCase(Locale.ROOT);
    }

    private String roleFor(
            String email
    ) {

        if (
                adminEmails == null
                        ||
                        adminEmails.isBlank()
        ) {

            return "USER";
        }

        String normalized =
                email == null
                        ? ""
                        : email.trim()
                                .toLowerCase(Locale.ROOT);

        for (
                String adminEmail
                : adminEmails.split(",")
        ) {

            if (
                    normalized.equals(
                            adminEmail.trim()
                                    .toLowerCase(Locale.ROOT)
                    )
            ) {

                return "ADMIN";
            }
        }

        return "USER";
    }

    private String roleFor(
            AifUserEntity user
    ) {

        if (
                "ADMIN".equals(
                        roleFor(
                                user.getEmail()
                        )
                )
        ) {

            return "ADMIN";
        }

        if (
                user.getRole() == null
                        ||
                        user.getRole()
                                .isBlank()
        ) {

            return roleFor(
                    user.getEmail()
            );
        }

        return user.getRole();
    }

    private String blankToNull(
            String value
    ) {

        if (
                value == null
                        ||
                        value.trim()
                                .isBlank()
        ) {

            return null;
        }

        return value.trim();
    }
}
