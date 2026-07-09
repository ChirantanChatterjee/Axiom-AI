package com.axiomai.ai.orchestrator;

import com.axiomai.ai.dto.AICommand;
import com.axiomai.ai.dto.AIResponse;
import com.axiomai.config.PublicBaseUrlResolver;
import com.axiomai.core.memory.ExecutionMemoryService;
import com.axiomai.qa.models.SiteMapResult;
import com.axiomai.qa.service.GeneratedTestExecutionService;
import com.axiomai.qa.service.WebsiteCrawlerService;
import com.axiomai.workspace.AutomationWorkspaceService;
import com.axiomai.workspace.GeneratedArtifact;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class AICommandOrchestratorTest {

    @Test
    void blocksSecondFrameworkWebsiteInSameChatSession() {

        WebsiteCrawlerService crawler =
                new GuardedCrawlerService();

        AutomationWorkspaceService workspaceService =
                new AutomationWorkspaceService();

        workspaceService.setWebsite(
                "chat-one",
                "https://saucedemo.com"
        );

        workspaceService.addArtifact(
                "chat-one",
                GeneratedArtifact.builder()
                        .name("framework.zip")
                        .type("FRAMEWORK")
                        .path("generated-frameworks/chat-one/framework.zip")
                        .downloadUrl("http://localhost:8080/api/workspace/artifacts/chat-one/framework.zip")
                        .build()
        );

        AICommandOrchestrator orchestrator =
                new AICommandOrchestrator(
                        null,
                        null,
                        null,
                        crawler,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new ExecutionMemoryService(),
                        workspaceService,
                        null,
                        null,
                        null,
                        new PublicBaseUrlResolver()
                );

        AIResponse response =
                orchestrator.execute(
                        AICommand.builder()
                                .intent("GENERATE_FRAMEWORK")
                                .url("https://example.com")
                                .userId("chat-one")
                                .build()
                );

        assertFalse(
                response.isSuccess()
        );

        assertEquals(
                "session_guard",
                response.getType()
        );

    }

    @Test
    void blocksFeatureGenerationForDifferentWebsiteInSameChatSession() {

        WebsiteCrawlerService crawler =
                new GuardedCrawlerService();

        AutomationWorkspaceService workspaceService =
                new AutomationWorkspaceService();

        workspaceService.setWebsite(
                "chat-one",
                "https://parabank.parasoft.com/parabank/index.htm"
        );

        workspaceService.addArtifact(
                "chat-one",
                GeneratedArtifact.builder()
                        .name("framework.zip")
                        .type("FRAMEWORK")
                        .path("generated-frameworks/chat-one/framework.zip")
                        .downloadUrl("http://localhost:8080/api/workspace/artifacts/chat-one/framework.zip")
                        .build()
        );

        AICommandOrchestrator orchestrator =
                new AICommandOrchestrator(
                        null,
                        null,
                        null,
                        crawler,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new ExecutionMemoryService(),
                        workspaceService,
                        null,
                        null,
                        null,
                        new PublicBaseUrlResolver()
                );

        AIResponse response =
                orchestrator.execute(
                        AICommand.builder()
                                .intent("GENERATE_FEATURE")
                                .featureName("bill pay")
                                .url("https://www.google.com")
                                .userId("chat-one")
                                .build()
                );

        assertFalse(
                response.isSuccess()
        );

        assertEquals(
                "session_guard",
                response.getType()
        );
    }

    @Test
    void blocksSecondFrameworkWhenOnlyPersistedArtifactContextIsKnown() {

        WebsiteCrawlerService crawler =
                new GuardedCrawlerService();

        AutomationWorkspaceService workspaceService =
                new AutomationWorkspaceService();

        workspaceService.addArtifact(
                "chat-one",
                GeneratedArtifact.builder()
                        .name("framework.zip")
                        .type("FRAMEWORK")
                        .path("generated-frameworks/chat-one/framework.zip")
                        .downloadUrl("http://localhost:8080/api/workspace/artifacts/chat-one/framework.zip")
                        .build()
        );

        AICommandOrchestrator orchestrator =
                new AICommandOrchestrator(
                        null,
                        null,
                        null,
                        crawler,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new ExecutionMemoryService(),
                        workspaceService,
                        null,
                        null,
                        null,
                        new PublicBaseUrlResolver()
                );

        AIResponse response =
                orchestrator.execute(
                        AICommand.builder()
                                .intent("GENERATE_FRAMEWORK")
                                .url("https://example.com")
                                .userId("chat-one")
                                .build()
                );

        assertFalse(
                response.isSuccess()
        );

        assertEquals(
                "session_guard",
                response.getType()
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void unknownIntentWithParsedVariablesUpdatesTestData() {

        AutomationWorkspaceService workspaceService =
                new AutomationWorkspaceService();

        AICommandOrchestrator orchestrator =
                new AICommandOrchestrator(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new ExecutionMemoryService(),
                        workspaceService,
                        null,
                        null,
                        null,
                        new PublicBaseUrlResolver()
                );

        AIResponse response =
                orchestrator.execute(
                        AICommand.builder()
                                .intent("UNKNOWN")
                                .variables(
                                        Map.of(
                                                "from",
                                                "New York",
                                                "to",
                                                "Sydney"
                                        )
                                )
                                .userId("chat-one")
                                .build()
                );

        assertTrue(
                response.isSuccess()
        );

        assertEquals(
                "variables",
                response.getType()
        );

        Map<String, String> variables =
                (Map<String, String>) response.getData();

        assertEquals(
                "New York",
                variables.get("from")
        );

        assertEquals(
                "Sydney",
                variables.get("to")
        );
    }

    @Test
    void resumesGeneratedTestExecutionAfterMissingRuntimeDataIsProvided() {

        AutomationWorkspaceService workspaceService =
                new AutomationWorkspaceService();

        StubGeneratedTestExecutionService generatedTestExecutionService =
                new StubGeneratedTestExecutionService();

        AICommandOrchestrator orchestrator =
                new AICommandOrchestrator(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        generatedTestExecutionService,
                        null,
                        null,
                        null,
                        null,
                        new ExecutionMemoryService(),
                        workspaceService,
                        null,
                        null,
                        null,
                        new PublicBaseUrlResolver()
                );

        AIResponse missing =
                orchestrator.execute(
                        AICommand.builder()
                                .intent("EXECUTE_GENERATED_TESTS")
                                .target("@login")
                                .userId("chat-one")
                                .build()
                );

        assertFalse(
                missing.isSuccess()
        );

        assertEquals(
                "missing-variables",
                missing.getType()
        );

        AIResponse resumed =
                orchestrator.execute(
                        AICommand.builder()
                                .intent("UPDATE_TEST_DATA")
                                .variables(
                                        Map.of(
                                                "username",
                                                "agileway"
                                        )
                                )
                                .userId("chat-one")
                                .build()
                );

        assertTrue(
                resumed.isSuccess()
        );

        assertEquals(
                "generated-test-execution",
                resumed.getType()
        );

        assertEquals(
                "@login",
                generatedTestExecutionService.lastRunTarget
        );
    }

    @Test
    void executeGeneratedAllRequestOverridesStaleFlowTagTarget() {

        AutomationWorkspaceService workspaceService =
                new AutomationWorkspaceService();

        StubGeneratedTestExecutionService generatedTestExecutionService =
                new StubGeneratedTestExecutionService();

        AICommandOrchestrator orchestrator =
                new AICommandOrchestrator(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        generatedTestExecutionService,
                        null,
                        null,
                        null,
                        null,
                        new ExecutionMemoryService(),
                        workspaceService,
                        null,
                        null,
                        null,
                        new PublicBaseUrlResolver()
                );

        AIResponse response =
                orchestrator.execute(
                        AICommand.builder()
                                .intent("EXECUTE_GENERATED_TESTS")
                                .target("@form_submission")
                                .message("Can you please run all the tests?")
                                .variables(
                                        Map.of(
                                                "username",
                                                "demo"
                                        )
                                )
                                .userId("chat-one")
                                .build()
                );

        assertTrue(
                response.isSuccess()
        );

        assertEquals(
                "ALL",
                generatedTestExecutionService.lastRunTarget
        );
    }

    @Test
    void executeGeneratedTagRequestOverridesStaleFlowTagTarget() {

        AutomationWorkspaceService workspaceService =
                new AutomationWorkspaceService();

        StubGeneratedTestExecutionService generatedTestExecutionService =
                new StubGeneratedTestExecutionService();

        AICommandOrchestrator orchestrator =
                new AICommandOrchestrator(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        generatedTestExecutionService,
                        null,
                        null,
                        null,
                        null,
                        new ExecutionMemoryService(),
                        workspaceService,
                        null,
                        null,
                        null,
                        new PublicBaseUrlResolver()
                );

        AIResponse response =
                orchestrator.execute(
                        AICommand.builder()
                                .intent("EXECUTE_GENERATED_TESTS")
                                .target("@form_submission")
                                .message("Can you please run all tests with @generated?")
                                .variables(
                                        Map.of(
                                                "username",
                                                "demo"
                                        )
                                )
                                .userId("chat-one")
                                .build()
                );

        assertTrue(
                response.isSuccess()
        );

        assertEquals(
                "@generated",
                generatedTestExecutionService.lastRunTarget
        );
    }

    private static class GuardedCrawlerService
            extends WebsiteCrawlerService {

        @Override
        public SiteMapResult crawl(
                String rootUrl
        ) {

            fail(
                    "Crawler should not run when the session guard blocks framework generation."
            );

            return null;
        }

        @Override
        public SiteMapResult crawl(
                String rootUrl,
                Map<String, String> variables
        ) {

            fail(
                    "Crawler should not run when the session guard blocks framework generation."
            );

            return null;
        }
    }

    private static class StubGeneratedTestExecutionService
            extends GeneratedTestExecutionService {

        private String lastRunTarget;

        private StubGeneratedTestExecutionService() {

            super(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        @Override
        public List<RuntimeVariableContext> missingRuntimeVariableContexts(
                String sessionId,
                String tagExpression,
                Map<String, String> variables
        ) {

            if (
                    variables != null
                            &&
                            variables.containsKey("username")
            ) {

                return List.of();
            }

            return List.of(
                    RuntimeVariableContext.builder()
                            .variable("username")
                            .feature("Login")
                            .scenario("User logs into application")
                            .step("When user enters \"${username}\" into \"username\"")
                            .hint("Value typed by this step.")
                            .build()
            );
        }

        @Override
        public GeneratedTestRunResult runTests(
                String sessionId,
                String tagExpression,
                Map<String, String> variables
        ) {

            lastRunTarget =
                    tagExpression;

            return GeneratedTestRunResult.builder()
                    .success(true)
                    .tagExpression(tagExpression)
                    .exitCode(0)
                    .message("Execution completed.")
                    .build();
        }
    }
}
