package com.axiomai.qa.service;

import com.axiomai.ml.FailureClassificationLabel;
import com.axiomai.ml.RepairRecommendationLabel;
import com.axiomai.qa.runtime.SmartLocatorResolver;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedLocatorRepairAnalyzerTest {

    @Test
    void classifiesWrongTextboxComplaintAsLocatorMismatch() {

        String evidence =
                "The generated test typed into the wrong textbox and action-evidence.json contains intendedFieldName.";

        assertEquals(
                FailureClassificationLabel.LOCATOR_MISMATCH,
                GeneratedLocatorRepairAnalyzer.classifyFailure(evidence)
        );

        assertEquals(
                RepairRecommendationLabel.REPAIR_LOCATORS_WITH_RUNTIME_EVIDENCE,
                GeneratedLocatorRepairAnalyzer.recommendedRepair(evidence)
        );
    }

    @Test
    void detectsGuidedRepairInstructionTypes() {

        assertEquals(
                "FIELD_LOCATOR",
                GeneratedRepairInstructionAnalyzer.guidedRepairInstruction(
                                "the field locator used for \"Email\" field is incorrect can you please fix it?"
                        )
                        .orElseThrow()
                        .repairArea()
        );

        assertEquals(
                "ASSERTION_EXPECTATION",
                GeneratedRepairInstructionAnalyzer.guidedRepairInstruction(
                                "The assertion is incorrect because the actual expectation should be \"Bill Payment Complete\""
                        )
                        .orElseThrow()
                        .repairArea()
        );

        assertEquals(
                "STEP_REMOVAL",
                GeneratedRepairInstructionAnalyzer.guidedRepairInstruction(
                                "The step \"Old Button\" is invalid can you please remove it?"
                        )
                        .orElseThrow()
                        .repairArea()
        );
    }

    @Test
    void scoresSemanticElementMetadataByIntendedField() {

        double multipleScore =
                GeneratedLocatorRepairAnalyzer.semanticScore(
                        "Multiple Color Names",
                        Map.of(
                                "id",
                                "autoCompleteMultipleInput",
                                "ariaLabel",
                                "Multiple Color Names"
                        )
                );

        double singleScore =
                GeneratedLocatorRepairAnalyzer.semanticScore(
                        "Multiple Color Names",
                        Map.of(
                                "id",
                                "autoCompleteSingleInput",
                                "ariaLabel",
                                "Single Color Name"
                        )
                );

        assertTrue(
                multipleScore > singleScore
        );
    }

    @Test
    void runtimeResolverScoresSemanticElementMetadataByIntendedField() {

        assertTrue(
                SmartLocatorResolver.semanticScore(
                        "Multiple Color Names",
                        Map.of(
                                "id",
                                "autoCompleteMultipleInput",
                                "ariaLabel",
                                "Multiple Color Names"
                        )
                )
                        >
                        SmartLocatorResolver.semanticScore(
                                "Multiple Color Names",
                                Map.of(
                                        "id",
                                        "autoCompleteSingleInput",
                                        "ariaLabel",
                                        "Single Color Name"
                                )
                        )
        );
    }

    @Test
    void detectsWeakBroadInputLocators() {

        assertTrue(
                GeneratedLocatorRepairAnalyzer.weakLocatorFindings(
                                "GeneratedPage.java",
                                """
                                        Locator locator = page.locator("input").first();
                                        Locator visible = firstVisible("input:visible");
                                        """
                        )
                        .size() >= 2
        );
    }
}
