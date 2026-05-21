package com.axiomai.ai.intent;

import com.axiomai.ai.dto.AICommand;
import com.axiomai.ai.model.GPTIntentResponse;
import com.axiomai.ai.planner.ScenarioPlanner;
import com.axiomai.ai.service.OpenAIIntentService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntentParserTest {

    @Test
    void generateFrameworkKeepsCredentialsFromFallbackCommand() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "generate framework for youtube.com where username is 27.chirantan@gmail.com and password is exampleSecret"
                );

        assertEquals(
                "GENERATE_FRAMEWORK",
                command.getIntent()
        );

        assertEquals(
                "https://youtube.com",
                command.getUrl()
        );

        assertEquals(
                "27.chirantan@gmail.com",
                command.getVariables()
                        .get("username")
        );

        assertEquals(
                "exampleSecret",
                command.getVariables()
                        .get("password")
        );
    }

    @Test
    void openAiExecuteFeatureTakesPrecedenceOverLocalScenarioDetection() {

        IntentParser parser =
                new IntentParser(
                        new StubOpenAIIntentService(
                                GPTIntentResponse.builder()
                                        .intent("EXECUTE_FEATURE")
                                        .featureName("product search")
                                        .build()
                        ),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "test product search"
                );

        assertEquals(
                "EXECUTE_FEATURE",
                command.getIntent()
        );

        assertEquals(
                "product search",
                command.getFeatureName()
        );
    }

    @Test
    void detectsGeneratedTestTagListingRequestBeforeOpenAi() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "Can you please provide me with the tags of the generated tests?"
                );

        assertEquals(
                "SHOW_GENERATED_TEST_TAGS",
                command.getIntent()
        );
    }

    @Test
    void detectsGeneratedTestExecutionByTag() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "Can you please run the tests with tag @checkout?"
                );

        assertEquals(
                "EXECUTE_GENERATED_TESTS",
                command.getIntent()
        );

        assertEquals(
                "@checkout",
                command.getTarget()
        );
    }

    @Test
    void detectsRunAllGeneratedTests() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "Can you please run all the generated tests?"
                );

        assertEquals(
                "EXECUTE_GENERATED_TESTS",
                command.getIntent()
        );

        assertEquals(
                "ALL",
                command.getTarget()
        );
    }

    @Test
    void detectsGeneratedTestRepairRequestBeforeOpenAi() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "The last test failed, can you look at it again and fix it?"
                );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                command.getIntent()
        );
    }

    private static class StubOpenAIIntentService
            extends OpenAIIntentService {

        private final GPTIntentResponse response;

        private StubOpenAIIntentService(
                GPTIntentResponse response
        ) {

            this.response = response;
        }

        @Override
        public GPTIntentResponse interpret(
                String userMessage
        ) {

            return response;
        }
    }
}
