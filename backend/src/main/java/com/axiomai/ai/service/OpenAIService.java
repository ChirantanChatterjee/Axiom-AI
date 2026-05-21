package com.axiomai.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenAIService {

    @Value("${openai.api.key:}")
    private String apiKey;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public String ask(String prompt) {

        try {

            if (
                    apiKey == null
                            ||
                            apiKey.isBlank()
            ) {

                System.out.println(
                        "[OPENAI SERVICE] API key not configured. Skipping LLM fallback."
                );

                return "";
            }

            WebClient client =
                    WebClient.builder()
                            .baseUrl("https://api.openai.com")
                            .defaultHeader(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + apiKey
                            )
                            .defaultHeader(
                                    HttpHeaders.CONTENT_TYPE,
                                    MediaType.APPLICATION_JSON_VALUE
                            )
                            .build();

            Map<String, Object> request =
                    new HashMap<>();

            request.put("model", "gpt-4.1");

            request.put(
                    "messages",
                    new Object[]{
                            Map.of(
                                    "role",
                                    "user",
                                    "content",
                                    prompt
                            )
                    }
            );

            String response = client.post()
                    .uri("/v1/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root =
                    objectMapper.readTree(response);

            return root
                    .get("choices")
                    .get(0)
                    .get("message")
                    .get("content")
                    .asText();

        } catch (Exception e) {

            System.out.println(
                    "[OPENAI SERVICE] Request failed: "
                            + e.getMessage()
            );

            return "OpenAI request failed.";

        }

    }

}
