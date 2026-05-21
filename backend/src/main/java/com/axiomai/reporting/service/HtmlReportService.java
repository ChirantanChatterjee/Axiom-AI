package com.axiomai.reporting.service;

import com.axiomai.execution.entity.FlowExecutionEntity;
import com.axiomai.execution.entity.StepExecutionEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class HtmlReportService {

    public String generateReport(
            FlowExecutionEntity execution,
            List<StepExecutionEntity> steps
    ) {

        try {

            File reportFolder =
                    new File("reports");

            if (!reportFolder.exists()) {

                reportFolder.mkdirs();

            }

            String reportPath =
                    "reports/execution_"
                            + execution.getId()
                            + ".html";

            FileWriter writer =
                    new FileWriter(reportPath);

            writer.write(
                    buildHtml(execution, steps)
            );

            writer.close();

            return reportPath;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to generate report",
                    e
            );

        }

    }

    private String buildHtml(
            FlowExecutionEntity execution,
            List<StepExecutionEntity> steps
    ) {

        StringBuilder html =
                new StringBuilder();

        html.append("""
                <html>

                <head>

                <title>
                AxiomAI Execution Report
                </title>

                <style>

                body {
                    font-family: Arial, sans-serif;
                    background: #f5f5f5;
                    padding: 20px;
                }

                .container {
                    max-width: 1200px;
                    margin: auto;
                }

                .header {
                    background: white;
                    padding: 25px;
                    border-radius: 12px;
                    margin-bottom: 25px;
                    box-shadow: 0px 2px 8px rgba(0,0,0,0.1);
                }

                .step {
                    background: white;
                    padding: 20px;
                    margin-bottom: 20px;
                    border-radius: 12px;
                    box-shadow: 0px 2px 8px rgba(0,0,0,0.08);
                }

                .passed {
                    border-left: 8px solid #16a34a;
                }

                .failed {
                    border-left: 8px solid #dc2626;
                }

                .label {
                    font-weight: bold;
                }

                .status-passed {
                    color: #16a34a;
                    font-weight: bold;
                }

                .status-failed {
                    color: #dc2626;
                    font-weight: bold;
                }

                .screenshot {
                    margin-top: 20px;
                }

                img {
                    width: 100%;
                    border-radius: 10px;
                    border: 1px solid #ccc;
                }

                .timeline {
                    margin-top: 10px;
                    color: #555;
                }

                .error-box {
                    background: #ffe5e5;
                    padding: 15px;
                    border-radius: 8px;
                    margin-top: 15px;
                    color: #b91c1c;
                    font-weight: bold;
                }

                </style>

                </head>

                <body>

                <div class='container'>
                """);

        /*
         * =====================================================
         * HEADER
         * =====================================================
         */

        html.append("""
                <div class='header'>
                <h1>AxiomAI Execution Report</h1>
                """);

        html.append("<p><span class='label'>Execution ID:</span> ")
                .append(execution.getId())
                .append("</p>");

        html.append("<p><span class='label'>Execution Status:</span> ");

        if (
                execution.getStatus()
                        .equalsIgnoreCase("PASSED")
        ) {

            html.append("<span class='status-passed'>PASSED</span>");

        } else {

            html.append("<span class='status-failed'>FAILED</span>");

        }

        html.append("</p>");

        html.append("<p><span class='label'>Started At:</span> ")
                .append(
                        execution.getStartedAt()
                                .format(
                                        DateTimeFormatter.ofPattern(
                                                "yyyy-MM-dd HH:mm:ss"
                                        )
                                )
                )
                .append("</p>");

        if (execution.getCompletedAt() != null) {

            html.append("<p><span class='label'>Completed At:</span> ")
                    .append(
                            execution.getCompletedAt()
                                    .format(
                                            DateTimeFormatter.ofPattern(
                                                    "yyyy-MM-dd HH:mm:ss"
                                            )
                                    )
                    )
                    .append("</p>");

        }

        html.append("</div>");

        /*
         * =====================================================
         * STEP REPORTS
         * =====================================================
         */

        for (StepExecutionEntity step : steps) {

            String cssClass =
                    step.getStatus()
                            .equalsIgnoreCase("PASSED")
                            ? "passed"
                            : "failed";

            html.append("<div class='step ")
                    .append(cssClass)
                    .append("'>");

            html.append("<h2>STEP ")
                    .append(step.getStepOrder())
                    .append("</h2>");

            html.append("<p><span class='label'>Action:</span> ")
                    .append(step.getAction())
                    .append("</p>");

            html.append("<p><span class='label'>Element:</span> ")
                    .append(step.getElementName())
                    .append("</p>");

            html.append("<p><span class='label'>Status:</span> ");

            if (
                    step.getStatus()
                            .equalsIgnoreCase("PASSED")
            ) {

                html.append("<span class='status-passed'>PASSED</span>");

            } else {

                html.append("<span class='status-failed'>FAILED</span>");

            }

            html.append("</p>");

            html.append("<p><span class='label'>Duration:</span> ")
                    .append(step.getDurationMs())
                    .append(" ms</p>");

            html.append("<p><span class='label'>Locator Strategy:</span> ")
                    .append(step.getLocatorStrategy())
                    .append("</p>");

            html.append("<p><span class='label'>Executed At:</span> ")
                    .append(
                            step.getExecutedAt()
                                    .format(
                                            DateTimeFormatter.ofPattern(
                                                    "yyyy-MM-dd HH:mm:ss"
                                            )
                                    )
                    )
                    .append("</p>");

            /*
             * =====================================================
             * ERROR
             * =====================================================
             */

            if (
                    step.getErrorMessage() != null
                            &&
                            !step.getErrorMessage().isBlank()
            ) {

                html.append("<div class='error-box'>")
                        .append(step.getErrorMessage())
                        .append("</div>");

            }

            /*
             * =====================================================
             * SCREENSHOT
             * =====================================================
             */

            if (
                    step.getScreenshotPath() != null
                            &&
                            !step.getScreenshotPath().isBlank()
            ) {

                html.append("<div class='screenshot'>");

                html.append("<p><span class='label'>Screenshot:</span></p>");

                html.append("<img src='../")
                        .append(step.getScreenshotPath())
                        .append("' />");

                html.append("</div>");

            }

            html.append("</div>");

        }

        /*
         * =====================================================
         * HTML END
         * =====================================================
         */

        html.append("""
                </div>

                </body>

                </html>
                """);

        return html.toString();

    }

}