package com.axiomai.qa.service;

import com.axiomai.flow.entity.FlowEntity;
import com.axiomai.flow.repository.FlowRepository;
import com.axiomai.flowstep.entity.FlowStepEntity;
import com.axiomai.flowstep.repository.FlowStepRepository;
import com.axiomai.qa.flow.DetectedFlow;
import com.axiomai.qa.models.FlowStep;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor

public class FlowPersistenceService {

    private final FlowRepository
            flowRepository;

    private final FlowStepRepository
            flowStepRepository;

    // =====================================================
    // SAVE DETECTED FLOWS
    // =====================================================

    public void saveFlows(

            List<DetectedFlow> flows
    ) {

        System.out.println(
                "SAVING FLOWS = "
                        + flows.size()
        );

        for (DetectedFlow detectedFlow : flows) {

            FlowEntity flowEntity =
                    buildFlowEntity(detectedFlow);

            FlowEntity savedFlow =
                    flowRepository.save(flowEntity);

            saveSteps(
                    savedFlow,
                    detectedFlow
            );

            System.out.println(
                    "FLOW SAVED = "
                            + savedFlow.getFlowName()
            );

            System.out.println(
                    "SAVING FLOW TYPE = "
                            + detectedFlow.getFlowType()
            );
        }
    }

    // =====================================================
    // BUILD FLOW ENTITY
    // =====================================================

    private FlowEntity buildFlowEntity(
            DetectedFlow flow
    ) {

        String url =
                flow.getPageUrl();

        String domain =
                extractDomain(url);

        return FlowEntity.builder()

                .flowName(
                        buildFlowName(
                                flow
                        )
                )

                .domainName(
                        domain
                )

                .baseUrl(
                        flow.getPageUrl()
                )

                .description(
                        "AI Generated Flow for "
                                + domain
                )

                .projectId(1L)

                .createdAt(
                        LocalDateTime.now()
                )

                .build();
    }

    // =====================================================
    // SAVE STEPS
    // =====================================================

    private void saveSteps(
            FlowEntity flowEntity,
            DetectedFlow flow
    ) {

        int order = 1;

        for (FlowStep step : flow.getSteps()) {

            FlowStepEntity entity =
                    FlowStepEntity.builder()

                            .flowId(
                                    flowEntity.getId()
                            )

                            .stepOrder(order++)

                            .action(
                                    step.getAction()
                            )

                            .elementName(
                                    step.getTarget()
                            )

                            .locatorType(
                                    detectLocatorType(
                                            step.getSelector()
                                    )
                            )

                            .locatorValue(
                                    safeSelector(
                                            step.getSelector()
                                    )
                            )

                            .fallbackLocator(
                                    buildFallback(
                                            step
                                    )
                            )

                            .inputValue(
                                    defaultInput(
                                            step
                                    )
                            )

                            .expectedValue(
                                    defaultExpected(
                                            step
                                    )
                            )

                            .required(true)

                            .build();

            flowStepRepository.save(entity);
        }
    }

    // =====================================================
    // DEFAULT INPUTS
    // =====================================================

    private String defaultInput(
            FlowStep step
    ) {

        String target =
                safe(step.getTarget());

        if (
                target.contains("USERNAME")
        ) {

            return "Admin";
        }

        if (
                target.contains("PASSWORD")
        ) {

            return "admin123";
        }

        if (
                target.contains("SEARCH")
        ) {

            return "Axiom AI";
        }

        return "Sample Data";
    }

    // =====================================================
    // DEFAULT EXPECTED
    // =====================================================

    private String defaultExpected(
            FlowStep step
    ) {

        return "";
    }

    // =====================================================
    // FALLBACK LOCATOR
    // =====================================================

    private String buildFallback(
            FlowStep step
    ) {

        if (
                step.getFallbackSelectors() != null
                        &&
                        !step.getFallbackSelectors()
                                .isEmpty()
        ) {

            return step.getFallbackSelectors()
                    .get(0);
        }

        return null;
    }

    // =====================================================
    // FLOW NAME
    // =====================================================

    private String buildFlowName(
            DetectedFlow flow
    ) {

        String domain =
                extractDomain(
                        flow.getPageUrl()
                );

        return domain
                + " "
                + flow.getFlowType()
                + " Flow";
    }

    // =====================================================
// EXTRACT DOMAIN
// =====================================================

    private String extractDomain(
            String url
    ) {

        try {

            String cleaned = url

                    .replace("https://", "")
                    .replace("http://", "")
                    .replace("www.", "");

            return cleaned.split("/")[0];

        } catch (Exception e) {

            return "unknown-domain";
        }
    }

    // =====================================================
// LOCATOR TYPE
// =====================================================

    private String detectLocatorType(
            String selector
    ) {

        if (
                selector == null
                        ||
                        selector.isBlank()
        ) {

            return "CSS";
        }

        if (
                selector.startsWith("//")
        ) {

            return "XPATH";
        }

        return "CSS";
    }

    // =====================================================
    // SAFE
    // =====================================================

    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value.toUpperCase();
    }

    // =====================================================
// SAFE SELECTOR
// =====================================================

    private String safeSelector(
            String selector
    ) {

        if (
                selector == null
                        ||
                        selector.isBlank()
        ) {

            return "body";
        }

        return selector;
    }
}