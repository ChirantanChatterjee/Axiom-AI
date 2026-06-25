package com.axiomai.ml;

import com.axiomai.ml.config.AIFMLProperties;
import com.axiomai.qa.service.GeneratedLocatorRepairAnalyzer;
import org.springframework.stereotype.Service;

@Service
public class RepairRecommendationModelService
        extends AbstractWekaTextClassificationModelService<RepairRecommendationLabel> {

    public RepairRecommendationModelService(
            AIFModelRegistryService modelRegistryService,
            MLFeatureExtractor featureExtractor,
            AIFMLProperties properties,
            MLConfidenceDecisionService confidenceDecisionService
    ) {

        super(
                modelRegistryService,
                featureExtractor,
                properties,
                confidenceDecisionService
        );
    }

    @Override
    public String modelName() {

        return AIFMLModelNames.REPAIR_RECOMMENDATION;
    }

    @Override
    protected Class<RepairRecommendationLabel> labelClass() {

        return RepairRecommendationLabel.class;
    }

    @Override
    protected RepairRecommendationLabel defaultLabel() {

        return RepairRecommendationLabel.ESCALATE_TO_OPENAI;
    }

    @Override
    protected MLPrediction heuristicPrediction(
            String input
    ) {

        if (
                GeneratedLocatorRepairAnalyzer.hasLocatorMismatchEvidence(
                        input
                )
        ) {

            return prediction(
                    RepairRecommendationLabel.REPAIR_LOCATORS_WITH_RUNTIME_EVIDENCE,
                    0.91,
                    "heuristic"
            );
        }

        if (
                containsAny(
                        input,
                        "undefined step",
                        "undefined steps",
                        "undefined scenario",
                        "undefined scenarios",
                        "you can implement missing steps",
                        "undefinedstepexception"
                )
        ) {

            return prediction(
                    RepairRecommendationLabel.ESCALATE_TO_OPENAI,
                    0.79,
                    "heuristic"
            );
        }

        if (
                containsAny(
                        input,
                        "popup",
                        "modal",
                        "overlay",
                        "cookie banner",
                        "dialog"
                )
        ) {

            return prediction(
                    RepairRecommendationLabel.HANDLE_POPUP,
                    0.83,
                    "heuristic"
            );
        }

        if (
                containsAny(
                        input,
                        "unable to resolve element",
                        "unable to locate element",
                        "locator failed",
                        "element not found",
                        "selector"
                )
        ) {

            return prediction(
                    RepairRecommendationLabel.UPDATE_LOCATOR,
                    0.87,
                    "heuristic"
            );
        }

        if (
                containsAny(
                        input,
                        "timeout",
                        "timed out",
                        "wait",
                        "not visible yet"
                )
        ) {

            return prediction(
                    RepairRecommendationLabel.ADD_WAIT,
                    0.82,
                    "heuristic"
            );
        }

        if (
                containsAny(
                        input,
                        "assertion failed",
                        "expected",
                        "actual",
                        "should see",
                        "stale text"
                )
        ) {

            return prediction(
                    RepairRecommendationLabel.FIX_ASSERTION,
                    0.85,
                    "heuristic"
            );
        }

        if (
                containsAny(
                        input,
                        "missing runtime",
                        "missing variable",
                        "invalid username",
                        "invalid password",
                        "test data",
                        "required value"
                )
        ) {

            return prediction(
                    RepairRecommendationLabel.UPDATE_TEST_DATA,
                    0.84,
                    "heuristic"
            );
        }

        if (
                containsAny(
                        input,
                        "intermittent",
                        "flaky",
                        "network",
                        "connection reset",
                        "retry"
                )
        ) {

            return prediction(
                    RepairRecommendationLabel.RETRY_ACTION,
                    0.78,
                    "heuristic"
            );
        }

        return prediction(
                RepairRecommendationLabel.ESCALATE_TO_OPENAI,
                0.45,
                "heuristic"
        );
    }
}
