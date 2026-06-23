package com.axiomai.ml;

import com.axiomai.ml.config.AIFMLProperties;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class LocatorRecoveryModelService
        extends AbstractWekaTextClassificationModelService<LocatorRecoveryLabel> {

    public LocatorRecoveryModelService(
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

        return AIFMLModelNames.LOCATOR_RECOVERY;
    }

    @Override
    protected Class<LocatorRecoveryLabel> labelClass() {

        return LocatorRecoveryLabel.class;
    }

    @Override
    protected LocatorRecoveryLabel defaultLabel() {

        return LocatorRecoveryLabel.CSS_SELECTOR;
    }

    @Override
    protected MLPrediction heuristicPrediction(
            String input
    ) {

        if (
                containsAny(
                        input,
                        "getbyrole",
                        "role=",
                        "aria-role",
                        "button",
                        "link"
                )
        ) {

            return prediction(
                    LocatorRecoveryLabel.ROLE_SELECTOR,
                    0.80,
                    "heuristic"
            );
        }

        if (
                containsAny(
                        input,
                        "placeholder",
                        "getbyplaceholder"
                )
        ) {

            return prediction(
                    LocatorRecoveryLabel.PLACEHOLDER_SELECTOR,
                    0.82,
                    "heuristic"
            );
        }

        if (
                containsAny(
                        input,
                        "label",
                        "getbylabel",
                        "aria-label"
                )
        ) {

            return prediction(
                    LocatorRecoveryLabel.LABEL_SELECTOR,
                    0.81,
                    "heuristic"
            );
        }

        if (
                containsAny(
                        input,
                        "text=",
                        "getbytext",
                        "has-text",
                        "visible text"
                )
        ) {

            return prediction(
                    LocatorRecoveryLabel.TEXT_SELECTOR,
                    0.79,
                    "heuristic"
            );
        }

        if (
                containsAny(
                        input,
                        "xpath",
                        "//",
                        "ancestor::",
                        "contains("
                )
        ) {

            return prediction(
                    LocatorRecoveryLabel.XPATH_SELECTOR,
                    0.74,
                    "heuristic"
            );
        }

        return prediction(
                LocatorRecoveryLabel.CSS_SELECTOR,
                0.70,
                "heuristic"
        );
    }

    public List<String> rankSelectors(
            List<String> selectors,
            MLPrediction prediction
    ) {

        if (
                selectors == null
                        ||
                        selectors.size() <= 1
                        ||
                        prediction == null
                        ||
                        prediction.getPredictedLabel() == null
        ) {

            return selectors;
        }

        LocatorRecoveryLabel preferred =
                parseLabel(
                        prediction.getPredictedLabel()
                );

        return selectors.stream()
                .sorted(
                        Comparator.comparingInt(
                                selector -> strategyDistance(
                                        selector,
                                        preferred
                                )
                        )
                )
                .toList();
    }

    public String finalStrategyForSelector(
            String selector
    ) {

        return selectorStrategy(selector)
                .name();
    }

    private int strategyDistance(
            String selector,
            LocatorRecoveryLabel preferred
    ) {

        LocatorRecoveryLabel actual =
                selectorStrategy(selector);

        if (
                actual == preferred
        ) {

            return 0;
        }

        if (
                preferred == LocatorRecoveryLabel.CSS_SELECTOR
                        &&
                        actual == LocatorRecoveryLabel.LABEL_SELECTOR
        ) {

            return 1;
        }

        return 2;
    }

    private LocatorRecoveryLabel selectorStrategy(
            String selector
    ) {

        String value =
                selector == null
                        ? ""
                        : selector.toLowerCase();

        if (
                value.startsWith("//")
                        ||
                        value.startsWith("xpath=")
        ) {

            return LocatorRecoveryLabel.XPATH_SELECTOR;
        }

        if (
                value.contains("placeholder")
        ) {

            return LocatorRecoveryLabel.PLACEHOLDER_SELECTOR;
        }

        if (
                value.contains("aria-label")
                        ||
                        value.contains("label")
        ) {

            return LocatorRecoveryLabel.LABEL_SELECTOR;
        }

        if (
                value.startsWith("text=")
                        ||
                        value.contains("has-text")
        ) {

            return LocatorRecoveryLabel.TEXT_SELECTOR;
        }

        if (
                value.contains("role=")
                        ||
                        value.contains("button")
                        ||
                        value.contains("a[")
        ) {

            return LocatorRecoveryLabel.ROLE_SELECTOR;
        }

        return LocatorRecoveryLabel.CSS_SELECTOR;
    }

    private LocatorRecoveryLabel parseLabel(
            String label
    ) {

        for (
                LocatorRecoveryLabel candidate
                : LocatorRecoveryLabel.values()
        ) {

            if (
                    candidate.name()
                            .equalsIgnoreCase(label)
            ) {

                return candidate;
            }
        }

        return LocatorRecoveryLabel.CSS_SELECTOR;
    }
}
