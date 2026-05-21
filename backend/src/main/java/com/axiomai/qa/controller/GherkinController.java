package com.axiomai.qa.controller;

import com.axiomai.qa.flow.DetectedFlow;
import com.axiomai.qa.models.GeneratedFeature;
import com.axiomai.qa.models.ScanRequest;
import com.axiomai.qa.models.SiteMapResult;
import com.axiomai.qa.service.FlowDetectionService;
import com.axiomai.qa.service.GherkinGeneratorService;
import com.axiomai.qa.service.WebsiteCrawlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/qa")
@CrossOrigin("*")

public class GherkinController {

    @Autowired
    private WebsiteCrawlerService
            crawlerService;

    @Autowired
    private FlowDetectionService
            flowDetectionService;

    @Autowired
    private GherkinGeneratorService
            gherkinGeneratorService;

    // =====================================================
    // GENERATE GHERKIN
    // =====================================================

    @PostMapping("/generate-gherkin")

    public List<GeneratedFeature> generate(
            @RequestBody ScanRequest request
    ) {

        // =================================================
        // WEBSITE CRAWL
        // =================================================

        SiteMapResult siteMap =
                crawlerService.crawl(
                        request.getUrl()
                );

        // =================================================
        // FLOW DETECTION
        // =================================================

        List<DetectedFlow> flows =
                flowDetectionService.detectFlows(
                        siteMap
                );

        // =================================================
        // FEATURE GENERATION
        // =================================================

        return gherkinGeneratorService
                .generateFeatures(flows);
    }
}