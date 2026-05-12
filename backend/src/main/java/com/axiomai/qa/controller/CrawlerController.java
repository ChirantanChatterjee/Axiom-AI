package com.axiomai.qa.controller;

import com.axiomai.qa.models.ScanRequest;
import com.axiomai.qa.models.SiteMapResult;
import com.axiomai.qa.service.WebsiteCrawlerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qa")
@CrossOrigin("*")
public class CrawlerController {

    @Autowired
    private WebsiteCrawlerService crawlerService;

    @PostMapping("/crawl")
    public SiteMapResult crawl(
            @RequestBody ScanRequest request
    ) {

        return crawlerService
                .crawl(request.getUrl());
    }
}