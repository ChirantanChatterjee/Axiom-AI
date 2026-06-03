package com.axiomai.ai.service;

import com.axiomai.ai.dto.AICommand;
import com.axiomai.ai.dto.AIResponse;
import com.axiomai.ai.intent.IntentParser;
import com.axiomai.ai.model.GPTIntentResponse;
import com.axiomai.ai.orchestrator.AICommandOrchestrator;
import com.axiomai.ai.planner.ScenarioPlanner;
import com.axiomai.config.PublicBaseUrlResolver;
import com.axiomai.core.memory.ExecutionMemoryService;
import com.axiomai.workspace.AutomationWorkspaceService;
import com.axiomai.workspace.GeneratedArtifact;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AIOrchestratorServiceTest {

    @Test
    void vagueRerunTestRequestUsesLastGeneratedTestTag() {

        AutomationWorkspaceService workspaceService =
                new AutomationWorkspaceService();

        workspaceService.setLastGeneratedTestExecution(
                "chat-one",
                "@login"
        );

        CapturingAICommandOrchestrator orchestrator =
                new CapturingAICommandOrchestrator();

        AIOrchestratorService service =
                service(
                        orchestrator,
                        workspaceService
                );

        service.processMessage(
                "can you rerun the test",
                "chat-one"
        );

        assertEquals(
                "EXECUTE_GENERATED_TESTS",
                orchestrator.capturedCommand.getIntent()
        );

        assertEquals(
                "@login",
                orchestrator.capturedCommand.getTarget()
        );
    }

    @Test
    void vagueRunTestRequestUsesGeneratedTestsWhenFrameworkExists() {

        AutomationWorkspaceService workspaceService =
                new AutomationWorkspaceService();

        workspaceService.addArtifact(
                "chat-one",
                GeneratedArtifact.builder()
                        .type("FRAMEWORK")
                        .name("framework.zip")
                        .path("generated-frameworks/chat-one/framework.zip")
                        .build()
        );

        CapturingAICommandOrchestrator orchestrator =
                new CapturingAICommandOrchestrator();

        AIOrchestratorService service =
                service(
                        orchestrator,
                        workspaceService
                );

        service.processMessage(
                "can you rerun the test",
                "chat-one"
        );

        assertEquals(
                "EXECUTE_GENERATED_TESTS",
                orchestrator.capturedCommand.getIntent()
        );

        assertEquals(
                "ALL",
                orchestrator.capturedCommand.getTarget()
        );
    }

    @Test
    void openAiGeneratedTestRerunIntentUsesLastGeneratedTestTagInsteadOfSentenceTarget() {

        AutomationWorkspaceService workspaceService =
                new AutomationWorkspaceService();

        workspaceService.setLastGeneratedTestExecution(
                "chat-one",
                "@product_sorting"
        );

        CapturingAICommandOrchestrator orchestrator =
                new CapturingAICommandOrchestrator();

        AIOrchestratorService service =
                service(
                        orchestrator,
                        workspaceService,
                        new StaticOpenAIIntentService(
                                GPTIntentResponse.builder()
                                        .intent("EXECUTE_GENERATED_TESTS")
                                        .build()
                        )
                );

        service.processMessage(
                "can you rerun the test?",
                "chat-one"
        );

        assertEquals(
                "EXECUTE_GENERATED_TESTS",
                orchestrator.capturedCommand.getIntent()
        );

        assertEquals(
                "@product_sorting",
                orchestrator.capturedCommand.getTarget()
        );
    }

    @Test
    void rerunForMeStillUsesLastGeneratedTestTag() {

        AutomationWorkspaceService workspaceService =
                new AutomationWorkspaceService();

        workspaceService.setLastGeneratedTestExecution(
                "chat-one",
                "@product_sorting"
        );

        CapturingAICommandOrchestrator orchestrator =
                new CapturingAICommandOrchestrator();

        AIOrchestratorService service =
                service(
                        orchestrator,
                        workspaceService
                );

        service.processMessage(
                "can you rerun the test for me?",
                "chat-one"
        );

        assertEquals(
                "EXECUTE_GENERATED_TESTS",
                orchestrator.capturedCommand.getIntent()
        );

        assertEquals(
                "@product_sorting",
                orchestrator.capturedCommand.getTarget()
        );
    }

    @Test
    void unknownNaturalRepairCommandUsesGeneratedTestRepairWhenFrameworkExists() {

        AutomationWorkspaceService workspaceService =
                new AutomationWorkspaceService();

        workspaceService.addArtifact(
                "chat-one",
                GeneratedArtifact.builder()
                        .type("FRAMEWORK")
                        .name("framework.zip")
                        .path("generated-frameworks/chat-one/framework.zip")
                        .build()
        );

        CapturingAICommandOrchestrator orchestrator =
                new CapturingAICommandOrchestrator();

        AIOrchestratorService service =
                service(
                        orchestrator,
                        workspaceService,
                        new StaticOpenAIIntentService(
                                GPTIntentResponse.builder()
                                        .intent("UNKNOWN")
                                        .build()
                        )
                );

        service.processMessage(
                "please heal this",
                "chat-one"
        );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                orchestrator.capturedCommand.getIntent()
        );
    }

    @Test
    void generatedTestIntentWithBlankTargetIsNotReplacedByActiveFlow() {

        AutomationWorkspaceService workspaceService =
                new AutomationWorkspaceService();

        workspaceService.setActiveFlow(
                "chat-one",
                "login"
        );

        CapturingAICommandOrchestrator orchestrator =
                new CapturingAICommandOrchestrator();

        AIOrchestratorService service =
                service(
                        orchestrator,
                        workspaceService,
                        new StaticOpenAIIntentService(
                                GPTIntentResponse.builder()
                                        .intent("EXECUTE_GENERATED_TESTS")
                                        .build()
                        )
                );

        service.processMessage(
                "run generated tests",
                "chat-one"
        );

        assertEquals(
                "EXECUTE_GENERATED_TESTS",
                orchestrator.capturedCommand.getIntent()
        );

        assertEquals(
                "ALL",
                orchestrator.capturedCommand.getTarget()
        );
    }

    @Test
    void explicitFlowRerunStillUsesFlowExecution() {

        AutomationWorkspaceService workspaceService =
                new AutomationWorkspaceService();

        workspaceService.setLastGeneratedTestExecution(
                "chat-one",
                "@login"
        );

        CapturingAICommandOrchestrator orchestrator =
                new CapturingAICommandOrchestrator();

        AIOrchestratorService service =
                service(
                        orchestrator,
                        workspaceService
                );

        service.processMessage(
                "can you rerun the login flow",
                "chat-one"
        );

        assertNotEquals(
                "EXECUTE_GENERATED_TESTS",
                orchestrator.capturedCommand.getIntent()
        );
    }

    private AIOrchestratorService service(

            CapturingAICommandOrchestrator orchestrator,
            AutomationWorkspaceService workspaceService

    ) {

        return new AIOrchestratorService(
                new IntentParser(
                        new StubOpenAIIntentService(),
                        new ScenarioPlanner()
                ),
                orchestrator,
                new ExecutionMemoryService(),
                workspaceService
        );
    }

    private AIOrchestratorService service(

            CapturingAICommandOrchestrator orchestrator,
            AutomationWorkspaceService workspaceService,
            OpenAIIntentService openAIIntentService

    ) {

        return new AIOrchestratorService(
                new IntentParser(
                        openAIIntentService,
                        new ScenarioPlanner()
                ),
                orchestrator,
                new ExecutionMemoryService(),
                workspaceService
        );
    }

    private static class CapturingAICommandOrchestrator
            extends AICommandOrchestrator {

        private AICommand capturedCommand;

        private CapturingAICommandOrchestrator() {

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
                    null,
                    null,
                    new ExecutionMemoryService(),
                    new AutomationWorkspaceService(),
                    null,
                    null,
                    null,
                    new PublicBaseUrlResolver()
            );
        }

        @Override
        public AIResponse execute(
                AICommand command
        ) {

            capturedCommand =
                    command;

            return AIResponse.builder()
                    .success(true)
                    .type(
                            command.getIntent()
                    )
                    .build();
        }
    }

    private static class StubOpenAIIntentService
            extends OpenAIIntentService {

        @Override
        public GPTIntentResponse interpret(
                String userMessage
        ) {

            return GPTIntentResponse.builder()
                    .intent("UNKNOWN")
                    .build();
        }
    }

    private static class StaticOpenAIIntentService
            extends OpenAIIntentService {

        private final GPTIntentResponse response;

        private StaticOpenAIIntentService(
                GPTIntentResponse response
        ) {

            this.response =
                    response;
        }

        @Override
        public GPTIntentResponse interpret(
                String userMessage
        ) {

            return response;
        }
    }
}
