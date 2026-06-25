package com.axiomai.ai.intent;

import com.axiomai.ai.dto.AICommand;
import com.axiomai.ai.model.GPTIntentResponse;
import com.axiomai.ai.planner.ScenarioPlanner;
import com.axiomai.ai.service.OpenAIIntentService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void detectsGiveGeneratedTestTagsRequestBeforeOpenAi() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "give me the tags of generated tests"
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
    void detectsMultipleGeneratedTestTagsAsUnion() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "Can you please run the tests with tags @abc and @def and @sdsd?"
                );

        assertEquals(
                "EXECUTE_GENERATED_TESTS",
                command.getIntent()
        );

        assertEquals(
                "@abc or @def or @sdsd",
                command.getTarget()
        );
    }

    @Test
    void supportsExplicitGeneratedTestTagIntersection() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "Run generated tests matching all tags @smoke and @checkout"
                );

        assertEquals(
                "EXECUTE_GENERATED_TESTS",
                command.getIntent()
        );

        assertEquals(
                "@smoke and @checkout",
                command.getTarget()
        );
    }

    @Test
    void parsesColonSeparatedRuntimeValuesWithoutOpenAi() {

        IntentParser parser =
                new IntentParser(
                        new UnexpectedOpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "From: New York To: Sydney"
                );

        assertEquals(
                "UPDATE_TEST_DATA",
                command.getIntent()
        );

        assertEquals(
                "New York",
                command.getVariables()
                        .get("from")
        );

        assertEquals(
                "Sydney",
                command.getVariables()
                        .get("to")
        );
    }

    @Test
    void parsesEqualsSeparatedRuntimeValuesWithoutOpenAi() {

        IntentParser parser =
                new IntentParser(
                        new UnexpectedOpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "from = New York, To = Sydney"
                );

        assertEquals(
                "UPDATE_TEST_DATA",
                command.getIntent()
        );

        assertEquals(
                "New York",
                command.getVariables()
                        .get("from")
        );

        assertEquals(
                "Sydney",
                command.getVariables()
                        .get("to")
        );
    }

    @Test
    void parsesNaturalLanguageRuntimeValuesWithoutOpenAi() {

        IntentParser parser =
                new IntentParser(
                        new UnexpectedOpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "from is New York and to is Sydney"
                );

        assertEquals(
                "UPDATE_TEST_DATA",
                command.getIntent()
        );

        assertEquals(
                "New York",
                command.getVariables()
                        .get("from")
        );

        assertEquals(
                "Sydney",
                command.getVariables()
                        .get("to")
        );
    }

    @Test
    void parsesFirstNameAndLastnameRuntimeValuesWithoutOpenAi() {

        IntentParser parser =
                new IntentParser(
                        new UnexpectedOpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "First Name is Chirantan and Lastname is Chatterjee"
                );

        assertEquals(
                "UPDATE_TEST_DATA",
                command.getIntent()
        );

        assertEquals(
                "Chirantan",
                command.getVariables()
                        .get("firstName")
        );

        assertEquals(
                "Chatterjee",
                command.getVariables()
                        .get("lastName")
        );
    }

    @Test
    void doesNotTreatFailureExplanationWithThereIsAsRuntimeData() {

        IntentParser parser =
                new IntentParser(
                        new UnexpectedOpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "The tests failed because there are no \"Select your departure flight\" fields after clicking continue. There is only First Name and Last Name field along with a Next button. Hence the gherkin are invalid."
                );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                command.getIntent()
        );
    }

    @Test
    void parsesGenericRuntimePlaceholderValuesWithoutOpenAi() {

        IntentParser parser =
                new IntentParser(
                        new UnexpectedOpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "departDay is 12 and returnMonth is June 2026"
                );

        assertEquals(
                "UPDATE_TEST_DATA",
                command.getIntent()
        );

        assertEquals(
                "12",
                command.getVariables()
                        .get("departDay")
        );

        assertEquals(
                "June 2026",
                command.getVariables()
                        .get("returnMonth")
        );
    }

    @Test
    void detectsGenerateThenRunAsCompoundCommand() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "can you create tests for register and then run the test for bill pay?"
                );

        assertEquals(
                "COMPOUND_COMMAND",
                command.getIntent()
        );

        assertNotNull(
                command.getCommands()
        );

        assertEquals(
                2,
                command.getCommands()
                        .size()
        );

        AICommand generate =
                command.getCommands()
                        .get(0);

        assertEquals(
                "GENERATE_FEATURE",
                generate.getIntent()
        );

        assertEquals(
                "registration",
                generate.getFeatureName()
        );

        AICommand execute =
                command.getCommands()
                        .get(1);

        assertEquals(
                "EXECUTE_GENERATED_TESTS",
                execute.getIntent()
        );

        assertEquals(
                "(@bill_pay or @billpay)",
                execute.getTarget()
        );
    }

    @Test
    void detectsGeneratedTestExecutionBySpacedAtTag() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "Can you run tests with tag @ registration"
                );

        assertEquals(
                "EXECUTE_GENERATED_TESTS",
                command.getIntent()
        );

        assertEquals(
                "@registration",
                command.getTarget()
        );
    }

    @Test
    void detectsGeneratedTestExecutionByPlainTagName() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "Can you run tests with tag registration"
                );

        assertEquals(
                "EXECUTE_GENERATED_TESTS",
                command.getIntent()
        );

        assertEquals(
                "@registration",
                command.getTarget()
        );
    }

    @Test
    void detectsGeneratedTestExecutionForRegisterFeaturePhraseBeforeOpenAi() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "can you run tests for register a user?"
                );

        assertEquals(
                "EXECUTE_GENERATED_TESTS",
                command.getIntent()
        );

        assertEquals(
                "(@register or @registration)",
                command.getTarget()
        );
    }

    @Test
    void detectsGeneratedTestExecutionForGenericFeaturePhrase() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "run tests for account transfer"
                );

        assertEquals(
                "EXECUTE_GENERATED_TESTS",
                command.getIntent()
        );

        assertEquals(
                "@account_transfer",
                command.getTarget()
        );
    }

    @Test
    void detectsBillPayGeneratedTestExecution() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "can you run the bill pay tests?"
                );

        assertEquals(
                "EXECUTE_GENERATED_TESTS",
                command.getIntent()
        );

        assertEquals(
                "(@bill_pay or @billpay)",
                command.getTarget()
        );
    }

    @Test
    void detectsBillPayNegativeGeneratedTestExecution() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "run the negative bill pay tests"
                );

        assertEquals(
                "EXECUTE_GENERATED_TESTS",
                command.getIntent()
        );

        assertEquals(
                "(@bill_pay or @billpay) and @negative",
                command.getTarget()
        );
    }

    @Test
    void explicitMultipleTagsUseAndByDefault() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "run generated tests tagged @bill_pay @negative"
                );

        assertEquals(
                "EXECUTE_GENERATED_TESTS",
                command.getIntent()
        );

        assertEquals(
                "@bill_pay and @negative",
                command.getTarget()
        );
    }

    @Test
    void detectsGenerateMoreBillPayTestsAsFeatureGeneration() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "Can you generate more tests for bill pay?"
                );

        assertEquals(
                "GENERATE_FEATURE",
                command.getIntent()
        );

        assertEquals(
                "bill pay",
                command.getFeatureName()
        );
    }

    @Test
    void detectsPastedRequirementDocumentAsFeatureGeneration() {

        IntentParser parser =
                new IntentParser(
                        new FailingOpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "User Stories US-001: Customer Registration As a new customer, I want to register. Acceptance Criteria User can open the registration page. User sees a success message."
                );

        assertEquals(
                "GENERATE_FEATURE",
                command.getIntent()
        );

        assertEquals(
                "requirements",
                command.getFeatureName()
        );
    }

    @Test
    void detectsUnnumberedRequirementDocumentAsFeatureGeneration() {

        IntentParser parser =
                new IntentParser(
                        new FailingOpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "User Story: Checkout As a shopper, I want to pay for my cart. Acceptance Criteria Valid card completes checkout. Invalid card shows an error."
                );

        assertEquals(
                "GENERATE_FEATURE",
                command.getIntent()
        );
    }

    @Test
    void featureGenerationDoesNotUseHallucinatedOpenAiUrl() {

        IntentParser parser =
                new IntentParser(
                        new StubOpenAIIntentService(
                                GPTIntentResponse.builder()
                                        .intent("GENERATE_FEATURE")
                                        .featureName("bill pay")
                                        .url("https://www.google.com")
                                        .build()
                        ),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "Can you generate more tests for bill pay?"
                );

        assertEquals(
                "GENERATE_FEATURE",
                command.getIntent()
        );

        assertEquals(
                null,
                command.getUrl()
        );
    }

    @Test
    void detectsBillPaymentEdgeCaseGeneration() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "add edge cases for bill payment"
                );

        assertEquals(
                "GENERATE_FEATURE",
                command.getIntent()
        );

        assertEquals(
                "bill pay",
                command.getFeatureName()
        );
    }

    @Test
    void extractsBillPayRuntimeValuesFromChat() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "payee is ACME Utilities and amount is 10 and zip code is 12345 and account is 98765"
                );

        assertEquals(
                "UPDATE_TEST_DATA",
                command.getIntent()
        );

        assertEquals(
                "ACME Utilities",
                command.getVariables()
                        .get("payee")
        );

        assertEquals(
                "10",
                command.getVariables()
                        .get("amount")
        );

        assertEquals(
                "12345",
                command.getVariables()
                        .get("zip")
        );

        assertEquals(
                "98765",
                command.getVariables()
                        .get("account")
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
    void detectsConversationalGeneratedTestRerunBeforeOpenAi() {

        IntentParser parser =
                new IntentParser(
                        new UnexpectedOpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "can you rerun the test for me?"
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
    void detectsGeneratedTestUpdateBeforeOpenAi() {

        IntentParser parser =
                new IntentParser(
                        new UnexpectedOpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "Update the generated tests to include Product Listing, Product Sorting, and Product Details scenarios."
                );

        assertEquals(
                "GENERATE_FEATURE",
                command.getIntent()
        );

        assertEquals(
                "generated",
                command.getFeatureName()
        );
    }

    @Test
    void detectsRequirementBackedAddTestsRequestAsFeatureGeneration() {

        IntentParser parser =
                new IntentParser(
                        new UnexpectedOpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        """
                                Please add tests for

                                Product Listing - The inventory page must display all products with name, price, image, and description.

                                Product Sorting - Sorting options (A-Z, Z-A, Price Low-High, Price High-Low) must reorder items correctly.

                                Product Details - Clicking a product must open a detail page with accurate information
                                """
                );

        assertEquals(
                "GENERATE_FEATURE",
                command.getIntent()
        );

        assertEquals(
                "generated",
                command.getFeatureName()
        );
    }

    @Test
    void detectsAddTestsForFailureBehaviorAsFeatureGeneration() {

        IntentParser parser =
                new IntentParser(
                        new UnexpectedOpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "Add tests for failed login error message validation."
                );

        assertEquals(
                "GENERATE_FEATURE",
                command.getIntent()
        );
    }

    @Test
    void doesNotTreatRuntimeTestDataUpdateAsGeneratedTestUpdate() {

        IntentParser parser =
                new IntentParser(
                        new UnexpectedOpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "update test data username is standard_user and password is secret_sauce"
                );

        assertEquals(
                "UPDATE_TEST_DATA",
                command.getIntent()
        );

        assertEquals(
                "standard_user",
                command.getVariables()
                        .get("username")
        );
    }

    @Test
    void detectsGeneratedTestRepairRequestBeforeOpenAi() {

        IntentParser parser =
                new IntentParser(
                        new UnexpectedOpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand genericCommand =
                parser.parse(
                        "The last test failed can you please fix it"
                );

        AICommand detailedCommand =
                parser.parse(
                        "The last test failed, can you look at it again and fix it?"
                );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                genericCommand.getIntent()
        );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                detailedCommand.getIntent()
        );
    }

    @Test
    void detectsDirectGeneratedRepairCommandsBeforeOpenAi() {

        IntentParser parser =
                new IntentParser(
                        new UnexpectedOpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand fixCommand =
                parser.parse(
                        "fix the test for me"
                );

        AICommand healCommand =
                parser.parse(
                        "please heal this"
                );

        AICommand resolveCommand =
                parser.parse(
                        "resolve the last generated test"
                );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                fixCommand.getIntent()
        );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                healCommand.getIntent()
        );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                resolveCommand.getIntent()
        );
    }

    @Test
    void detectsUsernamePasswordFieldMismatchAsGeneratedRepairRequest() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "The username field is filled with password value can you please resolve this?"
                );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                command.getIntent()
        );

        assertTrue(
                command.getVariables()
                        .isEmpty()
        );
    }

    @Test
    void doesNotSaveFieldComplaintAsGenericTestData() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "The last test failed as the username field is getting filled with value provided for password Can you please rectify this?"
                );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                command.getIntent()
        );

        assertTrue(
                command.getVariables()
                        .isEmpty()
        );
    }

    @Test
    void detectsIncorrectUsernameComplaintAsRepairWithoutSavingEnteredVariable() {

        IntentParser parser =
                new IntentParser(
                        new UnexpectedOpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "The username entered is incorrect can you please correct it?"
                );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                command.getIntent()
        );

        assertTrue(
                command.getVariables()
                        .isEmpty()
        );
    }

    @Test
    void detectsNaturalGeneratedFailureRepairRequestBeforeOpenAi() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "I see some failures can you please fix it?"
                );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                command.getIntent()
        );
    }

    @Test
    void detectsAssertionCorrectionRepairRequestBeforeOpenAi() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "This test failed because the assertion sentence actually was \"abc example\", can you please fix the generated test?"
                );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                command.getIntent()
        );
    }

    @Test
    void detectsActualSentenceFollowUpAsGeneratedRepairRequestBeforeOpenAi() {

        IntentParser parser =
                new IntentParser(
                        new OpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand unquotedCommand =
                parser.parse(
                        "The actual sentence is --> Please enter a valid number."
                );

        AICommand quotedCommand =
                parser.parse(
                        "The actual sentence is \"Please enter a valid number\"."
                );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                unquotedCommand.getIntent()
        );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                quotedCommand.getIntent()
        );
    }

    @Test
    void detectsGuidedGeneratedRepairInstructionsBeforeOpenAi() {

        IntentParser parser =
                new IntentParser(
                        new UnexpectedOpenAIIntentService(),
                        new ScenarioPlanner()
                );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                parser.parse(
                                "the field locator used for \"Email\" field is incorrect can you please fix it?"
                        )
                        .getIntent()
        );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                parser.parse(
                                "The assertion is incorrect because the actual expectation should be \"Bill Payment Complete\""
                        )
                        .getIntent()
        );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                parser.parse(
                                "The step \"Old Button\" is invalid can you please remove it?"
                        )
                        .getIntent()
        );
    }

    @Test
    void detectsElementTextCorrectionAsGeneratedRepairRequestBeforeOpenAi() {

        IntentParser parser =
                new IntentParser(
                        new UnexpectedOpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand expectedTextCommand =
                parser.parse(
                        "The expected element text should be \"Name (A to Z)\"."
                );

        AICommand elementTextCommand =
                parser.parse(
                        "The element text should be \"Name (A to Z)\""
                );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                expectedTextCommand.getIntent()
        );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                elementTextCommand.getIntent()
        );

        assertTrue(
                expectedTextCommand.getVariables()
                        .isEmpty()
        );
    }

    @Test
    void treatsNaturalSortElementHintAsGeneratedRepairInsteadOfRuntimeData() {

        IntentParser parser =
                new IntentParser(
                        new UnexpectedOpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "The element is Name (A to Z) and Name (Z to A)"
                );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                command.getIntent()
        );

        assertTrue(
                command.getVariables()
                        .isEmpty()
        );
    }

    @Test
    void detectsDiagnosticFailureCommandsBeforeOpenAi() {

        IntentParser parser =
                new IntentParser(
                        new UnexpectedOpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "Can you diagnose why the last generated test failed?"
                );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                command.getIntent()
        );
    }

    @Test
    void detectsLearnedRepairCommandsBeforeOpenAi() {

        IntentParser parser =
                new IntentParser(
                        new UnexpectedOpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "Use what you learned and make this test pass"
                );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                command.getIntent()
        );
    }

    @Test
    void detectsRepairThenRerunAsCompoundCommand() {

        IntentParser parser =
                new IntentParser(
                        new UnexpectedOpenAIIntentService(),
                        new ScenarioPlanner()
                );

        AICommand command =
                parser.parse(
                        "fix the last generated test and then rerun the same test"
                );

        assertEquals(
                "COMPOUND_COMMAND",
                command.getIntent()
        );

        assertEquals(
                2,
                command.getCommands()
                        .size()
        );

        assertEquals(
                "REPAIR_GENERATED_TESTS",
                command.getCommands()
                        .get(0)
                        .getIntent()
        );

        assertEquals(
                "EXECUTE_GENERATED_TESTS",
                command.getCommands()
                        .get(1)
                        .getIntent()
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

    private static class FailingOpenAIIntentService
            extends OpenAIIntentService {

        @Override
        public GPTIntentResponse interpret(
                String userMessage
        ) {

            throw new AssertionError(
                    "Requirement documents should be detected before OpenAI intent parsing."
            );
        }
    }

    private static class UnexpectedOpenAIIntentService
            extends OpenAIIntentService {

        @Override
        public GPTIntentResponse interpret(
                String userMessage
        ) {

            throw new AssertionError(
                    "Runtime variable replies should be parsed without OpenAI intent inference."
            );
        }
    }
}
