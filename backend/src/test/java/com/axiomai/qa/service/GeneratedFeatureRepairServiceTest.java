package com.axiomai.qa.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedFeatureRepairServiceTest {

    @Test
    void rewritesWrongGeneratedClickTargetWhenUserProvidesActualActionLabel() {

        String feature =
                """
                        Feature: select flight

                        @select_flight @generated
                        Scenario: User successfully selects a return flight journey
                          Given user launches "https://travel.agileway.net/login"
                          And user clicks "search flights"
                        """;

        GeneratedFeatureRepairService.FeatureRepair repair =
                new GeneratedFeatureRepairService()
                        .repairFeatureContent(
                                feature,
                                "java.lang.RuntimeException: Unable to resolve element: search flights",
                                "There is no button present on the page as \"search flights\" it just says \"continue\"."
                        );

        assertTrue(
                repair.changed()
        );

        assertTrue(
                repair.content()
                        .contains("And user clicks \"continue\"")
        );

        assertEquals(
                0,
                repair.content()
                        .lines()
                        .filter(line -> line.contains("user clicks \"search flights\""))
                        .count()
        );
    }

    @Test
    void rewritesWrongGeneratedClickTargetFromObservedCrawlerAction() {

        String feature =
                """
                        Feature: select flight

                        @select_flight @generated
                        Scenario: User successfully selects a return flight journey
                          Given user launches "https://travel.agileway.net/login"
                          And user clicks "search flights"
                        """;

        GeneratedFeatureRepairService.FeatureRepair repair =
                new GeneratedFeatureRepairService()
                        .repairFeatureContent(
                                feature,
                                """
                                        java.lang.RuntimeException: Unable to resolve element: search flights
                                        ELEMENT -> TAG=INPUT | TEXT=Continue | TYPE=submit | NAME= | PLACEHOLDER= | ARIA= | ROLE=NEXT_BUTTON | SELECTOR=input[type='submit'][value='Continue']
                                        """,
                                ""
                        );

        assertTrue(
                repair.changed()
        );

        assertTrue(
                repair.content()
                        .contains("And user clicks \"continue\"")
        );
    }

    @Test
    void rewritesSauceDemoSortOptionClicksToSupportedDropdownValues() {

        String feature =
                """
                        Feature: form

                        @generated @ai_requirement @product_sorting
                        Scenario: Product list is sorted by name ascending when A-Z is selected
                          Given user launches "https://www.saucedemo.com"
                          And user clicks "A-Z"
                          Then product list should be sorted by "name ascending"

                        @generated @ai_requirement @product_sorting
                        Scenario: Product list is sorted by name descending when Z-A is selected
                          Given user launches "https://www.saucedemo.com"
                          And user clicks "Z-A"
                          Then product list should be sorted by "name descending"

                        @generated @ai_requirement @product_sorting
                        Scenario: Product list is sorted by price ascending when Price Low-High is selected
                          Given user launches "https://www.saucedemo.com"
                          And user clicks "Price Low-High"
                          Then product list should be sorted by "price ascending"

                        @generated @ai_requirement @product_sorting
                        Scenario: Product list is sorted by price descending when Price High-Low is selected
                          Given user launches "https://www.saucedemo.com"
                          And user clicks "Price High-Low"
                          Then product list should be sorted by "price descending"
                        """;

        GeneratedFeatureRepairService.FeatureRepair repair =
                new GeneratedFeatureRepairService()
                        .repairFeatureContent(
                                feature,
                                "java.lang.RuntimeException: Unable to resolve element: A-Z",
                                "The element text should be \"Name (A to Z)\""
                        );

        assertTrue(
                repair.changed()
        );

        assertTrue(
                repair.content()
                        .contains("And user enters \"az\" into \"sort\"")
        );

        assertTrue(
                repair.content()
                        .contains("And user enters \"za\" into \"sort\"")
        );

        assertTrue(
                repair.content()
                        .contains("And user enters \"lohi\" into \"sort\"")
        );

        assertTrue(
                repair.content()
                        .contains("And user enters \"hilo\" into \"sort\"")
        );

        assertEquals(
                0,
                repair.content()
                        .lines()
                        .filter(line -> line.contains("user clicks \"A-Z\""))
                        .count()
        );
    }

    @Test
    void rewritesGeneratedSelectStepToSupportedSortDropdownEntryStep() {

        String feature =
                """
                        Feature: form

                        @generated @ai_requirement @product_sorting
                        Scenario: Product list is sorted by name ascending when Name A to Z is selected
                          Given user launches "https://www.saucedemo.com"
                          And user selects "Name (A to Z)" from "sort"
                          Then product list should be sorted by "name ascending"
                        """;

        GeneratedFeatureRepairService.FeatureRepair repair =
                new GeneratedFeatureRepairService()
                        .repairFeatureContent(
                                feature,
                                "java.lang.RuntimeException: Undefined step: user selects"
                        );

        assertTrue(
                repair.changed()
        );

        assertTrue(
                repair.content()
                        .contains("And user enters \"az\" into \"sort\"")
        );

        assertEquals(
                0,
                repair.content()
                        .lines()
                        .filter(line -> line.contains("user selects"))
                        .count()
        );
    }

    @Test
    void rewritesSortClickFromUserProvidedActualOptionTextWhenOutputIsSparse() {

        String feature =
                """
                        Feature: generated

                        @generated @ai_requirement
                        Scenario: Sort by name ascending
                          Given user launches "https://www.saucedemo.com"
                          And user clicks "A-Z"
                          Then user should see "Products"
                        """;

        GeneratedFeatureRepairService.FeatureRepair repair =
                new GeneratedFeatureRepairService()
                        .repairFeatureContent(
                                feature,
                                "Generated test command finished with exit code 1",
                                "The element text should be \"Name (A to Z)\""
                        );

        assertTrue(
                repair.changed()
        );

        assertTrue(
                repair.content()
                        .contains("And user enters \"az\" into \"sort\"")
        );
    }

    @Test
    void repairsSelectFlightScenarioFlowAfterContinueAsPassengerDetailsPage() {

        String feature =
                """
                        Feature: select flight for return journey

                        @select_flight @generated @positive
                        Scenario: User successfully selects a return flight journey
                          Given user launches "https://travel.agileway.net/login"
                          When user enters "${username}" into "username"
                          And user enters "${password}" into "password"
                          And user clicks "login button"
                          And user clicks "return journey"
                          And user enters "New York" into "from"
                          And user enters "Sydney" into "to"
                          And user clicks "search flights"
                          Then user should see "Select your departure flight"
                          And user clicks "outbound flight"
                          And user clicks "return flight"
                          And user clicks "continue"
                          Then flow should complete successfully
                        """;

        GeneratedFeatureRepairService.FeatureRepair repair =
                new GeneratedFeatureRepairService()
                        .repairFeatureContent(
                                feature,
                                "java.lang.RuntimeException: Unable to resolve element: search flights",
                                "The actual button says \"continue\". After continue the page asks for passenger details - First Name and Last Name. Use First Name = Chirantan and Last Name = Chatterjee."
                        );

        assertTrue(
                repair.changed()
        );

        assertTrue(
                repair.content()
                        .contains("And user clicks \"next\"")
        );

        assertTrue(
                repair.content()
                        .contains("Then user should see \"First Name\"")
        );

        assertTrue(
                repair.content()
                        .contains("And user should see \"Last Name\"")
        );

        assertTrue(
                repair.content()
                        .contains("And user enters \"Chirantan\" into \"First Name\"")
        );

        assertTrue(
                repair.content()
                        .contains("And user enters \"Chatterjee\" into \"Last Name\"")
        );

        assertEquals(
                0,
                repair.content()
                        .lines()
                        .filter(line -> line.contains("search flights"))
                        .count()
        );

        assertEquals(
                0,
                repair.content()
                        .lines()
                        .filter(line -> line.contains("Select your departure flight"))
                        .count()
        );

        assertEquals(
                0,
                repair.content()
                        .lines()
                        .filter(line -> line.contains("outbound flight"))
                        .count()
        );

        assertEquals(
                0,
                repair.content()
                        .lines()
                        .filter(line -> line.contains("return flight\""))
                        .count()
        );
    }

    @Test
    void updatesExistingPassengerDetailsAndRemovesInvalidSelectFlightSteps() {

        String feature =
                """
                        Feature: select flight

                        @select_flight @generated @positive
                        Scenario: User successfully selects a return flight journey
                          Given user launches "https://travel.agileway.net/login"
                          When user clicks "continue"
                          Then user should see "Select your departure flight"
                          And user enters "John" into "First Name"
                          And user enters "Doe" into "Last Name"
                          And user clicks "outbound flight"
                        """;

        GeneratedFeatureRepairService.FeatureRepair repair =
                new GeneratedFeatureRepairService()
                        .repairFeatureContent(
                                feature,
                                "java.lang.AssertionError: Expected page to contain text: Select your departure flight",
                                "The page actually needs First Name and Last Name after continue. Use First Name = Chirantan and Last Name = Chatterjee."
                        );

        assertTrue(
                repair.changed()
        );

        assertTrue(
                repair.content()
                        .contains("And user enters \"Chirantan\" into \"First Name\"")
        );

        assertTrue(
                repair.content()
                        .contains("And user enters \"Chatterjee\" into \"Last Name\"")
        );

        assertTrue(
                repair.content()
                        .contains("And user clicks \"next\"")
        );

        assertEquals(
                0,
                repair.content()
                        .lines()
                        .filter(line -> line.contains("Select your departure flight"))
                        .count()
        );

        assertEquals(
                0,
                repair.content()
                        .lines()
                        .filter(line -> line.contains("outbound flight"))
                        .count()
        );
    }

    @Test
    void replacesPassengerDetailsContinueWithNextButton() {

        String feature =
                """
                        Feature: select flight

                        @select_flight @generated @positive
                        Scenario: User successfully selects a return flight journey
                          Given user launches "https://travel.agileway.net/login"
                          When user clicks "continue"
                          Then user should see "First Name"
                          And user should see "Last Name"
                          And user enters "Chirantan" into "First Name"
                          And user enters "Chatterjee" into "Last Name"
                          And user clicks "continue"
                        """;

        GeneratedFeatureRepairService.FeatureRepair repair =
                new GeneratedFeatureRepairService()
                        .repairFeatureContent(
                                feature,
                                "java.lang.RuntimeException: Unable to resolve element: continue",
                                "After clicking continue the page shows First Name and Last Name fields with a Next button. The gherkin repeats to click the continue button after entering first name and last name."
                        );

        assertTrue(
                repair.changed()
        );

        assertTrue(
                repair.content()
                        .contains("And user clicks \"next\"")
        );

        assertEquals(
                1,
                repair.content()
                        .lines()
                        .filter(line -> line.contains("user clicks \"continue\""))
                        .count()
        );
    }

    @Test
    void doesNotReplaceNonActionControlWithObservedContinueButton() {

        String feature =
                """
                        Feature: select flight

                        @select_flight @generated
                        Scenario: User successfully selects a return flight journey
                          Given user launches "https://travel.agileway.net/login"
                          And user clicks "return journey"
                        """;

        GeneratedFeatureRepairService.FeatureRepair repair =
                new GeneratedFeatureRepairService()
                        .repairFeatureContent(
                                feature,
                                """
                                        java.lang.RuntimeException: Unable to resolve element: return journey
                                        ELEMENT -> TAG=INPUT | TEXT=Continue | TYPE=submit | NAME= | PLACEHOLDER= | ARIA= | ROLE=NEXT_BUTTON | SELECTOR=input[type='submit'][value='Continue']
                                        """,
                                ""
                        );

        assertEquals(
                List.of(),
                repair.changes()
        );

        assertTrue(
                repair.content()
                        .contains("And user clicks \"return journey\"")
        );
    }

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
