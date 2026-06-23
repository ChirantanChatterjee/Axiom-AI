package com.axiomai.ml;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AIFRepairMLContext {

    private MLPrediction failurePrediction;

    private MLPrediction repairPrediction;

    private List<AIFTrainingDataService.SimilarRepairExample> similarRepairs;

    public String toPromptSection() {

        StringBuilder section =
                new StringBuilder(
                        "AIF custom ML repair context:"
                );

        if (
                failurePrediction != null
        ) {

            section.append(System.lineSeparator())
                    .append("- Predicted failure type: ")
                    .append(failurePrediction.getPredictedLabel())
                    .append(" (confidence ")
                    .append(
                            String.format(
                                    "%.2f",
                                    failurePrediction.getConfidence()
                            )
                    )
                    .append(")");
        }

        if (
                repairPrediction != null
        ) {

            section.append(System.lineSeparator())
                    .append("- Recommended repair strategy: ")
                    .append(repairPrediction.getPredictedLabel())
                    .append(" (confidence ")
                    .append(
                            String.format(
                                    "%.2f",
                                    repairPrediction.getConfidence()
                            )
                    )
                    .append(")");
        }

        if (
                similarRepairs != null
                        &&
                        !similarRepairs.isEmpty()
        ) {

            section.append(System.lineSeparator())
                    .append("- Similar successful historical repairs:");

            for (
                    AIFTrainingDataService.SimilarRepairExample example
                    : similarRepairs
            ) {

                section.append(System.lineSeparator())
                        .append("  * strategy=")
                        .append(example.getFinalRepairStrategy())
                        .append(", similarity=")
                        .append(
                                String.format(
                                        "%.2f",
                                        example.getSimilarity()
                                )
                        );
            }
        }

        section.append(System.lineSeparator())
                .append("Use this as supporting context only. OpenAI must still validate the final repair.");

        return section.toString();
    }
}
