package com.axiomai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.Locale;

@Configuration
public class CorsConfig {

    private static final String DEFAULT_ALLOWED_ORIGINS =
            "https://aif-pi.vercel.app,http://localhost:5173,http://localhost:3000,http://localhost:8080";

    @Value("${aif.cors.allowed-origins:${aif.cors.allowed-origin-patterns:" + DEFAULT_ALLOWED_ORIGINS + "}}")
    private String allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {

        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(
                    CorsRegistry registry
            ) {

                registry.addMapping("/**")
                        .allowedOrigins(
                                origins()
                        )
                        .allowedMethods(
                                "GET",
                                "POST",
                                "PUT",
                                "PATCH",
                                "DELETE",
                                "OPTIONS"
                        )
                        .allowedHeaders(
                                "Accept",
                                "Authorization",
                                "Content-Type",
                                "X-AIF-Session",
                                "X-Requested-With"
                        )
                        .exposedHeaders(
                                "Content-Disposition"
                        )
                        .maxAge(3600)
                        .allowCredentials(true);

            }
        };
    }

    private String[] origins() {

        String[] configuredOrigins =
                Arrays.stream(
                                allowedOrigins.split(",")
                        )
                        .map(String::trim)
                        .filter(origin -> !origin.isBlank())
                        .filter(this::isSafeExactOrigin)
                        .toArray(String[]::new);

        if (
                configuredOrigins.length > 0
        ) {

            return configuredOrigins;
        }

        return Arrays.stream(
                        DEFAULT_ALLOWED_ORIGINS.split(",")
                )
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toArray(String[]::new);
    }

    private boolean isSafeExactOrigin(
            String origin
    ) {

        String lower =
                origin.toLowerCase(Locale.ROOT);

        return (lower.startsWith("https://")
                ||
                lower.startsWith("http://localhost:")
                ||
                lower.startsWith("http://127.0.0.1:"))
                &&
                !origin.contains("*");
    }
}
