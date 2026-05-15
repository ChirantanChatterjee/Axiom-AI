package com.axiomai.qa.controller;

import com.axiomai.qa.flow.DetectedFlow;
import com.axiomai.qa.models.GeneratedFramework;
import com.axiomai.qa.models.SiteMapResult;
import com.axiomai.qa.service.FlowDetectionService;
import com.axiomai.qa.service.FrameworkGeneratorService;
import com.axiomai.qa.service.WebsiteCrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/framework")

@RequiredArgsConstructor

public class FrameworkController {

    private final WebsiteCrawlerService
            websiteCrawlerService;

    private final FlowDetectionService
            flowDetectionService;

    private final FrameworkGeneratorService
            frameworkGeneratorService;

    // =====================================================
    // GENERATE FRAMEWORK
    // =====================================================

    @PostMapping("/generate")

    public GeneratedFramework generateFramework(
            @RequestParam String url
    ) {

        // =================================================
        // CRAWL WEBSITE
        // =================================================

        SiteMapResult siteMap =
                websiteCrawlerService
                        .crawl(url);

        // =================================================
        // DETECT FLOWS
        // =================================================

        List<DetectedFlow> flows =
                flowDetectionService
                        .detectFlows(siteMap);

        // =================================================
        // GENERATE FRAMEWORK
        // =================================================

        return frameworkGeneratorService
                .generate(flows);
    }
}