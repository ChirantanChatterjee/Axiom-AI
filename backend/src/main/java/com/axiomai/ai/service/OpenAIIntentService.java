package com.axiomai.ai.service;

import com.axiomai.ai.model.GPTIntentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor

public class OpenAIIntentService {

    @Value("${openai.api.key}")
    private String apiKey;

    private final ObjectMapper
            objectMapper =
            new ObjectMapper();

    public GPTIntentResponse interpret(
            String userMessage
    ) {

        try {

            RestTemplate restTemplate =
                    new RestTemplate();

            HttpHeaders headers =
                    new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_JSON
            );

            headers.setBearerAuth(apiKey);

            String prompt = """

You are an AI automation orchestrator.

Analyze the user request and extract:

1. intent
2. flowName

Allowed intents:

EXECUTE_FLOW
SHOW_REPORT
SHOW_DB
GENERATE_FRAMEWORK
UNKNOWN

Return ONLY valid JSON.

Example:

{
  "intent":"EXECUTE_FLOW",
  "flowName":"OrangeHRM Login"
}

User Request:
""" + userMessage;

            Map<String, Object> body =
                    new HashMap<>();

            body.put(
                    "model",
                    "gpt-4.1-mini"
            );

            List<Map<String, String>> messages =
                    List.of(

                            Map.of(
                                    "role",
                                    "system",
                                    "content",
                                    "You are an intelligent automation orchestration engine."
                            ),

                            Map.of(
                                    "role",
                                    "user",
                                    "content",
                                    prompt
                            )
                    );

            body.put(
                    "messages",
                    messages
            );

            HttpEntity<Map<String, Object>>
                    entity =
                    new HttpEntity<>(
                            body,
                            headers
                    );

            ResponseEntity<Map> response =
                    restTemplate.exchange(

                            "https://api.openai.com/v1/chat/completions",

                            HttpMethod.POST,

                            entity,

                            Map.class
                    );

            List choices =
                    (List) response.getBody()
                            .get("choices");

            Map choice =
                    (Map) choices.get(0);

            Map message =
                    (Map) choice.get("message");

            String content =
                    message.get("content")
                            .toString();

            System.out.println(
                    "OPENAI RESPONSE = "
                            + content
            );

            return objectMapper.readValue(
                    content,
                    GPTIntentResponse.class
            );

        } catch (Exception e) {

            System.out.println(
                    "OPENAI FAILED -> USING FALLBACK RULE ENGINE"
            );

            e.printStackTrace();

            return GPTIntentResponse.builder()

                    .intent("FALLBACK")

                    .responseMessage(userMessage)

                    .build();
        }

    }

}