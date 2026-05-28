package com.axiomai.qa.service;

import com.axiomai.qa.generator.flow.FlowPageObjectGenerator;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedTestExecutionServiceTest {

    @Test
    void missingRuntimeVariablesAreScopedToMatchingScenarioTags() throws Exception {

        String sessionId =
                "runtime-vars-"
                        + UUID.randomUUID();

        GeneratedProjectWriterService writerService =
                writerService();

        Path frameworkRoot =
                writerService.getFrameworkRoot(sessionId);

        Path featureRoot =
                frameworkRoot.resolve(
                        "src/test/resources/features"
                );

        try {

            Files.createDirectories(featureRoot);

            Files.writeString(
                    frameworkRoot.resolve("pom.xml"),
                    "<project></project>"
            );

            Files.writeString(
                    featureRoot.resolve("generated.feature"),
                    """
                            Feature: generated

                            @generated @flow_login @login
                            Scenario: User logs into application
                              Given user launches "https://example.crm.dynamics.com"
                              When user enters "${username}" into "username"
                              And user enters "${password}" into "password"
                              Then flow should complete successfully

                            @generated @flow_form_submission @form_submission
                            Scenario: User submits form
                              Given user launches "https://example.crm.dynamics.com"
                              When user enters "${authfield}" into "auth field"
                              Then flow should complete successfully
                            """
            );

            GeneratedTestExecutionService service =
                    generatedTestExecutionService(writerService);

            List<String> missing =
                    service.missingRuntimeVariables(
                            sessionId,
                            "@login",
                            Map.of(
                                    "username",
                                    "user@example.com",
                                    "password",
                                    "secret"
                            )
                    );

            assertTrue(
                    missing.isEmpty()
            );

            List<String> missingWithLegacyAuthField =
                    service.missingRuntimeVariables(
                            sessionId,
                            "@login",
                            Map.of(
                                    "authfield",
                                    "user@example.com",
                                    "password",
                                    "secret"
                            )
                    );

            assertTrue(
                    missingWithLegacyAuthField.isEmpty()
            );

            List<String> formMissing =
                    service.missingRuntimeVariables(
                            sessionId,
                            "@form_submission",
                            Map.of()
                    );

            assertEquals(
                    List.of("username"),
                    formMissing
            );

            List<String> formMissingWithLegacyAuthField =
                    service.missingRuntimeVariables(
                            sessionId,
                            "@form_submission",
                            Map.of(
                                    "authfield",
                                    "user@example.com"
                            )
                    );

            assertTrue(
                    formMissingWithLegacyAuthField.isEmpty()
            );

        } finally {

            writerService.deleteWorkspace(sessionId);
        }
    }

    private GeneratedTestExecutionService generatedTestExecutionService(
            GeneratedProjectWriterService writerService
    ) {

        return new GeneratedTestExecutionService(
                writerService,
                null,
                null,
                new FlowPageObjectGenerator(),
                new HookGeneratorService(),
                new RunnerGeneratorService(),
                new PomGeneratorService(),
                null,
                new GeneratedFeatureRepairService(),
                null,
                null
        );
    }

    private GeneratedProjectWriterService writerService() {

        return new GeneratedProjectWriterService(
                new HookGeneratorService(),
                new RunnerGeneratorService(),
                new PomGeneratorService()
        );
    }
}
