package com.axiomai.qa.service;

import org.junit.jupiter.api.Test;

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
    void summarizesParaBankInvalidCredentialsAsRuntimeDataIssue() {

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
                summary.contains("runtime test-data issue")
        );

        assertTrue(
                summary.contains("valid ParaBank credentials")
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
