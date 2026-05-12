package com.axiomai.qa.generator.flow;

import com.axiomai.qa.flow.DetectedFlow;
import com.axiomai.qa.flow.FlowStep;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class FlowStepDefinitionGenerator {

    public String generate(
            List<DetectedFlow> flows
    ) {

        StringBuilder sb =
                new StringBuilder();

        sb.append("package com.axiomai.generated.steps;\n\n");

        sb.append("import io.cucumber.java.en.*;\n");
        sb.append("import com.microsoft.playwright.*;\n");
        sb.append("import com.axiomai.generated.pages.GeneratedPage;\n\n");

        sb.append("public class GeneratedSteps {\n\n");

        sb.append("    private static Playwright playwright;\n");
        sb.append("    private static Browser browser;\n");
        sb.append("    private static Page page;\n\n");

        sb.append("    private GeneratedPage generatedPage;\n\n");

        // =================================================
        // GIVEN
        // =================================================

        sb.append("    @Given(\"user launches {string}\")\n");

        sb.append("    public void userLaunches(String url) {\n\n");

        sb.append("        playwright = Playwright.create();\n\n");

        sb.append("        browser = playwright.chromium()\n");
        sb.append("                .launch(\n");
        sb.append("                        new BrowserType.LaunchOptions()\n");
        sb.append("                                .setHeadless(false)\n");
        sb.append("                );\n\n");

        sb.append("        page = browser.newPage();\n\n");

        sb.append("        page.navigate(url);\n\n");

        sb.append("        generatedPage = new GeneratedPage(page);\n");

        sb.append("    }\n\n");

        // =================================================
        // UNIQUE STEPS
        // =================================================

        Set<String> uniqueMethods =
                new HashSet<>();

        for (DetectedFlow flow : flows) {

            for (FlowStep step : flow.getSteps()) {

                String methodName =
                        step.getAction()
                                +
                                "_"
                                +
                                step.getTarget();

                if (uniqueMethods.contains(methodName)) {
                    continue;
                }

                uniqueMethods.add(methodName);

                generateStep(
                        sb,
                        step
                );
            }
        }

        // =================================================
        // ASSERTION
        // =================================================

        sb.append("    @Then(\"flow should complete successfully\")\n");

        sb.append("    public void flowCompleted() {\n\n");

        sb.append("        System.out.println(\"Flow completed successfully\");\n\n");

        sb.append("        browser.close();\n");
        sb.append("        playwright.close();\n");

        sb.append("    }\n\n");

        sb.append("}\n");

        return sb.toString();
    }

    // =====================================================
    // GENERATE STEP
    // =====================================================

    private void generateStep(
            StringBuilder sb,
            FlowStep step
    ) {

        String action =
                step.getAction();

        String target =
                step.getTarget();

        String methodName =
                action.toLowerCase()
                        +
                        "_"
                        +
                        target.toLowerCase();

        if ("TYPE".equalsIgnoreCase(action)) {

            sb.append("    @When(\"user enters {string} into ")
                    .append(target.toLowerCase())
                    .append("\")\n");

            sb.append("    public void ")
                    .append(methodName)
                    .append("(String value) {\n\n");

            sb.append("        generatedPage.")
                    .append(methodName)
                    .append("(value);\n");

            sb.append("    }\n\n");
        }

        if ("CLICK".equalsIgnoreCase(action)) {

            sb.append("    @When(\"user clicks ")
                    .append(target.toLowerCase())
                    .append("\")\n");

            sb.append("    public void ")
                    .append(methodName)
                    .append("() {\n\n");

            sb.append("        generatedPage.")
                    .append(methodName)
                    .append("();\n");

            sb.append("    }\n\n");
        }
    }
}