package com.axiomai.qa.controller;

import com.axiomai.qa.generator.PageObjectGenerator;
import com.axiomai.qa.generator.StepDefinitionGenerator;
import com.axiomai.qa.models.PageScanResult;
import com.axiomai.qa.models.ScanRequest;
import com.axiomai.qa.service.PlaywrightScannerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/qa")
@CrossOrigin("*")
public class CodeGenerationController {

    @Autowired
    private PlaywrightScannerService scannerService;

    @Autowired
    private PageObjectGenerator pageObjectGenerator;

    @Autowired
    private StepDefinitionGenerator stepDefinitionGenerator;

    // =====================================================
    // GENERATE CODE
    // =====================================================

    @PostMapping("/generate-code")
    public Map<String, String> generateCode(
            @RequestBody ScanRequest request
    ) {

        PageScanResult scanResult =
                scannerService.scan(
                        request.getUrl()
                );

        String pageObject =
                pageObjectGenerator.generate(scanResult);

        String stepDefs =
                stepDefinitionGenerator.generate(scanResult);

        Map<String, String> response =
                new HashMap<>();

        response.put(
                "pageObject",
                pageObject
        );

        response.put(
                "stepDefinitions",
                stepDefs
        );

        return response;
    }
}