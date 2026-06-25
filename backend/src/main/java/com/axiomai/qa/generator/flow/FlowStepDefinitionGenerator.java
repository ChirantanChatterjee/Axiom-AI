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
                        generatedPage().fillField(
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

                    @When("user presses {string}")
                    public void userPresses(String key) {

                        System.out.println("AIF STEP start: user presses " + key);
                        generatedPage().pressKey(
                                resolveValue(key)
                        );
                        System.out.println("AIF STEP done: user presses " + key);
                    }

                    @When("user presses {string} key")
                    public void userPressesKey(String key) {

                        userPresses(key);
                    }

                    @When("user refreshes page")
                    public void userRefreshesPage() {

                        System.out.println("AIF STEP start: user refreshes page");
                        generatedPage().refresh();
                        System.out.println("AIF STEP done: user refreshes page");
                    }

                    @Then("user should see {string}")
                    public void userShouldSee(String expectedText) {

                        System.out.println("AIF STEP start: user should see " + expectedText);
                        generatedPage().shouldSee(
                                resolveValue(expectedText)
                        );
                        System.out.println("AIF STEP done: user should see " + expectedText);
                    }

                    @Then("user should not see {string}")
                    public void userShouldNotSee(String unexpectedText) {

                        System.out.println("AIF STEP start: user should not see " + unexpectedText);
                        generatedPage().shouldNotSee(
                                resolveValue(unexpectedText)
                        );
                        System.out.println("AIF STEP done: user should not see " + unexpectedText);
                    }

                    @Then("product list should be sorted by {string}")
                    public void productListShouldBeSortedBy(String order) {

                        System.out.println("AIF STEP start: product list should be sorted by " + order);
                        generatedPage().productListShouldBeSortedBy(
                                resolveValue(order)
                        );
                        System.out.println("AIF STEP done: product list should be sorted by " + order);
                    }

                    @Then("cart badge should show {string}")
                    public void cartBadgeShouldShow(String count) {

                        System.out.println("AIF STEP start: cart badge should show " + count);
                        generatedPage().cartBadgeShouldShow(
                                resolveValue(count)
                        );
                        System.out.println("AIF STEP done: cart badge should show " + count);
                    }

                    @Then("cart should contain {string}")
                    public void cartShouldContain(String product) {

                        System.out.println("AIF STEP start: cart should contain " + product);
                        generatedPage().cartShouldContain(
                                resolveValue(product)
                        );
                        System.out.println("AIF STEP done: cart should contain " + product);
                    }

                    @Then("cart should not contain {string}")
                    public void cartShouldNotContain(String product) {

                        System.out.println("AIF STEP start: cart should not contain " + product);
                        generatedPage().cartShouldNotContain(
                                resolveValue(product)
                        );
                        System.out.println("AIF STEP done: cart should not contain " + product);
                    }

                    @Then("checkout total should equal item total plus tax")
                    public void checkoutTotalShouldEqualItemTotalPlusTax() {

                        System.out.println("AIF STEP start: checkout total should equal item total plus tax");
                        generatedPage().checkoutTotalShouldEqualItemTotalPlusTax();
                        System.out.println("AIF STEP done: checkout total should equal item total plus tax");
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
