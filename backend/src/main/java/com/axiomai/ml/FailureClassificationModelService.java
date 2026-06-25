package com.axiomai.ml;

import com.axiomai.ml.config.AIFMLProperties;
import com.axiomai.qa.service.GeneratedLocatorRepairAnalyzer;
import org.springframework.stereotype.Service;

@Service
public class FailureClassificationModelService
        extends AbstractWekaTextClassificationModelService<FailureClassificationLabel> {

    public FailureClassificationModelService(
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

        return AIFMLModelNames.FAILURE_CLASSIFICATION;
    }

    @Override
    protected Class<FailureClassificationLabel> labelClass() {

        return FailureClassificationLabel.class;
    }

    @Override
    protected FailureClassificationLabel defaultLabel() {

        return FailureClassificationLabel.UNKNOWN;
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
                    FailureClassificationLabel.LOCATOR_MISMATCH,
                    0.91,
                    "heuristic"
            );
        }

        if (
                containsAny(
                        input,
                        "unable to resolve element",
                        "unable to locate element",
                        "no valid locator",
                        "locator failed",
                        "strict mode violation",
                        "element not found",
                        "waiting for locator"
                )
        ) {

            return prediction(
                    FailureClassificationLabel.LOCATOR_FAILURE,
                    0.88,
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
                        "comparison failure"
                )
        ) {

            return prediction(
                    FailureClassificationLabel.ASSERTION_FAILURE,
                    0.86,
                    "heuristic"
            );
        }

        if (
                containsAny(
                        input,
                        "timeout",
                        "timed out",
                        "exceeded timeout",
                        "waiting failed"
                )
        ) {

            return prediction(
                    FailureClassificationLabel.TIMEOUT_FAILURE,
                    0.84,
                    "heuristic"
            );
        }

        if (
                containsAny(
                        input,
                        "unauthorized",
                        "forbidden",
                        "login failed",
                        "invalid username",
                        "invalid password",
                        "401",
                        "403"
                )
        ) {

            return prediction(
                    FailureClassificationLabel.AUTH_FAILURE,
                    0.83,
                    "heuristic"
            );
        }

        if (
                containsAny(
                        input,
                        "missing runtime",
                        "missing variable",
                        "test data",
                        "invalid data",
                        "required value"
                )
        ) {

            return prediction(
                    FailureClassificationLabel.DATA_FAILURE,
                    0.82,
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
                        "undefinedstepexception",
                        "browser executable",
                        "playwright install",
                        "connection refused",
                        "unknown host",
                        "maven",
                        "dependency",
                        "java.lang",
                        "environment"
                )
        ) {

            return prediction(
                    FailureClassificationLabel.ENVIRONMENT_FAILURE,
                    0.80,
                    "heuristic"
            );
        }

        return prediction(
                FailureClassificationLabel.UNKNOWN,
                0.22,
                "heuristic"
        );
    }
}
