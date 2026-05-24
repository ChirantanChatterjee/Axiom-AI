package com.axiomai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PublicBaseUrlResolver {

    @Value("${aif.public-base-url:http://localhost:8080}")
    private String configuredBaseUrl;

    public String url(String path) {
        String normalizedPath =
                path == null
                        ? ""
                        : path.trim();

        if (
                !normalizedPath.isEmpty()
                        && !normalizedPath.startsWith("/")
        ) {
            normalizedPath =
                    "/" + normalizedPath;
        }

        return normalizedBaseUrl()
                + normalizedPath;
    }

    private String normalizedBaseUrl() {
        String baseUrl =
                configuredBaseUrl == null
                        ? ""
                        : configuredBaseUrl.trim();

        if (baseUrl.isBlank()) {
            baseUrl =
                    "http://localhost:8080";
        }

        if (
                !baseUrl.startsWith("http://")
                        && !baseUrl.startsWith("https://")
        ) {
            baseUrl =
                    "https://" + baseUrl;
        }

        while (
                baseUrl.endsWith("/")
                        && baseUrl.length() > "https://".length()
        ) {
            baseUrl =
                    baseUrl.substring(
                            0,
                            baseUrl.length() - 1
                    );
        }

        return baseUrl;
    }
}
