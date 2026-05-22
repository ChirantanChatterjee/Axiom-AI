package com.axiomai.qa.generator.flow;

import com.axiomai.qa.flow.DetectedFlow;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FlowStepDefinitionGenerator {

    public static String generate(
            List<DetectedFlow> flows
    ) {

        return """
                package com.axiomai.generated.steps;

                import com.axiomai.generated.hooks.Hooks;
                import com.axiomai.generated.pages.GeneratedPage;
                import io.cucumber.java.en.*;

                public class GeneratedSteps {

                    private GeneratedPage generatedPage() {

                        return new GeneratedPage(Hooks.page);
                    }

                    @Given("user launches {string}")
                    public void userLaunches(String url) {

                        System.out.println("AIF STEP start: user launches " + url);
                        generatedPage().launch(resolveValue(url));
                        System.out.println("AIF STEP done: user launches " + url);
                    }

                    @When("user enters {string} into {string}")
                    public void userEntersInto(String value, String target) {

                        System.out.println("AIF STEP start: user enters into " + target);
                        generatedPage().enter(
                                resolveValue(target),
                                resolveValue(value)
                        );
                        System.out.println("AIF STEP done: user enters into " + target);
                    }

                    @When("user clicks {string}")
                    public void userClicks(String target) {

                        System.out.println("AIF STEP start: user clicks " + target);
                        generatedPage().click(
                                resolveValue(target)
                        );
                        System.out.println("AIF STEP done: user clicks " + target);
                    }

                    @Then("user should see {string}")
                    public void userShouldSee(String expectedText) {

                        System.out.println("AIF STEP start: user should see " + expectedText);
                        generatedPage().shouldSee(
                                resolveValue(expectedText)
                        );
                        System.out.println("AIF STEP done: user should see " + expectedText);
                    }

                    @Then("flow should complete successfully")
                    public void flowCompleted() {

                        System.out.println("Flow completed successfully");
                    }

                    private String resolveValue(String value) {

                        if (
                                value == null
                                        ||
                                        !value.startsWith("${")
                                        ||
                                        !value.endsWith("}")
                        ) {

                            return value;
                        }

                        String key = value.substring(2, value.length() - 1);
                        String systemValue = System.getProperty(key);

                        if (systemValue != null && !systemValue.isBlank()) {
                            return systemValue;
                        }

                        String envValue = System.getenv(key.toUpperCase());

                        if (envValue != null && !envValue.isBlank()) {
                            return envValue;
                        }

                        return value;
                    }
                }
                """;
    }
}
