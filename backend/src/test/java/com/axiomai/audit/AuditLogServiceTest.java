package com.axiomai.audit;

import com.axiomai.audit.entity.AuditLogEntity;
import com.axiomai.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditLogServiceTest {

    @Test
    void auditDetailsRedactSecretsBeforePersisting() {

        AtomicReference<AuditLogEntity> savedAudit =
                new AtomicReference<>();

        AuditLogService service =
                new AuditLogService(
                        auditRepository(savedAudit),
                        new ObjectMapper()
                );

        service.recordSuccess(
                "user-1",
                "session-1",
                "test.action",
                "TEST",
                "resource-1",
                Map.of(
                        "safeValue",
                        "visible",
                        "password",
                        "ClearPassword123!",
                        "openAiApiKey",
                        "sk-real-key",
                        "supabaseServiceRoleKey",
                        "sb_secret_real",
                        "authorization",
                        "Bearer token-value",
                        "nested",
                        Map.of(
                                "credentials",
                                "standard_user:secret_sauce"
                        )
                )
        );

        String detailsJson =
                savedAudit.get()
                        .getDetailsJson();

        assertTrue(
                detailsJson.contains("visible")
        );

        assertTrue(
                detailsJson.contains("<redacted>")
        );

        assertFalse(
                detailsJson.contains("ClearPassword123")
        );

        assertFalse(
                detailsJson.contains("sk-real-key")
        );

        assertFalse(
                detailsJson.contains("sb_secret_real")
        );

        assertFalse(
                detailsJson.contains("token-value")
        );

        assertFalse(
                detailsJson.contains("secret_sauce")
        );
    }

    private AuditLogRepository auditRepository(
            AtomicReference<AuditLogEntity> savedAudit
    ) {

        return (AuditLogRepository) Proxy.newProxyInstance(
                AuditLogRepository.class.getClassLoader(),
                new Class<?>[]{
                        AuditLogRepository.class
                },
                (proxy, method, args) -> {

                    if (
                            "save".equals(method.getName())
                    ) {

                        AuditLogEntity audit =
                                (AuditLogEntity) args[0];

                        savedAudit.set(audit);

                        return audit;
                    }

                    if (
                            "toString".equals(method.getName())
                    ) {

                        return "StubAuditLogRepository";
                    }

                    throw new UnsupportedOperationException(
                            method.getName()
                    );
                }
        );
    }
}
