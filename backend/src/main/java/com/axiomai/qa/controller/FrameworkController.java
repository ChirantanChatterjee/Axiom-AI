package com.axiomai.qa.controller;

import com.axiomai.qa.models.*;
import com.axiomai.qa.service.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/qa")
@CrossOrigin("*")
public class FrameworkController {

    @Autowired
    private WebsiteCrawlerService crawlerService;

    @Autowired
    private FlowDetectionService flowDetectionService;

    @Autowired
    private FrameworkGeneratorService frameworkGeneratorService;

    @Autowired
    private GeneratedProjectWriterService writerService;

    // =====================================================
    // GENERATE FRAMEWORK
    // =====================================================

    @PostMapping("/generate-framework")
    public GeneratedFramework generateFramework(
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

        GeneratedFramework framework =
                frameworkGeneratorService
                        .generate(flows);

        // =============================================
        // WRITE FILES
        // =============================================

        String result =
                writerService.writeFramework(
                        framework
                );

        System.out.println(result);

        return framework;
    }
}