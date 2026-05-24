package com.axiomai.core.runtime;

import com.axiomai.config.PublicBaseUrlResolver;
import com.axiomai.core.execution.ExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor

public class RuntimeReportManager {

    private final PublicBaseUrlResolver
            publicBaseUrlResolver;

    // =====================================================
    // GENERATE REPORT
    // =====================================================

    public String generateReport(
            UnifiedRuntimeContext context
    ) {

        try {

            Files.createDirectories(
                    Paths.get("reports")
            );

            String fileName =
                    context.getExecutionId()
                            + ".html";

            Path reportPath =
                    Paths.get("reports")
                            .resolve(fileName);

            Files.writeString(
                    reportPath,
                    buildHtml(context)
            );

            String reportUrl =
                    publicBaseUrlResolver.url(
                            "/api/reports/"
                                    + fileName
                    );

            log.info(
                    "Generated runtime report: {}",
                    reportUrl
            );

            return reportUrl;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to generate runtime report.",
                    e
            );
        }
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

    private String buildHtml(
            UnifiedRuntimeContext context
    ) {

        String completedAt =
                context.getCompletedAt() == null
                        ? ""
                        : context.getCompletedAt()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyy-MM-dd HH:mm:ss"
                                )
                        );

        String startedAt =
                context.getStartedAt() == null
                        ? ""
                        : context.getStartedAt()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyy-MM-dd HH:mm:ss"
                                )
                        );

        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>AIF Execution Report</title>
                    <style>
                        body {
                            margin: 0;
                            font-family: Arial, sans-serif;
                            background: #f4f6f8;
                            color: #172033;
                        }
                        main {
                            max-width: 980px;
                            margin: 32px auto;
                            padding: 0 20px;
                        }
                        .panel {
                            background: #ffffff;
                            border: 1px solid #d9e0ea;
                            border-radius: 8px;
                            padding: 24px;
                            margin-bottom: 16px;
                        }
                        h1 {
                            margin: 0 0 8px;
                            font-size: 28px;
                        }
                        h2 {
                            margin: 0 0 12px;
                            font-size: 18px;
                        }
                        .meta {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
                            gap: 12px;
                            margin-top: 20px;
                        }
                        .item {
                            background: #f7f9fc;
                            border: 1px solid #e3e9f1;
                            border-radius: 6px;
                            padding: 12px;
                        }
                        .label {
                            display: block;
                            color: #637083;
                            font-size: 12px;
                            text-transform: uppercase;
                            margin-bottom: 6px;
                        }
                        .status {
                            display: inline-block;
                            padding: 4px 10px;
                            border-radius: 999px;
                            background: #dcfce7;
                            color: #166534;
                            font-weight: 700;
                        }
                        ul {
                            margin: 0;
                            padding-left: 20px;
                        }
                        .step {
                            border: 1px solid #e3e9f1;
                            border-radius: 8px;
                            padding: 16px;
                            margin-bottom: 16px;
                            background: #ffffff;
                        }
                        .step.passed {
                            border-left: 6px solid #16a34a;
                        }
                        .step.failed {
                            border-left: 6px solid #dc2626;
                        }
                        .step-header {
                            display: flex;
                            justify-content: space-between;
                            gap: 16px;
                            align-items: center;
                            margin-bottom: 12px;
                        }
                        .failed-badge {
                            background: #fee2e2;
                            color: #991b1b;
                        }
                        .passed-badge {
                            background: #dcfce7;
                            color: #166534;
                        }
                        .badge {
                            display: inline-block;
                            border-radius: 999px;
                            padding: 4px 10px;
                            font-weight: 700;
                            font-size: 12px;
                        }
                        .error {
                            background: #fff1f2;
                            border: 1px solid #fecdd3;
                            border-radius: 6px;
                            color: #9f1239;
                            padding: 10px;
                            margin-top: 12px;
                            white-space: pre-wrap;
                        }
                        img {
                            display: block;
                            width: 100%%;
                            margin-top: 12px;
                            border: 1px solid #d9e0ea;
                            border-radius: 6px;
                        }
                    </style>
                </head>
                <body>
                    <main>
                        <section class="panel">
                            <h1>AIF Execution Report</h1>
                            <span class="status">%s</span>
                            <div class="meta">
                                <div class="item">
                                    <span class="label">Execution ID</span>
                                    %s
                                </div>
                                <div class="item">
                                    <span class="label">Flow</span>
                                    %s
                                </div>
                                <div class="item">
                                    <span class="label">Started</span>
                                    %s
                                </div>
                                <div class="item">
                                    <span class="label">Completed</span>
                                    %s
                                </div>
                            </div>
                        </section>
                        <section class="panel">
                            <h2>Step Timeline</h2>
                            %s
                        </section>
                    </main>
                </body>
                </html>
                """.formatted(
                escape(context.getStatus()),
                escape(context.getExecutionId()),
                escape(context.getFlowName()),
                escape(startedAt),
                escape(completedAt),
                buildStepTimeline(context)
        );
    }

    private String buildStepTimeline(
            UnifiedRuntimeContext context
    ) {

        if (
                context.getStepReports() == null
                        ||
                        context.getStepReports()
                                .isEmpty()
        ) {

            return "<p>No step details were captured.</p>";
        }

        StringBuilder builder =
                new StringBuilder();

        for (
                RuntimeStepReport step
                : context.getStepReports()
        ) {

            boolean passed =
                    "PASSED".equalsIgnoreCase(
                            step.getStatus()
                    );

            builder.append("<article class='step ")
                    .append(
                            passed
                                    ? "passed"
                                    : "failed"
                    )
                    .append("'>");

            builder.append("<div class='step-header'>")
                    .append("<h3>Step ")
                    .append(step.getStepOrder())
                    .append(": ")
                    .append(escape(step.getAction()))
                    .append(" ")
                    .append(escape(step.getTarget()))
                    .append("</h3>")
                    .append("<span class='badge ")
                    .append(
                            passed
                                    ? "passed-badge"
                                    : "failed-badge"
                    )
                    .append("'>")
                    .append(escape(step.getStatus()))
                    .append("</span>")
                    .append("</div>");

            builder.append("<div class='meta'>")
                    .append("<div class='item'><span class='label'>Node ID</span>")
                    .append(escape(step.getNodeId()))
                    .append("</div>")
                    .append("<div class='item'><span class='label'>Duration</span>")
                    .append(step.getDurationMs())
                    .append(" ms</div>")
                    .append("<div class='item'><span class='label'>Executed At</span>")
                    .append(
                            step.getExecutedAt() == null
                                    ? ""
                                    : escape(
                                            step.getExecutedAt()
                                                    .format(
                                                            DateTimeFormatter.ofPattern(
                                                                    "yyyy-MM-dd HH:mm:ss"
                                                            )
                                                    )
                                    )
                    )
                    .append("</div>")
                    .append("</div>");

            if (
                    step.getErrorMessage() != null
                            &&
                            !step.getErrorMessage()
                                    .isBlank()
            ) {

                builder.append("<div class='error'>")
                        .append(
                                escape(
                                        step.getErrorMessage()
                                )
                        )
                        .append("</div>");
            }

            String image =
                    imageDataUri(
                            step.getScreenshotPath()
                    );

            if (
                    image != null
            ) {

                builder.append("<img src='")
                        .append(image)
                        .append("' alt='Step ")
                        .append(step.getStepOrder())
                        .append(" screenshot' />");
            }

            builder.append("</article>");
        }

        return builder.toString();
    }

    private String imageDataUri(
            String screenshotPath
    ) {

        try {

            if (
                    screenshotPath == null
                            ||
                            screenshotPath.isBlank()
            ) {

                return null;
            }

            byte[] bytes =
                    Files.readAllBytes(
                            Paths.get(screenshotPath)
                    );

            return "data:image/png;base64,"
                    + Base64.getEncoder()
                    .encodeToString(bytes);

        } catch (Exception ignored) {

            return null;
        }
    }

    private String buildList(
            Iterable<String> values
    ) {

        StringBuilder builder =
                new StringBuilder("<ul>");

        boolean hasValues =
                false;

        for (
                String value
                : values
        ) {

            hasValues =
                    true;

            builder.append("<li>")
                    .append(escape(value))
                    .append("</li>");
        }

        if (
                !hasValues
        ) {

            builder.append("<li>None</li>");
        }

        builder.append("</ul>");

        return builder.toString();
    }

    private String escape(
            String value
    ) {

        if (
                value == null
        ) {

            return "";
        }

        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
