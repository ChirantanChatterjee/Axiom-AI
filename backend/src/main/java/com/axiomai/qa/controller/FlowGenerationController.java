package com.axiomai.qa.controller;

import com.axiomai.qa.flow.DetectedFlow;
import com.axiomai.qa.flow.FlowDetectionEngine;
import com.axiomai.qa.generator.flow.FlowFrameworkAssembler;
import com.axiomai.qa.models.GenerateFlowRequest;
import com.axiomai.qa.models.PageScanResult;
import com.axiomai.qa.service.PlaywrightScannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/flow")
public class FlowGenerationController {

    @Autowired
    private PlaywrightScannerService scannerService;

    @Autowired
    private FlowFrameworkAssembler assembler;

    @PostMapping("/generate")
    public String generateFlowFramework(

            @RequestBody GenerateFlowRequest request

    ) {

        // =============================================
        // URL
        // =============================================

        String url =
                request.getUrl();

        // =============================================
        // SCAN
        // =============================================

        PageScanResult result =
                scannerService.scan(url);

        // =============================================
        // DETECT FLOWS
        // =============================================

        List<DetectedFlow> flows =
                FlowDetectionEngine.detectFlows(

                        result.getUrl(),
                        result.getElements()
                );

        // =============================================
        // GENERATE FRAMEWORK
        // =============================================

        assembler.assemble(flows);

        return "Flow framework generated successfully.";
    }
}