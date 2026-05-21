package com.axiomai.qa.controller;

import com.axiomai.qa.service.FrameworkModificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/framework/session")
@RequiredArgsConstructor
@CrossOrigin("*")
public class FrameworkModificationController {

    private final FrameworkModificationService frameworkModificationService;

    @PostMapping("/{sessionId}/upload")
    public FrameworkModificationService.UploadedFrameworkResult upload(

            @PathVariable String sessionId,

            @RequestParam("file") MultipartFile file

    ) {

        return frameworkModificationService.uploadModifiedFramework(
                sessionId,
                file
        );
    }
}
