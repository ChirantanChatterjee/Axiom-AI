package com.axiomai.qa.controller;

import com.axiomai.qa.models.*;
import com.axiomai.qa.service.FlowDetectionService;
import com.axiomai.qa.service.WebsiteCrawlerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/qa")
@CrossOrigin("*")
public class FlowController {

    @Autowired
    private WebsiteCrawlerService crawlerService;

    @Autowired
    private FlowDetectionService flowDetectionService;

    @PostMapping("/detect-flows")
    public List<DetectedFlow> detectFlows(
            @RequestBody ScanRequest request
    ) {

        SiteMapResult siteMap =
                crawlerService.crawl(
                        request.getUrl()
                );

        return flowDetectionService
                .detectFlows(siteMap);
    }
}