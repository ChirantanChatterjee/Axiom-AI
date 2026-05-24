package com.axiomai.core.runtime;

import com.axiomai.config.PublicBaseUrlResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeReportManagerTest {

    @Test
    void reportTemplateAllowsCssPercentValues() throws Exception {

        PublicBaseUrlResolver publicBaseUrlResolver =
                new PublicBaseUrlResolver();

        ReflectionTestUtils.setField(
                publicBaseUrlResolver,
                "configuredBaseUrl",
                "http://localhost:8080"
        );

        RuntimeReportManager manager =
                new RuntimeReportManager(
                        publicBaseUrlResolver
                );

        UnifiedRuntimeContext context =
                UnifiedRuntimeContext.builder()
                        .executionId("runtime-report-format-test")
                        .flowName("format check")
                        .status("PASSED")
                        .startedAt(LocalDateTime.now())
                        .completedAt(LocalDateTime.now())
                        .stepReports(
                                List.of(
                                        RuntimeStepReport.builder()
                                                .stepOrder(1)
                                                .action("CLICK")
                                                .target("send payment button")
                                                .status("PASSED")
                                                .durationMs(12)
                                                .build()
                                )
                        )
                        .build();

        String reportUrl =
                manager.generateReport(context);

        Path reportPath =
                Path.of(
                        "reports",
                        "runtime-report-format-test.html"
                );

        assertTrue(
                reportUrl.endsWith("/api/reports/runtime-report-format-test.html")
        );

        assertTrue(
                Files.readString(reportPath)
                        .contains("width: 100%;")
        );

        Files.deleteIfExists(reportPath);
    }
}
