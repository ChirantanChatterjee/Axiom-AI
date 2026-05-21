package com.axiomai.qa.service;

import com.axiomai.qa.flow.DetectedFlow;
import com.axiomai.qa.flow.FlowDetectionEngine;
import com.axiomai.qa.models.PageNode;
import com.axiomai.qa.models.SiteMapResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FlowDetectionService {

    // =====================================================
    // DETECT FLOWS
    // =====================================================

    public List<DetectedFlow> detectFlows(
            SiteMapResult siteMap
    ) {

        List<DetectedFlow> detectedFlows =
                new ArrayList<>();

        if (
                siteMap == null
                        ||
                        siteMap.getPages() == null
        ) {

            return detectedFlows;
        }

        // =================================================
        // PROCESS EACH PAGE
        // =================================================

        for (PageNode page : siteMap.getPages()) {

            try {

                List<DetectedFlow> pageFlows =

                        FlowDetectionEngine
                                .detectFlows(

                                        page.getUrl(),
                                        page.getElements()
                                );

                if (
                        pageFlows != null
                                &&
                                !pageFlows.isEmpty()
                ) {

                    detectedFlows.addAll(
                            pageFlows
                    );
                }

            } catch (Exception e) {

                System.out.println(
                        "FLOW DETECTION FAILED FOR PAGE = "
                                + page.getUrl()
                );

                e.printStackTrace();
            }
        }

        return detectedFlows;
    }

    // =====================================================
    // DEBUG FLOWS
    // =====================================================

    public void printFlows(
            List<DetectedFlow> flows
    ) {

        for (DetectedFlow flow : flows) {

            System.out.println(
                    "FLOW TYPE = "
                            + flow.getFlowType()
            );

            System.out.println(
                    "PAGE URL = "
                            + flow.getPageUrl()
            );

            System.out.println(
                    "TOTAL STEPS = "
                            + flow.getSteps().size()
            );

            System.out.println(
                    "=================================="
            );
        }
    }
}