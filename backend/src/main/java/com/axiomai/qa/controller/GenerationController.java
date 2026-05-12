package com.axiomai.qa.controller;

import com.axiomai.qa.generator.FeatureFileGenerator;
import com.axiomai.qa.models.PageScanResult;
import com.axiomai.qa.models.ScanRequest;
import com.axiomai.qa.service.PlaywrightScannerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qa")
@CrossOrigin("*")
public class GenerationController {

    @Autowired
    private PlaywrightScannerService scannerService;

    @Autowired
    private FeatureFileGenerator featureFileGenerator;

    // =====================================================
    // GENERATE FEATURE FILE
    // =====================================================

    @PostMapping("/generate-feature")
    public String generateFeature(
            @RequestBody ScanRequest request
    ) {

        PageScanResult scanResult =
                scannerService.scan(
                        request.getUrl()
                );

        return featureFileGenerator
                .generate(scanResult);
    }
}