package com.axiomai.qa.controller;

import com.axiomai.qa.models.PageScanResult;
import com.axiomai.qa.models.ScanRequest;
import com.axiomai.qa.service.PlaywrightScannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qa")
@CrossOrigin("*")
public class ScanController {

    @Autowired
    private PlaywrightScannerService scannerService;

    // =====================================================
    // SCAN ENDPOINT
    // =====================================================

    @PostMapping("/scan")
    public PageScanResult scan(
            @RequestBody ScanRequest request
    ) {

        return scannerService
                .scan(request.getUrl());
    }
}