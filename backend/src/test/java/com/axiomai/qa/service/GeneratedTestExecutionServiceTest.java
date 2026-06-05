package com.axiomai.qa.service;

import com.axiomai.qa.generator.flow.FlowPageObjectGenerator;
import com.axiomai.workspace.SupabaseStorageCleanupService;
import com.axiomai.workspace.entity.GeneratedFrameworkArchiveEntity;
import com.axiomai.workspace.repository.GeneratedFrameworkArchiveRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void refreshesPersistedFrameworkBeforeParsingTagsWhenLocalCopyIsRunnableButStale() throws Exception {

        String sessionId =
                "stale-worker-framework-"
                        + UUID.randomUUID();

        GeneratedProjectWriterService writerService =
                writerService();

        Map<String, GeneratedFrameworkArchiveEntity> archiveStore =
                new HashMap<>();

        GeneratedFrameworkPersistenceService persistenceService =
                new GeneratedFrameworkPersistenceService(
                        writerService,
                        new SupabaseStorageCleanupService(
                                new ObjectMapper(),
                                "",
                                "",
                                "",
                                "generated-frameworks/"
                        ),
                        inMemoryArchiveRepository(archiveStore)
                );

        Path frameworkRoot =
                writerService.getFrameworkRoot(sessionId);

        Path featureRoot =
                frameworkRoot.resolve(
                        "src/test/resources/features"
                );

        Path addToCartFeature =
                featureRoot.resolve(
                        "add_to_cart.feature"
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
                            Feature: login

                            @generated @login
                            Scenario: User logs into application
                              Then flow should complete successfully
                            """
            );

            Files.writeString(
                    addToCartFeature,
                    """
                            Feature: add to cart

                            @generated @ai_requirement @add_to_cart
                            Scenario: User adds a product to cart after successful login
                              When user clicks "${product} add to cart"
                              Then cart should contain "${product}"
                            """
            );

            Path updatedArchive =
                    Path.of(
                            writerService.zipFramework(sessionId)
                    );

            archiveStore.put(
                    sessionId,
                    GeneratedFrameworkArchiveEntity.builder()
                            .sessionId(sessionId)
                            .archive(
                                    Files.readAllBytes(updatedArchive)
                            )
                            .archiveName("framework.zip")
                            .sizeBytes(
                                    Files.size(updatedArchive)
                            )
                            .updatedAt(
                                    Instant.now()
                                            .plusSeconds(60)
                            )
                            .build()
            );

            Files.deleteIfExists(addToCartFeature);

            assertFalse(
                    Files.exists(addToCartFeature)
            );

            GeneratedTestExecutionService service =
                    generatedTestExecutionService(
                            writerService,
                            persistenceService
                    );

            GeneratedTestExecutionService.GeneratedTestCatalog catalog =
                    service.listTags(sessionId);

            assertTrue(
                    catalog.getTags()
                            .stream()
                            .anyMatch(tag ->
                                    "@add_to_cart".equals(
                                            tag.getTag()
                                    )
                            )
            );

            assertTrue(
                    Files.exists(addToCartFeature)
            );

        } finally {

            writerService.deleteWorkspace(sessionId);
        }
    }

    private GeneratedTestExecutionService generatedTestExecutionService(
            GeneratedProjectWriterService writerService
    ) {

        return generatedTestExecutionService(
                writerService,
                null
        );
    }

    private GeneratedTestExecutionService generatedTestExecutionService(
            GeneratedProjectWriterService writerService,
            GeneratedFrameworkPersistenceService persistenceService
    ) {

        return new GeneratedTestExecutionService(
                writerService,
                persistenceService,
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

    private static GeneratedFrameworkArchiveRepository inMemoryArchiveRepository(
            Map<String, GeneratedFrameworkArchiveEntity> archiveStore
    ) {

        return (GeneratedFrameworkArchiveRepository) Proxy.newProxyInstance(
                GeneratedFrameworkArchiveRepository.class.getClassLoader(),
                new Class<?>[]{
                        GeneratedFrameworkArchiveRepository.class
                },
                (proxy, method, args) -> {

                    String methodName =
                            method.getName();

                    if (
                            "save".equals(methodName)
                    ) {

                        GeneratedFrameworkArchiveEntity entity =
                                (GeneratedFrameworkArchiveEntity) args[0];

                        archiveStore.put(
                                entity.getSessionId(),
                                entity
                        );

                        return entity;
                    }

                    if (
                            "findById".equals(methodName)
                    ) {

                        return java.util.Optional.ofNullable(
                                archiveStore.get(args[0])
                        );
                    }

                    if (
                            "deleteById".equals(methodName)
                    ) {

                        archiveStore.remove(args[0]);
                        return null;
                    }

                    if (
                            "toString".equals(methodName)
                    ) {

                        return "InMemoryGeneratedFrameworkArchiveRepository";
                    }

                    if (
                            "hashCode".equals(methodName)
                    ) {

                        return System.identityHashCode(proxy);
                    }

                    if (
                            "equals".equals(methodName)
                    ) {

                        return proxy == args[0];
                    }

                    throw new UnsupportedOperationException(
                            "Unexpected repository method: "
                                    + methodName
                    );
                }
        );
    }
}
