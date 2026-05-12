package com.axiomai.qa.controller;

import com.axiomai.qa.models.*;
import com.axiomai.qa.service.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/qa")
@CrossOrigin("*")
public class GherkinController {

    @Autowired
    private WebsiteCrawlerService crawlerService;

    @Autowired
    private FlowDetectionService flowDetectionService;

    @Autowired
    private GherkinGeneratorService gherkinGeneratorService;

    @PostMapping("/generate-gherkin")
    public List<GeneratedFeature> generate(
            @RequestBody ScanRequest request
    ) {

        SiteMapResult siteMap =
                crawlerService.crawl(
                        request.getUrl()
                );

        List<DetectedFlow> flows =
                flowDetectionService.detectFlows(
                        siteMap
                );

        return gherkinGeneratorService
                .generateFeatures(flows);
    }
}