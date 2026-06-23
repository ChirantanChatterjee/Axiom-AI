package com.axiomai.ml;

import com.axiomai.ml.config.AIFMLProperties;
import org.springframework.stereotype.Service;

@Service
public class IntentClassificationModelService
        extends AbstractWekaTextClassificationModelService<IntentClassificationLabel> {

    public IntentClassificationModelService(
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

        return AIFMLModelNames.INTENT_CLASSIFICATION;
    }

    @Override
    protected Class<IntentClassificationLabel> labelClass() {

        return IntentClassificationLabel.class;
    }

    @Override
    protected IntentClassificationLabel defaultLabel() {

        return IntentClassificationLabel.UNKNOWN;
    }

    @Override
    protected MLPrediction heuristicPrediction(
            String input
    ) {

        if (
                containsAny(
                        input,
                        "generate framework",
                        "create framework",
                        "build framework",
                        "automation framework",
                        "crawl website",
                        "scan website"
                )
        ) {

            return prediction(
                    IntentClassificationLabel.GENERATE_FRAMEWORK,
                    0.86,
                    "heuristic"
            );
        }

        if (
                containsAny(
                        input,
                        "repair",
                        "fix failed",
                        "heal",
                        "broken test",
                        "failed generated test"
                )
        ) {

            return prediction(
                    IntentClassificationLabel.REPAIR_TEST,
                    0.84,
                    "heuristic"
            );
        }

        if (
                containsAny(
                        input,
                        "show report",
                        "open report",
                        "latest report",
                        "test report",
                        "execution report"
                )
        ) {

            return prediction(
                    IntentClassificationLabel.SHOW_REPORT,
                    0.83,
                    "heuristic"
            );
        }

        if (
                containsAny(
                        input,
                        "show db",
                        "show database",
                        "database",
                        "db tables",
                        "show tables"
                )
        ) {

            return prediction(
                    IntentClassificationLabel.SHOW_DB,
                    0.82,
                    "heuristic"
            );
        }

        if (
                containsAny(
                        input,
                        "execute flow",
                        "run flow",
                        "execute test",
                        "run test",
                        "rerun test",
                        "start execution"
                )
        ) {

            return prediction(
                    IntentClassificationLabel.EXECUTE_FLOW,
                    0.81,
                    "heuristic"
            );
        }

        return prediction(
                IntentClassificationLabel.UNKNOWN,
                0.20,
                "heuristic"
        );
    }
}
