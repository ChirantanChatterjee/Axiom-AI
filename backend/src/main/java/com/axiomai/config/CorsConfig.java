package com.axiomai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Value("${aif.cors.allowed-origin-patterns:${AIF_CORS_ALLOWED_ORIGIN_PATTERNS:https://aif-pi.vercel.app,https://*.vercel.app,http://localhost:5173,http://localhost:3000,http://localhost:8080}}")
    private String allowedOriginPatterns;

    @Bean
    public WebMvcConfigurer corsConfigurer() {

        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(
                    CorsRegistry registry
            ) {

                registry.addMapping("/**")
                        .allowedOriginPatterns(
                                originPatterns()
                        )
                        .allowedMethods(
                                "GET",
                                "POST",
                                "PUT",
                                "PATCH",
                                "DELETE",
                                "OPTIONS"
                        )
                        .allowedHeaders("*")
                        .exposedHeaders(
                                "Content-Disposition"
                        )
                        .maxAge(3600)
                        .allowCredentials(true);

            }
        };
    }

    private String[] originPatterns() {

        return Arrays.stream(
                        allowedOriginPatterns.split(",")
                )
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toArray(String[]::new);
    }
}
