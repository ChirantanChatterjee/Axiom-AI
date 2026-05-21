package com.axiomai.admin.controller;

import com.axiomai.admin.dto.AdminMetricsResponse;
import com.axiomai.admin.service.AdminMetricsService;
import com.axiomai.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AdminController {

    private final AuthService authService;

    private final AdminMetricsService adminMetricsService;

    @GetMapping("/metrics")
    public AdminMetricsResponse metrics(
            @RequestHeader("X-AIF-Session") String token
    ) {

        authService.requireAdmin(token);

        return adminMetricsService.metrics();
    }
}
