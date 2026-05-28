package com.axiomai.qa.generator.flow;

import com.axiomai.qa.flow.DetectedFlow;
import com.axiomai.qa.models.FlowStep;
import com.axiomai.qa.service.HookGeneratorService;
import org.junit.jupiter.api.Test;

import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedFrameworkContentTest {

    @Test
    void generatedFeatureContainsTagsAndGenericQuotedSteps() {

        String feature =
                new FlowFeatureGenerator()
                        .generate(
                                List.of(loginFlow())
                        );

        assertTrue(
                feature.contains("@generated @flow_login @login")
        );

        assertTrue(
                feature.contains("When user enters \"${username}\" into \"username\"")
        );

        assertTrue(
                feature.contains("And user clicks \"login button\"")
        );
    }

    @Test
    void authFieldTargetsUseUsernamePlaceholderAndLabel() {

        FlowStep authField =
                new FlowStep(
                        "TYPE",
                        "AUTH_FIELD",
                        "input[name='loginfmt']"
                );

        FlowStep passwordField =
                new FlowStep(
                        "TYPE",
                        "PASSWORD_FIELD",
                        "input[name='passwd']"
                );

        FlowStep login =
                new FlowStep(
                        "CLICK",
                        "LOGIN_BUTTON",
                        "#idSIButton9"
                );

        DetectedFlow flow =
                new DetectedFlow();

        flow.setFlowType("LOGIN");
        flow.setPageUrl("https://example.crm.dynamics.com");
        flow.setSteps(
                List.of(
                        authField,
                        passwordField,
                        login
                )
        );

        String feature =
                new FlowFeatureGenerator()
                        .generate(
                                List.of(flow)
                        );

        assertTrue(
                feature.contains("When user enters \"${username}\" into \"username\"")
        );

        assertTrue(
                feature.contains("And user enters \"${password}\" into \"password\"")
        );

        assertFalse(
                feature.contains("authfield")
        );
    }

    @Test
    void generatedJavaIsStandaloneAndAddsStepScreenshots() {

        String page =
                new FlowPageObjectGenerator()
                        .generate(
                                List.of(loginFlow())
                        );

        String steps =
                FlowStepDefinitionGenerator.generate(
                        List.of(loginFlow())
                );

        String hooks =
                new HookGeneratorService()
                        .generateHooks();

        assertFalse(
                page.contains("com.axiomai.qa.runtime")
        );

        assertFalse(
                steps.contains("AIActionExecutor")
        );

        assertTrue(
                hooks.contains("@AfterStep")
        );

        assertTrue(
                hooks.contains("resolveHeadless()")
        );

        assertTrue(
                hooks.contains("--no-sandbox")
        );

        assertFalse(
                hooks.contains("getOrDefault(\"AIF_BROWSER_CHANNEL\", \"chrome\")")
        );

        assertTrue(
                hooks.contains("scenario.attach")
        );

        assertTrue(
                page.contains("resolveEditable(target)")
        );

        assertTrue(
                page.contains("inputSemanticSelectors")
        );

        assertTrue(
                page.contains("resolveProductActionButton")
        );

        assertTrue(
                page.contains("actionPrefix + \"-sauce-labs-\"")
        );

        assertTrue(
                page.contains("lower.startsWith(\"add \")")
        );

        assertTrue(
                page.contains("resolveSubmitButton")
        );

        assertTrue(
                page.contains("input[name='payee.name']")
        );

        assertTrue(
                page.contains("confirmFieldIfNeeded(target, value)")
        );

        assertTrue(
                page.contains("waitForExpectedText(expectedText)")
        );

        assertTrue(
                page.contains("recordAssertionFailure(expectedText, body)")
        );

        assertTrue(
                page.contains("matchesHtmlValidation(expectedText)")
        );

        assertTrue(
                page.contains("expected.contains(\"amount validation\")")
        );

        assertTrue(
                page.contains("actual.contains(\"match\")")
        );

        assertTrue(
                page.contains("actual.contains(\"cannot be empty\")")
        );

        assertTrue(
                page.contains("handleSpecialClick(target)")
        );

        assertTrue(
                page.contains("resolveOptionControl(target)")
        );

        assertTrue(
                page.contains("input[type='radio'][value=")
        );

        assertTrue(
                page.contains("checkOrClickOption")
        );

        assertTrue(
                page.contains("optionTerms")
        );

        assertTrue(
                page.contains("handleGenericNavigationClick")
        );

        assertTrue(
                page.contains("dynamicNavigationSelectors")
        );

        assertTrue(
                page.contains("navigateToLikelyRoute")
        );

        assertTrue(
                page.contains("failFastOnAuthenticationError")
        );

        assertTrue(
                page.contains("page.goBack()")
        );

        assertTrue(
                page.contains("locator.selectOption")
        );

        assertTrue(
                page.contains("productListShouldBeSortedBy")
        );

        assertTrue(
                page.contains("checkoutTotalShouldEqualItemTotalPlusTax")
        );

        assertTrue(
                steps.contains("@Then(\"product list should be sorted by {string}\")")
        );

        assertTrue(
                steps.contains("@Then(\"checkout total should equal item total plus tax\")")
        );

        assertTrue(
                page.contains("input[name='customer.firstName']")
        );

        assertTrue(
                page.contains("input[name='repeatedPassword']")
        );

        assertTrue(
                page.contains("expected.contains(\"registration success\")")
        );

        assertTrue(
                page.contains("expected.contains(\"duplicate username\")")
        );

        assertTrue(
                page.contains("expected.contains(\"transfer confirmation\")")
        );

        assertTrue(
                page.contains("lower.startsWith(\"send \")")
        );
    }

    @Test
    void generatedPageObjectCompiles() throws Exception {

        String page =
                new FlowPageObjectGenerator()
                        .generate(
                                List.of(loginFlow())
                        );

        Path sourceRoot =
                Files.createTempDirectory(
                        "generated-page-compile"
                );

        Path packageRoot =
                sourceRoot.resolve(
                        "com/axiomai/generated/pages"
                );

        Files.createDirectories(packageRoot);

        Path source =
                packageRoot.resolve(
                        "GeneratedPage.java"
                );

        Files.writeString(
                source,
                page
        );

        int result =
                ToolProvider.getSystemJavaCompiler()
                        .run(
                                null,
                                null,
                                null,
                                "-classpath",
                                System.getProperty("java.class.path"),
                                source.toString()
                        );

        assertEquals(
                0,
                result
        );
    }

    private DetectedFlow loginFlow() {

        FlowStep username =
                new FlowStep(
                        "TYPE",
                        "USERNAME",
                        "[data-test='username']"
                );

        FlowStep password =
                new FlowStep(
                        "TYPE",
                        "PASSWORD",
                        "[data-test='password']"
                );

        FlowStep login =
                new FlowStep(
                        "CLICK",
                        "LOGIN_BUTTON",
                        "[data-test='login-button']"
                );

        DetectedFlow flow =
                new DetectedFlow();

        flow.setFlowType("LOGIN");
        flow.setPageUrl("https://www.saucedemo.com");
        flow.setSteps(
                List.of(
                        username,
                        password,
                        login
                )
        );

        return flow;
    }
}
