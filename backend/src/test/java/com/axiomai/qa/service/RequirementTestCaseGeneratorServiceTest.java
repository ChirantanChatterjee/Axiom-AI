package com.axiomai.qa.service;

import com.axiomai.ai.service.OpenAIService;
import com.axiomai.qa.flow.DetectedFlow;
import com.axiomai.qa.models.GeneratedFramework;
import com.axiomai.qa.models.RequirementTestCase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequirementTestCaseGeneratorServiceTest {

    @Test
    void pastedRequirementDocumentGeneratesTestMatrixAndExecutableGherkin() {

        RequirementTestCaseGeneratorService service =
                new RequirementTestCaseGeneratorService(
                        new FailingOpenAIService(),
                        new StubFrameworkGeneratorService(),
                        new EmptyFrameworkLearningService()
                );

        GeneratedFramework framework =
                service.generate(
                        sampleRequirementDocument(),
                        "requirements",
                        "https://parabank.parasoft.com/parabank/index.htm",
                        List.of(new DetectedFlow()),
                        "chat-one"
                );

        List<RequirementTestCase> testCases =
                framework.getTestCases();

        assertFalse(
                testCases.isEmpty()
        );

        assertTrue(
                testCases.size() >= 20
        );

        assertEquals(
                "TC-001",
                testCases.get(0)
                        .getTcId()
        );

        assertEquals(
                "US-001",
                testCases.get(0)
                        .getUserStory()
        );

        assertTrue(
                testCases.stream()
                        .anyMatch(testCase ->
                                testCase.getScenario()
                                        .equals("Register without first name")
                        )
        );

        assertTrue(
                testCases.stream()
                        .anyMatch(testCase ->
                                testCase.getScenario()
                                        .equals("Pay bill with mismatched account verification")
                        )
        );

        assertTrue(
                testCases.stream()
                        .anyMatch(testCase ->
                                testCase.getUserStory()
                                        .equals("Security")
                        )
        );

        String feature =
                framework.getFeatureFile();

        assertTrue(
                feature.contains("@generated @ai_requirement @requirements @tc_001 @us_001")
        );

        assertTrue(
                feature.contains("Scenario: TC-001 Register with valid customer details")
        );

        assertTrue(
                feature.contains("And user enters \"${confirmPassword}\" into \"confirm password\"")
        );

        assertTrue(
                feature.contains("And user clicks \"browser back\"")
        );

        assertTrue(
                feature.contains("Then user should see \"Bill Payment Complete\"")
        );
    }

    @Test
    void unnumberedRequirementDocumentGeneratesGenericRequirementCases() {

        RequirementTestCaseGeneratorService service =
                new RequirementTestCaseGeneratorService(
                        new FailingOpenAIService(),
                        new StubFrameworkGeneratorService(),
                        new EmptyFrameworkLearningService()
                );

        GeneratedFramework framework =
                service.generate(
                        "User Story: Checkout As a shopper, I want to pay for my cart. Acceptance Criteria Valid card completes checkout. Invalid card shows an error.",
                        "requirements",
                        "https://example.test",
                        List.of(new DetectedFlow()),
                        "chat-one"
                );

        assertFalse(
                framework.getTestCases()
                        .isEmpty()
        );

        assertEquals(
                "REQ-001",
                framework.getTestCases()
                        .get(0)
                        .getUserStory()
        );

        assertTrue(
                framework.getFeatureFile()
                        .contains("@req_001")
        );
    }

    @Test
    void billPayFallbackGeneratesExpandedCoverage() {

        RequirementTestCaseGeneratorService service =
                new RequirementTestCaseGeneratorService(
                        null,
                        null,
                        null
                );

        String feature =
                service.fallbackFeature(
                        "Can you generate more tests for bill pay?",
                        "bill pay",
                        "https://parabank.parasoft.com/parabank/index.htm"
                );

        assertEquals(
                6,
                scenarioCount(feature)
        );

        assertTrue(
                feature.contains("@generated @ai_requirement @bill_pay @positive")
        );

        assertTrue(
                feature.contains("@required_field @negative")
        );

        assertTrue(
                feature.contains("@validation @negative")
        );

        assertTrue(
                feature.contains("@boundary")
        );

        assertTrue(
                feature.contains("And user enters \"${account}\" into \"verify account\"")
        );

        assertTrue(
                feature.contains("Then user should see \"amount validation error\"")
        );
    }

    @Test
    void billPayGenerationRejectsHallucinatedAiLaunchUrlWhenNoUrlIsExplicit() {

        RequirementTestCaseGeneratorService service =
                new RequirementTestCaseGeneratorService(
                        new StubOpenAIService(),
                        new StubFrameworkGeneratorService(),
                        new EmptyFrameworkLearningService()
                );

        GeneratedFramework framework =
                service.generate(
                        "Can you generate more tests for bill pay?",
                        "bill pay",
                        "",
                        List.of(new DetectedFlow()),
                        "chat-one"
                );

        String feature =
                framework.getFeatureFile();

        assertFalse(
                feature.contains("https://www.google.com")
        );

        assertTrue(
                feature.contains("https://parabank.parasoft.com/parabank/index.htm")
        );

        assertEquals(
                6,
                scenarioCount(feature)
        );
    }

    private int scenarioCount(
            String feature
    ) {

        int count =
                0;

        for (
                String line
                : feature.split("\\R")
        ) {

            if (
                    line.trim()
                            .startsWith("Scenario:")
            ) {

                count++;
            }
        }

        return count;
    }

    private static class StubOpenAIService
            extends OpenAIService {

        @Override
        public String ask(
                String prompt
        ) {

            return """
                    Feature: bill pay

                    @bill_pay @generated @ai_requirement
                    Scenario: AI hallucinated bill payment
                      Given user launches "https://www.google.com"
                      When user enters "${username}" into "username"
                      And user clicks "Bill Pay"
                      Then flow should complete successfully
                    """;
        }
    }

    private static class FailingOpenAIService
            extends OpenAIService {

        @Override
        public String ask(
                String prompt
        ) {

            throw new AssertionError(
                    "Requirement documents should be analyzed without calling OpenAI."
            );
        }
    }

    private static class StubFrameworkGeneratorService
            extends FrameworkGeneratorService {

        private StubFrameworkGeneratorService() {

            super(null);
        }

        @Override
        public GeneratedFramework generate(
                List<DetectedFlow> flows
        ) {

            return new GeneratedFramework(
                    "",
                    "page",
                    "steps"
            );
        }
    }

    private static class EmptyFrameworkLearningService
            extends FrameworkLearningService {

        private EmptyFrameworkLearningService() {

            super(null);
        }

        @Override
        public String learningSummary(
                String sessionId
        ) {

            return "";
        }
    }

    private String sampleRequirementDocument() {

        return """
                User Stories US-001: Customer Registration As a new banking customer, I want to register for online banking, so that I can access my accounts digitally.
                Acceptance Criteria User can open the registration page. User can submit valid personal details. User must provide matching password and confirm password. System creates the account successfully. User sees a registration success message.
                US-002: Mandatory Field Validation As a new customer, I want the system to validate required registration fields, so that incomplete customer profiles are not created.
                Acceptance Criteria Empty required fields show validation errors. User cannot register without first name, last name, address, city, state, zip code, phone, SSN, username, password, and confirmation password. Error messages are displayed near the form.
                US-003: Password Confirmation Validation As a new customer, I want the system to reject mismatched passwords, so that my account credentials are created correctly.
                Acceptance Criteria Password and confirm password must match. If they do not match, registration fails. User sees a clear error message.
                US-004: Existing User Login As a registered banking customer, I want to log in using my username and password, so that I can access account services.
                Acceptance Criteria Valid credentials allow login. Invalid credentials show an error. Logged-in user sees account services.
                US-005: Account Overview As a logged-in customer, I want to view my account overview, so that I can check my available accounts and balances.
                Acceptance Criteria Account overview page is accessible after login. Customer accounts are listed. Balance and available amount are displayed.
                US-006: Open New Account As a logged-in customer, I want to open a new checking or savings account, so that I can manage money separately.
                Acceptance Criteria User can select account type. User can select source account. New account is created successfully. New account number is displayed.
                US-007: Transfer Funds As a logged-in customer, I want to transfer money between my accounts, so that I can manage funds internally.
                Acceptance Criteria User can enter transfer amount. User can select from-account and to-account. Transfer confirmation is displayed. Transfer details show amount and account numbers.
                US-008: Bill Payment As a logged-in customer, I want to pay a bill online, so that I can send payments to a payee.
                Acceptance Criteria User can enter payee details. User can enter account number and verify account number. User can enter amount. Bill payment confirmation is displayed.
                US-009: Find Transactions As a logged-in customer, I want to search account transactions, so that I can review specific activity.
                Acceptance Criteria User can search by transaction ID, date, date range, or amount. Matching transactions are displayed. No-match scenario is handled gracefully.
                US-010: Logout As a logged-in customer, I want to log out securely, so that my banking session is closed.
                Acceptance Criteria Logout link is available. Clicking logout ends the session. User is returned to login page.
                """;
    }
}
