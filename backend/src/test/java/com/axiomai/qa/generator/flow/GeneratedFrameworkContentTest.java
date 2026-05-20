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
                hooks.contains("scenario.attach")
        );

        assertTrue(
                page.contains("resolveEditable(target)")
        );

        assertTrue(
                page.contains("inputSemanticSelectors")
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
