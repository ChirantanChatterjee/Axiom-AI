package com.axiomai.security;

import com.axiomai.audit.AuditLogService;
import com.axiomai.auth.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
@Slf4j
public class AifRequestSecurityFilter extends OncePerRequestFilter {

    public static final String SESSION_HEADER =
            "X-AIF-Session";

    private static final String DEFAULT_ALLOWED_ORIGINS =
            "https://aif-pi.vercel.app,http://localhost:5173,http://localhost:3000,http://localhost:8080";

    private static final String ALLOWED_METHODS =
            "GET,POST,PUT,PATCH,DELETE,OPTIONS";

    private static final String ALLOWED_HEADERS =
            "Accept,Authorization,Content-Type,X-AIF-Session,X-Requested-With";

    private static final Set<String> PUBLIC_POST_PATHS =
            Set.of(
                    "/api/auth/signup",
                    "/api/auth/login",
                    "/api/auth/oauth-login"
            );

    private final AuthService authService;

    @Value("${aif.cors.allowed-origins:${aif.cors.allowed-origin-patterns:" + DEFAULT_ALLOWED_ORIGINS + "}}")
    private String allowedOrigins;

    private AuditLogService auditLogService;

    @Autowired(required = false)
    public void setAuditLogService(
            AuditLogService auditLogService
    ) {

        this.auditLogService =
                auditLogService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        addSecurityHeaders(
                request,
                response
        );

        addCorsHeaders(
                request,
                response
        );

        if (
                "OPTIONS".equalsIgnoreCase(
                        request.getMethod()
                )
        ) {

            response.setStatus(
                    HttpServletResponse.SC_NO_CONTENT
            );

            return;
        }

        String path =
                path(request);

        if (
                isPublicRequest(
                        request,
                        path
                )
                        ||
                        !requiresAuthenticatedSession(path)
        ) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        try {

            String token =
                    request.getHeader(SESSION_HEADER);

            if (
                    path.startsWith("/api/admin/")
            ) {

                authService.requireAdmin(token);

            } else {

                authService.requireUser(token);
            }

        } catch (ResponseStatusException e) {

            int status =
                    e.getStatusCode()
                            .value();

            String reason =
                    safeReason(e);

            logWorkspaceSessionDenied(
                    request,
                    path,
                    status,
                    reason
            );

            auditAccessDenied(
                    request,
                    path,
                    status,
                    reason
            );

            writeError(
                    response,
                    status,
                    reason
            );

            return;

        } catch (RuntimeException e) {

            logWorkspaceSessionDenied(
                    request,
                    path,
                    HttpStatus.UNAUTHORIZED.value(),
                    "Unauthorized"
            );

            auditAccessDenied(
                    request,
                    path,
                    HttpStatus.UNAUTHORIZED.value(),
                    "Unauthorized"
            );

            writeError(
                    response,
                    HttpStatus.UNAUTHORIZED.value(),
                    "Unauthorized"
            );

            return;
        }

        filterChain.doFilter(
                request,
                response
        );
    }

    private void logWorkspaceSessionDenied(
            HttpServletRequest request,
            String path,
            int status,
            String reason
    ) {

        if (
                !path.startsWith("/api/workspace/sessions")
        ) {

            return;
        }

        log.warn(
                "Workspace chat session request denied method={} path={} status={} reason={} hasSessionHeader={}",
                request.getMethod(),
                path,
                status,
                reason,
                hasSessionHeader(request)
        );
    }

    private boolean hasSessionHeader(
            HttpServletRequest request
    ) {

        String token =
                request.getHeader(SESSION_HEADER);

        return token != null
                &&
                !token.isBlank();
    }

    private void addSecurityHeaders(
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        response.setHeader(
                "X-Content-Type-Options",
                "nosniff"
        );

        response.setHeader(
                "X-Frame-Options",
                "DENY"
        );

        response.setHeader(
                "Referrer-Policy",
                "no-referrer"
        );

        response.setHeader(
                "Permissions-Policy",
                "camera=(), microphone=(), geolocation=()"
        );

        if (
                path(request).startsWith("/api/")
        ) {

            response.setHeader(
                    "Cache-Control",
                    "no-store"
            );
        }

        if (
                request.isSecure()
                        ||
                        "https".equalsIgnoreCase(
                                request.getHeader("X-Forwarded-Proto")
                        )
        ) {

            response.setHeader(
                    "Strict-Transport-Security",
                    "max-age=31536000; includeSubDomains"
            );
        }
    }

    private void addCorsHeaders(
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        String origin =
                normalizeOrigin(
                        request.getHeader("Origin")
                );

        if (
                origin == null
        ) {

            return;
        }

        if (
                !isAllowedOrigin(origin)
        ) {

            return;
        }

        response.setHeader(
                "Access-Control-Allow-Origin",
                origin
        );
        response.setHeader(
                "Access-Control-Allow-Credentials",
                "true"
        );
        response.setHeader(
                "Access-Control-Allow-Methods",
                ALLOWED_METHODS
        );
        response.setHeader(
                "Access-Control-Allow-Headers",
                ALLOWED_HEADERS
        );
        response.setHeader(
                "Access-Control-Expose-Headers",
                "Content-Disposition"
        );
        response.addHeader(
                "Vary",
                "Origin"
        );
        response.addHeader(
                "Vary",
                "Access-Control-Request-Method"
        );
        response.addHeader(
                "Vary",
                "Access-Control-Request-Headers"
        );
    }

    private boolean isAllowedOrigin(
            String origin
    ) {

        return Arrays.stream(
                        configuredAllowedOrigins()
                )
                .anyMatch(origin::equals);
    }

    private String[] configuredAllowedOrigins() {

        String[] configured =
                Arrays.stream(
                                safeAllowedOrigins()
                                        .split(",")
                        )
                        .map(this::normalizeOrigin)
                        .filter(origin -> origin != null)
                        .filter(this::isSafeExactOrigin)
                        .toArray(String[]::new);

        if (
                configured.length > 0
        ) {

            return configured;
        }

        return Arrays.stream(
                        DEFAULT_ALLOWED_ORIGINS.split(",")
                )
                .map(this::normalizeOrigin)
                .filter(origin -> origin != null)
                .toArray(String[]::new);
    }

    private String safeAllowedOrigins() {

        return allowedOrigins == null
                ||
                allowedOrigins.isBlank()
                ? DEFAULT_ALLOWED_ORIGINS
                : allowedOrigins;
    }

    private String normalizeOrigin(
            String origin
    ) {

        if (
                origin == null
        ) {

            return null;
        }

        String normalized =
                origin.trim();

        while (
                normalized.endsWith("/")
        ) {

            normalized =
                    normalized.substring(
                            0,
                            normalized.length() - 1
                    );
        }

        return normalized.isBlank()
                ? null
                : normalized;
    }

    private boolean isSafeExactOrigin(
            String origin
    ) {

        String lower =
                origin.toLowerCase(Locale.ROOT);

        return (lower.startsWith("https://")
                ||
                lower.startsWith("http://localhost:")
                ||
                lower.startsWith("http://127.0.0.1:"))
                &&
                !origin.contains("*");
    }

    private boolean isPublicRequest(
            HttpServletRequest request,
            String path
    ) {

        if (
                "OPTIONS".equalsIgnoreCase(
                        request.getMethod()
                )
        ) {

            return true;
        }

        if (
                isHealthPath(path)
                        ||
                        path.startsWith("/api/reports/")
        ) {

            return true;
        }

        return "POST".equalsIgnoreCase(
                request.getMethod()
        )
                &&
                PUBLIC_POST_PATHS.contains(path);
    }

    private boolean isHealthPath(
            String path
    ) {

        return "/health".equals(path)
                ||
                "/api/health".equals(path)
                ||
                "/actuator/health".equals(path)
                ||
                path.startsWith("/actuator/health/");
    }

    private boolean requiresAuthenticatedSession(
            String path
    ) {

        return path.startsWith("/api/")
                ||
                path.startsWith("/qa/")
                ||
                path.startsWith("/flow/")
                ||
                "/chat".equals(path)
                ||
                path.startsWith("/chat/");
    }

    private String path(
            HttpServletRequest request
    ) {

        String servletPath =
                request.getServletPath();

        if (
                servletPath == null
                        ||
                        servletPath.isBlank()
        ) {

            String requestUri =
                    request.getRequestURI();

            String contextPath =
                    request.getContextPath();

            if (
                    requestUri != null
                            &&
                            contextPath != null
                            &&
                            !contextPath.isBlank()
                            &&
                            requestUri.startsWith(contextPath)
            ) {

                return requestUri.substring(
                        contextPath.length()
                );
            }

            if (
                    requestUri != null
                            &&
                            !requestUri.isBlank()
            ) {

                return requestUri;
            }

            return "/";
        }

        return servletPath;
    }

    private String safeReason(
            ResponseStatusException e
    ) {

        String reason =
                e.getReason();

        if (
                reason == null
                        ||
                        reason.isBlank()
        ) {

            return e.getStatusCode()
                    .toString();
        }

        return reason;
    }

    private void writeError(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {

        if (
                response.isCommitted()
        ) {

            return;
        }

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter()
                .write(
                        "{\"error\":\""
                                + jsonEscape(message)
                                + "\"}"
                );
    }

    private String jsonEscape(
            String value
    ) {

        if (
                value == null
        ) {

            return "";
        }

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private void auditAccessDenied(
            HttpServletRequest request,
            String path,
            int status,
            String reason
    ) {

        if (
                auditLogService == null
        ) {

            return;
        }

        auditLogService.recordDenied(
                null,
                null,
                "api.access",
                "HTTP_ENDPOINT",
                path,
                Map.of(
                        "method",
                        request.getMethod(),
                        "status",
                        status,
                        "reason",
                        reason,
                        "adminEndpoint",
                        path.startsWith("/api/admin/")
                )
        );
    }
}
