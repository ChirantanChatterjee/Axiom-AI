package com.axiomai.ai.orchestrator;

import com.axiomai.ai.dto.AICommand;
import com.axiomai.ai.dto.AIResponse;
import com.axiomai.config.PublicBaseUrlResolver;
import com.axiomai.core.memory.ExecutionMemoryService;
import com.axiomai.qa.models.SiteMapResult;
import com.axiomai.qa.service.WebsiteCrawlerService;
import com.axiomai.workspace.AutomationWorkspaceService;
import com.axiomai.workspace.GeneratedArtifact;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    }
}
