package com.axiomai.security;

import com.axiomai.auth.entity.AifUserEntity;
import com.axiomai.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AifRequestSecurityFilterTest {

    private final StubAuthService authService =
            new StubAuthService();

    private final AifRequestSecurityFilter filter =
            new AifRequestSecurityFilter(authService);

    @Test
    void healthEndpointDoesNotRequireSession()
            throws Exception {

        MockHttpServletRequest request =
                request("GET", "/api/health");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                new MockFilterChain()
        );

        assertNotEquals(
                HttpStatus.UNAUTHORIZED.value(),
                response.getStatus()
        );

        assertEquals(
                0,
                authService.requireUserCalls
        );
    }

    @Test
    void workspaceEndpointRejectsMissingSession()
            throws Exception {

        authService.requireUserException =
                new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Missing session token."
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request(
                        "GET",
                        "/api/workspace/artifacts/session/framework.zip"
                ),
                response,
                new MockFilterChain()
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED.value(),
                response.getStatus()
        );

        assertEquals(
                "{\"error\":\"Missing session token.\"}",
                response.getContentAsString()
        );
    }

    @Test
    void workspaceEndpointAllowsValidSession()
            throws Exception {

        authService.user =
                user();

        MockHttpServletRequest request =
                request(
                        "GET",
                        "/api/generated-test-executions/session/chat-1"
                );

        request.addHeader(
                AifRequestSecurityFilter.SESSION_HEADER,
                "valid-session"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                new MockFilterChain()
        );

        assertNotEquals(
                HttpStatus.UNAUTHORIZED.value(),
                response.getStatus()
        );

        assertEquals(
                "valid-session",
                authService.lastRequireUserToken
        );
    }

    @Test
    void adminEndpointRequiresAdminSession()
            throws Exception {

        authService.requireAdminException =
                new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Admin access is required."
                );

        MockHttpServletRequest request =
                request(
                        "GET",
                        "/api/admin/metrics"
                );

        request.addHeader(
                AifRequestSecurityFilter.SESSION_HEADER,
                "user-session"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                new MockFilterChain()
        );

        assertEquals(
                HttpStatus.FORBIDDEN.value(),
                response.getStatus()
        );

        assertEquals(
                "user-session",
                authService.lastRequireAdminToken
        );
    }

    @Test
    void loginEndpointDoesNotRequireSession()
            throws Exception {

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request(
                        "POST",
                        "/api/auth/login"
                ),
                response,
                new MockFilterChain()
        );

        assertNotEquals(
                HttpStatus.UNAUTHORIZED.value(),
                response.getStatus()
        );

        assertEquals(
                0,
                authService.requireUserCalls
        );
    }

    private MockHttpServletRequest request(
            String method,
            String path
    ) {

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        method,
                        path
                );

        request.setServletPath(path);

        return request;
    }

    private AifUserEntity user() {

        return AifUserEntity.builder()
                .id(1L)
                .email("user@example.com")
                .role("USER")
                .build();
    }

    private static class StubAuthService extends AuthService {

        private AifUserEntity user =
                AifUserEntity.builder()
                        .id(1L)
                        .email("user@example.com")
                        .role("USER")
                        .build();

        private ResponseStatusException requireUserException;

        private ResponseStatusException requireAdminException;

        private String lastRequireUserToken;

        private String lastRequireAdminToken;

        private int requireUserCalls;

        private StubAuthService() {

            super(
                    null,
                    null,
                    null
            );
        }

        @Override
        public AifUserEntity requireUser(
                String token
        ) {

            requireUserCalls += 1;
            lastRequireUserToken =
                    token;

            if (
                    requireUserException != null
            ) {

                throw requireUserException;
            }

            return user;
        }

        @Override
        public AifUserEntity requireAdmin(
                String token
        ) {

            lastRequireAdminToken =
                    token;

            if (
                    requireAdminException != null
            ) {

                throw requireAdminException;
            }

            return user;
        }
    }
}
