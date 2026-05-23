package com.axiomai.ai.service;

import com.axiomai.ai.model.GPTIntentResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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

            if (
                    apiKey == null
                            ||
                            apiKey.isBlank()
            ) {

                System.out.println(
                        "OPENAI_API_KEY NOT SET -> USING FALLBACK RULE ENGINE"
                );

                return GPTIntentResponse.builder()

                        .intent("FALLBACK")

                        .responseMessage(userMessage)

                        .build();
            }

            RestTemplate restTemplate =
                    new RestTemplate();

            HttpHeaders headers =
                    new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_JSON
            );

            headers.setBearerAuth(apiKey);

            String prompt = """

You are an AI automation workspace orchestrator.

Analyze the user request and extract these fields:
- intent
- flowName
- url
- featureName
- artifactName
- variables

Allowed intents:
- GENERATE_FRAMEWORK
- GENERATE_FEATURE
- UPDATE_TEST_DATA
- EXECUTE_FEATURE
- EXECUTE_FLOW
- SHOW_GENERATED_TEST_TAGS
- EXECUTE_GENERATED_TESTS
- REPAIR_GENERATED_TESTS
- DOWNLOAD_FRAMEWORK
- SHOW_REPORT
- SHOW_DB
- UNKNOWN

Rules:
- GENERATE_FRAMEWORK means the user wants a full automation framework for a website.
- GENERATE_FEATURE means the user wants a specific feature/scenario/test created or updated.
- Phrases such as "generate more tests", "add edge cases", "add negative tests", or "create boundary scenarios" for a named feature are GENERATE_FEATURE.
- Treat "bill pay", "billpay", "bill payment", and "pay bill" as featureName "bill pay".
- UPDATE_TEST_DATA means the user provides values such as username, password, email, search term, or other field data.
- EXECUTE_FEATURE means the user wants to run a named generated feature.
- EXECUTE_FLOW means the user wants to run a detected or stored flow.
- SHOW_GENERATED_TEST_TAGS means the user asks for available generated-test tags.
- EXECUTE_GENERATED_TESTS means the user wants to run generated Cucumber tests by tag, all generated tests, or a named generated test feature.
- REPAIR_GENERATED_TESTS means the user asks to inspect, fix, repair, correct, or update a failed generated/Cucumber test, including natural phrasing like "I see some failures, can you fix it?" or assertion-text corrections like "the actual assertion text was X".
- DOWNLOAD_FRAMEWORK means the user wants the generated framework zip/download.
- Put website targets in url. Normalize bare domains if possible.
- Put data values in variables as a JSON object with lower-case keys.

Return ONLY valid JSON with this shape:

{
  "intent": "GENERATE_FEATURE",
  "flowName": "login",
  "url": "https://www.saucedemo.com",
  "featureName": "login",
  "artifactName": null,
  "variables": {
    "username": "standard_user",
    "password": "secret_sauce"
  }
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

            content =
                    extractJson(content);

            GPTIntentResponse intentResponse =
                    parseIntentResponse(content);

            System.out.println(
                    "OPENAI RESPONSE = "
                            + formatForLog(intentResponse)
            );

            return intentResponse;

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

    private String extractJson(
            String content
    ) {

        if (
                content == null
        ) {

            return null;
        }

        String cleaned =
                content.trim();

        if (
                cleaned.startsWith("```")
        ) {

            cleaned =
                    cleaned.replaceFirst(
                            "^```(?:json)?\\s*",
                            ""
                    );

            cleaned =
                    cleaned.replaceFirst(
                            "\\s*```$",
                            ""
                    )
                            .trim();
        }

        int objectStart =
                cleaned.indexOf('{');

        int objectEnd =
                cleaned.lastIndexOf('}');

        if (
                objectStart >= 0
                        &&
                        objectEnd > objectStart
        ) {

            return cleaned.substring(
                    objectStart,
                    objectEnd + 1
            );
        }

        return cleaned;
    }

    private GPTIntentResponse parseIntentResponse(
            String content
    ) throws Exception {

        JsonNode root =
                objectMapper.readTree(content);

        if (
                root instanceof ObjectNode objectNode
        ) {

            normalizeVariables(objectNode);
        }

        return objectMapper.treeToValue(
                root,
                GPTIntentResponse.class
        );
    }

    private void normalizeVariables(
            ObjectNode root
    ) {

        JsonNode variables =
                root.get("variables");

        if (
                variables == null
                        ||
                        variables.isNull()
        ) {

            return;
        }

        ObjectNode normalized =
                objectMapper.createObjectNode();

        if (
                variables.isObject()
        ) {

            Iterator<Map.Entry<String, JsonNode>> fields =
                    variables.fields();

            while (
                    fields.hasNext()
            ) {

                Map.Entry<String, JsonNode> field =
                        fields.next();

                normalized.put(
                        field.getKey(),
                        stringifyVariable(
                                field.getValue()
                        )
                );
            }

        } else {

            normalized.put(
                    "value",
                    stringifyVariable(variables)
            );
        }

        root.set(
                "variables",
                normalized
        );
    }

    private String stringifyVariable(
            JsonNode node
    ) {

        try {

            if (
                    node == null
                            ||
                            node.isNull()
            ) {

                return "";
            }

            if (
                    node.isValueNode()
            ) {

                return node.asText();
            }

            return objectMapper.writeValueAsString(node);

        } catch (Exception ignored) {

            return "";
        }
    }

    private String formatForLog(
            GPTIntentResponse response
    ) {

        try {

            ObjectNode node =
                    objectMapper.valueToTree(response);

            JsonNode variables =
                    node.get("variables");

            if (
                    variables instanceof ObjectNode objectNode
            ) {

                Iterator<Map.Entry<String, JsonNode>> fields =
                        objectNode.fields();

                List<String> sensitiveKeys =
                        new ArrayList<>();

                while (
                        fields.hasNext()
                ) {

                    Map.Entry<String, JsonNode> field =
                            fields.next();

                    if (
                            isSensitiveVariable(
                                    field.getKey(),
                                    field.getValue()
                            )
                    ) {

                        sensitiveKeys.add(
                                field.getKey()
                        );
                    }
                }

                for (
                        String sensitiveKey
                        : sensitiveKeys
                ) {

                    objectNode.put(
                            sensitiveKey,
                            "<redacted>"
                    );
                }
            }

            return objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(node);

        } catch (Exception ignored) {

            return "<parsed>";
        }
    }

    private boolean isSensitiveVariable(
            String key,
            JsonNode value
    ) {

        String lowerKey =
                key == null
                        ? ""
                        : key.toLowerCase();

        if (
                lowerKey.contains("password")
                        ||
                        lowerKey.contains("pass")
                        ||
                        lowerKey.contains("secret")
                        ||
                        lowerKey.contains("token")
                        ||
                        lowerKey.contains("otp")
                        ||
                        lowerKey.contains("email")
                        ||
                        lowerKey.contains("username")
                        ||
                        lowerKey.equals("user")
        ) {

            return true;
        }

        String text =
                value == null
                        ? ""
                        : value.asText("");

        return text.matches(
                ".*[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}.*"
        );
    }

}
