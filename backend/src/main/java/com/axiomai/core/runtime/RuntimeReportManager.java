package com.axiomai.core.runtime;

import com.axiomai.core.execution.ExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component

public class RuntimeReportManager {

    // =====================================================
    // GENERATE REPORT
    // =====================================================

    public String generateReport(
            UnifiedRuntimeContext context
    ) {

        String reportPath =

                "reports/"
                        + context.getExecutionId()
                        + ".html";

        log.info(
                "Generating runtime report: {}",
                reportPath
        );

        return reportPath;
    }

    // =====================================================
    // ATTACH REPORT
    // =====================================================

    public void attachReport(

            ExecutionResult result,

            String reportPath

    ) {

        result.setReportPath(
                reportPath
        );
    }
}