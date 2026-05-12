package com.axiomai.qa.generator.flow;

import com.axiomai.qa.flow.DetectedFlow;
import com.axiomai.qa.flow.FlowStep;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class FlowPageObjectGenerator {

    // =====================================================
    // MAIN GENERATOR
    // =====================================================

    public String generate(
            List<DetectedFlow> flows
    ) {

        StringBuilder sb =
                new StringBuilder();

        sb.append("package com.axiomai.generated.pages;\n\n");

        sb.append("import com.microsoft.playwright.*;\n");

        sb.append("import java.util.List;\n");
        sb.append("import java.util.Arrays;\n\n");

        sb.append("import com.axiomai.qa.runtime.SmartActionEngine;\n\n");

        sb.append("public class GeneratedPage {\n\n");

        sb.append("    private final Page page;\n\n");

        // =================================================
        // SELECTOR ARRAYS
        // =================================================

        Set<String> generatedFields =
                new HashSet<>();

        for (DetectedFlow flow : flows) {

            for (FlowStep step : flow.getSteps()) {

                String fieldName =
                        buildFieldName(step);

                if (
                        generatedFields.contains(fieldName)
                ) {

                    continue;
                }

                generatedFields.add(fieldName);

                sb.append("    private final List<String> ")
                        .append(fieldName)
                        .append("Selectors;\n");
            }
        }

        sb.append("\n");

        // =================================================
        // CONSTRUCTOR
        // =================================================

        sb.append("    public GeneratedPage(Page page) {\n\n");

        sb.append("        this.page = page;\n\n");

        generatedFields.clear();

        for (DetectedFlow flow : flows) {

            for (FlowStep step : flow.getSteps()) {

                String fieldName =
                        buildFieldName(step);

                if (
                        generatedFields.contains(fieldName)
                ) {

                    continue;
                }

                generatedFields.add(fieldName);

                sb.append("        this.")
                        .append(fieldName)
                        .append("Selectors = Arrays.asList(\n");

                sb.append("                \"")
                        .append(
                                escape(step.getSelector())
                        )
                        .append("\"");

                // =========================================
                // FALLBACKS
                // =========================================

                if (
                        step.getFallbackSelectors()
                                != null
                ) {

                    for (
                            String fallback
                            : step.getFallbackSelectors()
                    ) {

                        if (
                                fallback.equals(
                                        step.getSelector()
                                )
                        ) {

                            continue;
                        }

                        sb.append(",\n");

                        sb.append("                \"")
                                .append(
                                        escape(fallback)
                                )
                                .append("\"");
                    }
                }

                sb.append("\n");

                sb.append("        );\n\n");
            }
        }

        sb.append("    }\n\n");

        // =================================================
        // METHODS
        // =================================================

        generatedFields.clear();

        for (DetectedFlow flow : flows) {

            for (FlowStep step : flow.getSteps()) {

                String fieldName =
                        buildFieldName(step);

                if (
                        generatedFields.contains(fieldName)
                ) {

                    continue;
                }

                generatedFields.add(fieldName);

                generateMethod(
                        sb,
                        step,
                        fieldName
                );
            }
        }

        sb.append("}\n");

        return sb.toString();
    }

    // =====================================================
    // GENERATE METHOD
    // =====================================================

    private void generateMethod(

            StringBuilder sb,
            FlowStep step,
            String fieldName

    ) {

        String action =
                step.getAction();

        // =================================================
        // TYPE
        // =================================================

        if (
                "TYPE".equalsIgnoreCase(action)
        ) {

            sb.append("    public void ")
                    .append(fieldName)
                    .append("(String value) {\n\n");

            sb.append("        SmartActionEngine.type(\n");

            sb.append("                page,\n");

            sb.append("                ")
                    .append(fieldName)
                    .append("Selectors,\n");

            sb.append("                value\n");

            sb.append("        );\n");

            sb.append("    }\n\n");
        }

        // =================================================
        // CLICK
        // =================================================

        if (
                "CLICK".equalsIgnoreCase(action)
        ) {

            sb.append("    public void ")
                    .append(fieldName)
                    .append("() {\n\n");

            sb.append("        SmartActionEngine.click(\n");

            sb.append("                page,\n");

            sb.append("                ")
                    .append(fieldName)
                    .append("Selectors\n");

            sb.append("        );\n");

            sb.append("    }\n\n");
        }
    }

    // =====================================================
    // FIELD NAME
    // =====================================================

    private String buildFieldName(
            FlowStep step
    ) {

        return step.getAction()
                .toLowerCase()
                +
                "_"
                +
                step.getTarget()
                        .toLowerCase();
    }

    // =====================================================
    // ESCAPE
    // =====================================================

    private String escape(
            String value
    ) {

        if (value == null) {

            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}