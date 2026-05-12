package com.axiomai.qa.generator;

import com.axiomai.qa.models.PageElement;
import com.axiomai.qa.models.PageScanResult;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PageObjectGenerator {

    // =====================================================
    // MAIN GENERATOR
    // =====================================================

    public String generate(PageScanResult scanResult) {

        String className =
                buildClassName(scanResult.getTitle())
                        + "Page";

        StringBuilder sb =
                new StringBuilder();

        sb.append("package com.axiomai.generated.pages;\n\n");

        sb.append("import com.microsoft.playwright.Page;\n");
        sb.append("import com.microsoft.playwright.Locator;\n\n");

        sb.append("public class ")
                .append(className)
                .append(" {\n\n");

        sb.append("    private final Page page;\n\n");

        // =================================================
        // LOCATORS
        // =================================================

        List<PageElement> elements =
                scanResult.getElements();

        for (PageElement element : elements) {

            if (!element.isTestCandidate()) {
                continue;
            }

            String fieldName =
                    buildFieldName(element);

            if (fieldName.isBlank()) {
                continue;
            }

            sb.append("    private final Locator ")
                    .append(fieldName)
                    .append(";\n");
        }

        sb.append("\n");

        // =================================================
        // CONSTRUCTOR
        // =================================================

        sb.append("    public ")
                .append(className)
                .append("(Page page) {\n\n");

        sb.append("        this.page = page;\n\n");

        for (PageElement element : elements) {

            if (!element.isTestCandidate()) {
                continue;
            }

            String fieldName =
                    buildFieldName(element);

            if (fieldName.isBlank()) {
                continue;
            }

            String selector =
                    escape(element.getCssSelector());

            sb.append("        this.")
                    .append(fieldName)
                    .append(" = page.locator(\"")
                    .append(selector)
                    .append("\");\n");
        }

        sb.append("    }\n\n");

        // =================================================
        // ACTION METHODS
        // =================================================

        for (PageElement element : elements) {

            if (!element.isTestCandidate()) {
                continue;
            }

            generateActionMethods(
                    sb,
                    element
            );
        }

        sb.append("}\n");

        return sb.toString();
    }

    // =====================================================
    // ACTION METHODS
    // =====================================================

    private void generateActionMethods(
            StringBuilder sb,
            PageElement element
    ) {

        String fieldName =
                buildFieldName(element);

        if (fieldName.isBlank()) {
            return;
        }

        String action =
                safe(element.getRecommendedAction())
                        .toUpperCase();

        // ================================================
        // TYPE METHOD
        // ================================================

        if (action.equals("TYPE")) {

            sb.append("    public void enter")
                    .append(capitalize(fieldName))
                    .append("(String value) {\n\n");

            sb.append("        ")
                    .append(fieldName)
                    .append(".fill(value);\n");

            sb.append("    }\n\n");
        }

        // ================================================
        // CLICK METHOD
        // ================================================

        if (action.equals("CLICK")) {

            sb.append("    public void click")
                    .append(capitalize(fieldName))
                    .append("() {\n\n");

            sb.append("        ")
                    .append(fieldName)
                    .append(".click();\n");

            sb.append("    }\n\n");
        }

        // ================================================
        // SELECT METHOD
        // ================================================

        if (action.equals("SELECT")) {

            sb.append("    public void select")
                    .append(capitalize(fieldName))
                    .append("(String value) {\n\n");

            sb.append("        ")
                    .append(fieldName)
                    .append(".selectOption(value);\n");

            sb.append("    }\n\n");
        }
    }

    // =====================================================
    // FIELD NAME
    // =====================================================

    private String buildFieldName(PageElement element) {

        String base =
                safe(element.getBusinessRole())
                        + "_"
                        + safe(element.getId())
                        + "_"
                        + safe(element.getName())
                        + "_"
                        + safe(element.getText());

        base = base
                .replaceAll("[^a-zA-Z0-9]", "_")
                .replaceAll("_+", "_")
                .toLowerCase();

        if (base.startsWith("_")) {
            base = base.substring(1);
        }

        if (base.endsWith("_")) {
            base = base.substring(0, base.length() - 1);
        }

        return base;
    }

    // =====================================================
    // CLASS NAME
    // =====================================================

    private String buildClassName(String title) {

        String cleaned =
                safe(title)
                        .replaceAll("[^a-zA-Z0-9]", " ");

        String[] parts =
                cleaned.split("\\s+");

        StringBuilder sb =
                new StringBuilder();

        for (String part : parts) {

            if (part.isBlank()) {
                continue;
            }

            sb.append(capitalize(part.toLowerCase()));
        }

        return sb.isEmpty()
                ? "Generated"
                : sb.toString();
    }

    // =====================================================
    // CAPITALIZE
    // =====================================================

    private String capitalize(String value) {

        if (value == null || value.isBlank()) {
            return "";
        }

        return value.substring(0, 1).toUpperCase()
                + value.substring(1);
    }

    // =====================================================
    // SAFE
    // =====================================================

    private String safe(String value) {

        return value == null
                ? ""
                : value;
    }

    // =====================================================
    // ESCAPE
    // =====================================================

    private String escape(String value) {

        return safe(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}