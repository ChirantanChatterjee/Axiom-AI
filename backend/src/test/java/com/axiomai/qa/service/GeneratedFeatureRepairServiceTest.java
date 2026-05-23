package com.axiomai.qa.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedFeatureRepairServiceTest {

    @Test
    void repairsParaBankBillPayGeneratedFeatureAfterLocatorFailure() {

        String feature =
                """
                        Feature: bill pay

                        @billpay @generated @ai_requirement
                        Scenario: Successful bill payment with valid information
                          Given user launches "https://parabank.parasoft.com/parabank/admin.htm"
                          When user enters "${payee}" into "payee name"
                          And user enters "${account}" into "account"
                          And user enters "${amount}" into "amount"
                          And user clicks "send payment button"
                          Then user should see "Bill Payment Complete"
                        """;

        GeneratedFeatureRepairService.FeatureRepair repair =
                new GeneratedFeatureRepairService()
                        .repairFeatureContent(
                                feature,
                                "Unable to resolve element: send payment button"
                        );

        assertTrue(
                repair.changed()
        );

        assertTrue(
                repair.content()
                        .contains("parabank/index.htm")
        );

        assertTrue(
                repair.content()
                        .contains("user clicks \"Bill Pay\"")
        );

        assertTrue(
                repair.content()
                        .contains("user enters \"${account}\" into \"verify account\"")
        );
    }

    @Test
    void repairsParaBankBillPayNavigationFailureWithMissingLogin() {

        String feature =
                """
                        Feature: bill pay

                        @bill_pay @generated
                        Scenario: Open bill pay
                          Given user launches "https://parabank.parasoft.com/parabank/admin.htm"
                          When user clicks "bill pay"
                          Then user should see "Bill Payment Service"
                        """;

        GeneratedFeatureRepairService.FeatureRepair repair =
                new GeneratedFeatureRepairService()
                        .repairFeatureContent(
                                feature,
                                "java.lang.RuntimeException: Unable to resolve element: bill pay"
                        );

        assertTrue(
                repair.changed()
        );

        assertTrue(
                repair.content()
                        .contains("parabank/index.htm")
        );

        assertTrue(
                repair.content()
                        .contains("user enters \"${username}\" into \"username\"")
        );

        assertTrue(
                repair.content()
                        .contains("user clicks \"login button\"")
        );

        assertTrue(
                repair.content()
                        .contains("user clicks \"Bill Pay\"")
        );
    }

    @Test
    void removesFailingIntermediateAssertionBeforeLaterActions() {

        String feature =
                """
                        Feature: bill pay

                        @bill_pay @generated
                        Scenario: Open bill pay
                          Given user launches "https://parabank.parasoft.com/parabank/index.htm"
                          When user enters "${username}" into "username"
                          And user enters "${password}" into "password"
                          And user clicks "login button"
                          And user clicks "Bill Pay"
                          Then user should see "Accounts Overview"
                          And user enters "${payee}" into "payee name"
                          Then user should see "Bill Payment Service"
                        """;

        GeneratedFeatureRepairService.FeatureRepair repair =
                new GeneratedFeatureRepairService()
                        .repairFeatureContent(
                                feature,
                                "java.lang.AssertionError: Expected page to contain text: Accounts Overview"
                        );

        assertTrue(
                repair.changed()
        );

        assertEquals(
                0,
                repair.content()
                        .lines()
                        .filter(line -> line.contains("Then user should see \"Accounts Overview\""))
                        .count()
        );

        assertTrue(
                repair.content()
                        .contains("And user enters \"${payee}\" into \"payee name\"")
        );
    }

    @Test
    void keepsFailingFinalAssertionAsBusinessFailure() {

        String feature =
                """
                        Feature: checkout

                        @checkout @generated
                        Scenario: Checkout
                          Given user launches "https://example.test"
                          When user clicks "checkout"
                          Then user should see "Order Complete"
                        """;

        GeneratedFeatureRepairService.FeatureRepair repair =
                new GeneratedFeatureRepairService()
                        .repairFeatureContent(
                                feature,
                                "java.lang.AssertionError: Expected page to contain text: Order Complete"
                        );

        assertEquals(
                List.of(),
                repair.changes()
        );

        assertTrue(
                repair.content()
                        .contains("Then user should see \"Order Complete\"")
        );
    }

    @Test
    void doesNotRewriteParaBankUrlForFinalValidationAssertionMismatch() {

        String feature =
                """
                        Feature: bill pay

                        @bill_pay @generated
                        Scenario: Bill pay with invalid amount
                          Given user launches "https://parabank.parasoft.com/parabank/admin.htm"
                          When user enters "INVALIDAMOUNT" into "amount"
                          Then user should see "amount validation error"
                        """;

        GeneratedFeatureRepairService.FeatureRepair repair =
                new GeneratedFeatureRepairService()
                        .repairFeatureContent(
                                feature,
                                "java.lang.AssertionError: Expected page to contain text: amount validation error"
                        );

        assertEquals(
                List.of(),
                repair.changes()
        );

        assertTrue(
                repair.content()
                        .contains("parabank/admin.htm")
        );
    }

    @Test
    void updatesAssertionTextWhenUserProvidesActualTextForSingleFailure() {

        String feature =
                """
                        Feature: bill pay

                        @bill_pay @generated
                        Scenario: Bill pay with mismatched verify account number
                          Given user launches "https://parabank.parasoft.com/parabank/admin.htm"
                          Then user should see "account mismatch error"
                        """;

        GeneratedFeatureRepairService.FeatureRepair repair =
                new GeneratedFeatureRepairService()
                        .repairFeatureContent(
                                feature,
                                "java.lang.AssertionError: Expected page to contain text: account mismatch error",
                                "This test failed because the assertion sentence actually was \"Please enter a valid number.\", can you please fix the generated test?"
                        );

        assertTrue(
                repair.changed()
        );

        assertTrue(
                repair.content()
                        .contains("Then user should see \"Please enter a valid number.\"")
        );

        assertEquals(
                List.of("Updated assertion text from \"account mismatch error\" to \"Please enter a valid number.\"."),
                repair.changes()
        );
    }

    @Test
    void doesNotGuessAssertionReplacementWhenMultipleFailuresShareOneActualText() {

        String feature =
                """
                        Feature: bill pay

                        @bill_pay @generated
                        Scenario: Bill pay with mismatched verify account number
                          Then user should see "account mismatch error"

                        @bill_pay @generated
                        Scenario: Bill pay with negative amount
                          Then user should see "amount validation error"
                        """;

        GeneratedFeatureRepairService.FeatureRepair repair =
                new GeneratedFeatureRepairService()
                        .repairFeatureContent(
                                feature,
                                """
                                Bill pay with mismatched verify account number <<< FAILURE!
                                java.lang.AssertionError: Expected page to contain text: account mismatch error
                                Bill pay with negative amount <<< FAILURE!
                                java.lang.AssertionError: Expected page to contain text: amount validation error
                                """,
                                "The test failed because the assertion sentence actually was \"Please enter a valid number.\", can you fix it?"
                        );

        assertEquals(
                List.of(),
                repair.changes()
        );

        assertTrue(
                repair.content()
                        .contains("Then user should see \"account mismatch error\"")
        );

        assertTrue(
                repair.content()
                        .contains("Then user should see \"amount validation error\"")
        );
    }

    @Test
    void updatesScenarioSpecificAssertionWhenMultipleFailuresExist() {

        String feature =
                """
                        Feature: bill pay

                        @bill_pay @generated
                        Scenario: Bill pay with mismatched verify account number
                          Then user should see "account mismatch error"

                        @bill_pay @generated
                        Scenario: Bill pay with negative amount
                          Then user should see "amount validation error"
                        """;

        GeneratedFeatureRepairService.FeatureRepair repair =
                new GeneratedFeatureRepairService()
                        .repairFeatureContent(
                                feature,
                                """
                                bill pay.Bill pay with mismatched verify account number <<< FAILURE!
                                java.lang.AssertionError: Expected page to contain text: account mismatch error
                                bill pay.Bill pay with negative amount <<< FAILURE!
                                java.lang.AssertionError: Expected page to contain text: amount validation error
                                """,
                                "In scenario \"Bill pay with mismatched verify account number\", the actual sentence is \"Please enter a valid number.\""
                        );

        assertTrue(
                repair.changed()
        );

        assertTrue(
                repair.content()
                        .contains("Then user should see \"Please enter a valid number.\"")
        );

        assertTrue(
                repair.content()
                        .contains("Then user should see \"amount validation error\"")
        );

        assertEquals(
                List.of("Updated assertion text from \"account mismatch error\" to \"Please enter a valid number.\"."),
                repair.changes()
        );
    }

    @Test
    void updatesAssertionTextWhenUserProvidesUnquotedActualSentenceFollowUp() {

        String feature =
                """
                        Feature: bill pay

                        @bill_pay @generated
                        Scenario: Bill pay with mismatched verify account number
                          Then user should see "account mismatch error"
                        """;

        GeneratedFeatureRepairService.FeatureRepair repair =
                new GeneratedFeatureRepairService()
                        .repairFeatureContent(
                                feature,
                                "java.lang.AssertionError: Expected page to contain text: account mismatch error",
                                "The actual sentence is --> Please enter a valid number."
                        );

        assertTrue(
                repair.changed()
        );

        assertTrue(
                repair.content()
                        .contains("Then user should see \"Please enter a valid number.\"")
        );
    }

    @Test
    void summarizesExpectedTextAssertionFailures() {

        String summary =
                new GeneratedFeatureRepairService()
                        .failureSummary(
                                """
                                bill pay.Successful bill payment with valid information <<< FAILURE!
                                java.lang.AssertionError: Expected page to contain text: Bill Payment Complete
                                bill pay.Bill payment fails with invalid amount <<< FAILURE!
                                java.lang.AssertionError: Expected page to contain text: Invalid amount
                                """
                        );

        assertEquals(
                "The last generated test failed because the page did not show expected text: Bill Payment Complete, Invalid amount.",
                summary
        );
    }

    @Test
    void treatsSurefireFailureLinesAsRecognizedOutput() {

        String summary =
                new GeneratedFeatureRepairService()
                        .failureSummary(
                                "Tests run: 5, Failures: 2, Errors: 0, Skipped: 1 <<< FAILURE!"
                        );

        assertTrue(
                summary.contains("Failures: 2")
        );
    }

    @Test
    void providesSpecificGuidanceForMultipleAssertionFailures() {

        String guidance =
                new GeneratedFeatureRepairService()
                        .assertionMismatchRepairGuidance(
                                """
                                bill pay.Bill pay with mismatched verify account number <<< FAILURE!
                                java.lang.AssertionError: Expected page to contain text: account mismatch error
                                bill pay.Bill pay with negative amount <<< FAILURE!
                                java.lang.AssertionError: Expected page to contain text: amount validation error
                                """
                        );

        assertTrue(
                guidance.contains("\"Bill pay with mismatched verify account number\" expected \"account mismatch error\"")
        );

        assertTrue(
                guidance.contains("\"Bill pay with negative amount\" expected \"amount validation error\"")
        );

        assertTrue(
                guidance.contains("replace assertion \"account mismatch error\" with")
        );
    }

    @Test
    void summarizesAuthenticationFailuresAsRuntimeDataIssue() {

        String summary =
                new GeneratedFeatureRepairService()
                        .failureSummary(
                                """
                                java.lang.AssertionError: Expected page to contain text: Accounts Overview
                                Body:
                                Error!
                                The username and password could not be verified.
                                """
                        );

        assertTrue(
                summary.contains("did not complete authentication")
        );

        assertTrue(
                summary.contains("valid credentials")
        );
    }

    @Test
    void summarizesPlaywrightBrowserStartupFailuresAsRuntimeIssue() {

        String summary =
                new GeneratedFeatureRepairService()
                        .failureSummary(
                                """
                                java.lang.RuntimeException: Failed to create driver
                                Caused by: java.lang.RuntimeException: Failed to install browsers
                                """
                        );

        assertTrue(
                summary.contains("browser-runtime issue")
        );
    }
}
