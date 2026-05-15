package com.axiomai.qa.generator.flow;

import com.axiomai.qa.flow.DetectedFlow;
import com.axiomai.qa.models.FlowStep;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FlowStepDefinitionGenerator {

    // =====================================================
    // GENERATE STEP DEFINITIONS
    // =====================================================

    public static String generate(

            List<DetectedFlow> flows

    ) {

        StringBuilder sb =
                new StringBuilder();

        sb.append(
                "package com.axiomai.generated.steps;\n\n"
        );

        sb.append(
                "import io.cucumber.java.en.*;\n"
        );

        sb.append(
                "import com.microsoft.playwright.*;\n"
        );

        sb.append(
                "import com.axiomai.generated.pages.GeneratedPage;\n"
        );

        sb.append(
                "import com.axiomai.qa.runtime.AIActionExecutor;\n"
        );

        sb.append(
                "import com.axiomai.qa.models.FlowStep;\n\n"
        );

        sb.append(
                "public class GeneratedSteps {\n\n"
        );

        // =================================================
        // PLAYWRIGHT
        // =================================================

        sb.append(
                "    private static Playwright playwright;\n"
        );

        sb.append(
                "    private static Browser browser;\n"
        );

        sb.append(
                "    private static Page page;\n\n"
        );

        sb.append(
                "    private GeneratedPage generatedPage;\n\n"
        );

        // =================================================
        // GIVEN
        // =================================================

        sb.append(
                "    @Given(\"user launches {string}\")\n"
        );

        sb.append(
                "    public void userLaunches(String url) {\n\n"
        );

        sb.append(
                "        playwright = Playwright.create();\n\n"
        );

        sb.append(
                "        browser = playwright.chromium()\n"
        );

        sb.append(
                "                .launch(\n"
        );

        sb.append(
                "                        new BrowserType.LaunchOptions()\n"
        );

        sb.append(
                "                                .setHeadless(false)\n"
        );

        sb.append(
                "                );\n\n"
        );

        sb.append(
                "        page = browser.newPage();\n\n"
        );

        sb.append(
                "        page.navigate(url);\n\n"
        );

        sb.append(
                "        generatedPage = new GeneratedPage(page);\n"
        );

        sb.append(
                "    }\n\n"
        );

        // =================================================
        // GENERATE STEPS
        // =================================================

        for (DetectedFlow flow : flows) {

            for (FlowStep step : flow.getSteps()) {

                generateStepMethod(
                        sb,
                        step
                );
            }
        }

        // =================================================
        // THEN
        // =================================================

        sb.append(
                "    @Then(\"flow should complete successfully\")\n"
        );

        sb.append(
                "    public void flowCompleted() {\n\n"
        );

        sb.append(
                "        System.out.println(\"Flow completed successfully\");\n\n"
        );

        sb.append(
                "        browser.close();\n"
        );

        sb.append(
                "        playwright.close();\n"
        );

        sb.append(
                "    }\n\n"
        );

        sb.append("}\n");

        return sb.toString();
    }

    // =====================================================
    // GENERATE STEP METHOD
    // =====================================================

    private static void generateStepMethod(

            StringBuilder sb,
            FlowStep step

    ) {

        String methodName =
                buildMethodName(step);

        String selector =
                escape(step.getSelector());

        // =================================================
        // TYPE STEP
        // =================================================

        if (

                step.getAction()
                        .equalsIgnoreCase("TYPE")

        ) {

            sb.append(
                    "    @When(\"user enters {string} into "
                            + step.getTarget().toLowerCase()
                            + "\")\n"
            );

            sb.append(
                    "    public void "
                            + methodName
                            + "(String value) {\n\n"
            );

            sb.append(
                    "        FlowStep step = new FlowStep();\n"
            );

            sb.append(
                    "        step.setSelector(\""
                            + selector
                            + "\");\n"
            );

            sb.append(
                    "        AIActionExecutor.type(page, step, value);\n"
            );

            sb.append(
                    "    }\n\n"
            );
        }

        // =================================================
        // CLICK STEP
        // =================================================

        if (

                step.getAction()
                        .equalsIgnoreCase("CLICK")

        ) {

            sb.append(
                    "    @When(\"user clicks "
                            + step.getTarget().toLowerCase()
                            + "\")\n"
            );

            sb.append(
                    "    public void "
                            + methodName
                            + "() {\n\n"
            );

            sb.append(
                    "        FlowStep step = new FlowStep();\n"
            );

            sb.append(
                    "        step.setSelector(\""
                            + selector
                            + "\");\n"
            );

            sb.append(
                    "        AIActionExecutor.click(page, step);\n"
            );

            sb.append(
                    "    }\n\n"
            );
        }
    }

    // =====================================================
    // METHOD NAME
    // =====================================================

    private static String buildMethodName(
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

    private static String escape(
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