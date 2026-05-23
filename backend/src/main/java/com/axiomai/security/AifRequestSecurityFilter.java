package com.axiomai.security;

import com.axiomai.auth.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class AifRequestSecurityFilter extends OncePerRequestFilter {

    public static final String SESSION_HEADER =
            "X-AIF-Session";

    private static final Set<String> PUBLIC_POST_PATHS =
            Set.of(
                    "/api/auth/signup",
                    "/api/auth/login",
                    "/api/auth/oauth-login"
            );

    private final AuthService authService;

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

            writeError(
                    response,
                    e.getStatusCode()
                            .value(),
                    safeReason(e)
            );

            return;

        } catch (RuntimeException e) {

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
}
