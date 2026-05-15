package com.axiomai.qa.service;

import com.axiomai.qa.flow.DetectedFlow;
import com.axiomai.qa.models.FlowStep;
import com.axiomai.qa.models.GeneratedFeature;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GherkinGeneratorService {

    // =====================================================
    // GENERATE MULTIPLE FEATURES
    // =====================================================

    public List<GeneratedFeature> generateFeatures(
            List<DetectedFlow> flows
    ) {

        List<GeneratedFeature> features =
                new ArrayList<>();

        for (DetectedFlow flow : flows) {

            GeneratedFeature feature =
                    new GeneratedFeature();

            feature.setFeatureName(
                    flow.getFlowType()
            );

            feature.setFeatureContent(
                    buildFeature(flow)
            );

            features.add(feature);
        }

        return features;
    }

    // =====================================================
    // SINGLE FEATURE
    // =====================================================

    public String generateFeature(
            List<DetectedFlow> flows
    ) {

        StringBuilder builder =
                new StringBuilder();

        for (DetectedFlow flow : flows) {

            builder.append(
                    buildFeature(flow)
            );

            builder.append("\n\n");
        }

        return builder.toString();
    }

    // =====================================================
    // BUILD FEATURE
    // =====================================================

    private String buildFeature(
            DetectedFlow flow
    ) {

        StringBuilder builder =
                new StringBuilder();

        builder.append("Feature: ")
                .append(flow.getFlowType())
                .append("\n\n");

        builder.append("  Scenario: Execute ")
                .append(flow.getFlowType())
                .append(" flow\n");

        builder.append("    Given user launches \"")
                .append(flow.getPageUrl())
                .append("\"\n");

        for (FlowStep step : flow.getSteps()) {

            switch (
                    step.getAction().toUpperCase()
            ) {

                case "TYPE" ->

                        builder.append(
                                        "    When user enters value into ")
                                .append(step.getTarget())
                                .append("\n");

                case "CLICK" ->

                        builder.append(
                                        "    And user clicks ")
                                .append(step.getTarget())
                                .append("\n");

                default ->

                        builder.append(
                                        "    Then validate ")
                                .append(step.getTarget())
                                .append("\n");
            }
        }

        return builder.toString();
    }
}