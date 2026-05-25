package com.axiomai.qa.service;

import com.axiomai.ai.service.OpenAIService;
import com.axiomai.security.SensitiveLogSanitizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class OpenAIGeneratedTestRepairService {

    private static final int MAX_FILE_CHARS =
            32_000;

    private static final int MAX_PROMPT_OUTPUT_CHARS =
            60_000;

    private static final Set<String> REPAIRABLE_EXTENSIONS =
            Set.of(
                    ".feature",
                    ".java",
                    ".xml"
            );

    private final OpenAIService openAIService;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public OpenAIRepairAttempt repair(
            Path frameworkRoot,
            String previousExecutionOutput,
            String userInstruction
    ) {

        if (
                frameworkRoot == null
                        ||
                        !Files.exists(frameworkRoot)
        ) {

            return OpenAIRepairAttempt.unavailable(
                    "Generated framework root is not available."
            );
        }

        try {

            Map<String, String> files =
                    collectRepairableFiles(frameworkRoot);

            if (
                    files.isEmpty()
            ) {

                return OpenAIRepairAttempt.unavailable(
                        "No generated framework files were available for OpenAI repair."
                );
            }

            String response =
                    openAIService.ask(
                            repairPrompt(
                                    files,
                                    previousExecutionOutput,
                                    userInstruction
                            )
                    );

            if (
                    response == null
                            ||
                            response.isBlank()
                            ||
                            response.equalsIgnoreCase("OpenAI request failed.")
            ) {

                return OpenAIRepairAttempt.unavailable(
                        "OpenAI repair was unavailable or the API request failed."
                );
            }

            return applyOpenAIResponse(
                    frameworkRoot,
                    files.keySet(),
                    response
            );

        } catch (Exception e) {

            return OpenAIRepairAttempt.unavailable(
                    "OpenAI repair failed: "
                            + SensitiveLogSanitizer.redact(
                            e.getMessage()
                    )
            );
        }
    }

    private String repairPrompt(
            Map<String, String> files,
            String previousExecutionOutput,
            String userInstruction
    ) throws IOException {

        Map<String, Object> payload =
                new LinkedHashMap<>();

        payload.put(
                "userInstruction",
                redactSensitiveText(
                        truncate(
                                userInstruction,
                                6_000
                        )
                )
        );

        payload.put(
                "failureOutput",
                redactSensitiveText(
                        truncate(
                                previousExecutionOutput,
                                MAX_PROMPT_OUTPUT_CHARS
                        )
                )
        );

        List<Map<String, String>> filePayload =
                new ArrayList<>();

        for (
                Map.Entry<String, String> entry
                : files.entrySet()
        ) {

            filePayload.add(
                    Map.of(
                            "path",
                            entry.getKey(),
                            "content",
                            redactSensitiveText(
                                    truncate(
                                            entry.getValue(),
                                            MAX_FILE_CHARS
                                    )
                            )
                    )
            );
        }

        payload.put(
                "files",
                filePayload
        );

        return """
                You are repairing a generated Java Cucumber + Playwright automation framework.

                Analyze the failed generated test output and the generated framework files.
                Return a minimal, production-safe repair that updates generated files.

                Rules:
                - Return ONLY valid JSON. Do not use Markdown fences.
                - Do not include passwords, usernames, emails, JWTs, auth headers, API keys, Supabase keys, or OpenAI keys.
                - Do not invent test credentials or hard-code secrets.
                - Only update files listed in the input.
                - Preserve existing project structure, package names, Cucumber step style, and Java syntax.
                - Prefer fixing the generated feature, page object, or step definitions over deleting assertions blindly.
                - If a step clicks a label that is not present in the application (for example the feature says click "search flights" but the crawler/runtime evidence shows the actual submit action is "Continue"), update the .feature step to the real observed label. Do not add a page-object alias for a control that does not exist.
                - For "Unable to resolve element: <target>", first decide whether <target> is a bad generated Gherkin target. If it is, repair the feature file. Only patch GeneratedPage.java when the target is a real UI label/field and the locator strategy is missing.
                - If this is runtime data, invalid credentials, CAPTCHA, browser installation, or external service instability, set canRepair=false and explain why.
                - If you repair, include complete replacement content for each changed file.

                Response schema:
                {
                  "canRepair": true,
                  "failureSummary": "specific failure in one sentence",
                  "failureDetails": ["what failed and why"],
                  "changes": ["exact repair made"],
                  "files": [
                    {
                      "path": "relative/path/from/framework/root",
                      "content": "complete updated file content"
                    }
                  ]
                }

                Input JSON:
                """
                + objectMapper.writeValueAsString(payload);
    }

    private OpenAIRepairAttempt applyOpenAIResponse(
            Path frameworkRoot,
            Set<String> allowedPaths,
            String response
    ) throws IOException {

        JsonNode root =
                objectMapper.readTree(
                        extractJson(response)
                );

        if (
                !root.path("canRepair")
                        .asBoolean(false)
        ) {

            return OpenAIRepairAttempt.unavailable(
                    firstNonBlank(
                            root.path("failureSummary")
                                    .asText(),
                            "OpenAI did not find a safe generated-framework repair."
                    )
            );
        }

        JsonNode files =
                root.path("files");

        if (
                !files.isArray()
                        ||
                        files.isEmpty()
        ) {

            return OpenAIRepairAttempt.unavailable(
                    "OpenAI repair did not return any file updates."
            );
        }

        Map<Path, String> updates =
                new LinkedHashMap<>();

        for (
                JsonNode file
                : files
        ) {

            String relativePath =
                    file.path("path")
                            .asText("");

            String content =
                    file.path("content")
                            .asText(null);

            validateRepairPath(
                    relativePath,
                    allowedPaths
            );

            if (
                    content == null
            ) {

                throw new IOException(
                        "OpenAI repair omitted content for "
                                + relativePath
                );
            }

            Path target =
                    frameworkRoot.resolve(relativePath)
                            .normalize();

            if (
                    !target.startsWith(
                            frameworkRoot.normalize()
                    )
            ) {

                throw new IOException(
                        "OpenAI repair attempted to write outside the generated framework."
                );
            }

            updates.put(
                    target,
                    content
            );
        }

        List<String> changedFiles =
                new ArrayList<>();

        for (
                Map.Entry<Path, String> update
                : updates.entrySet()
        ) {

            String existing =
                    Files.readString(
                            update.getKey()
                    );

            if (
                    !existing.equals(
                            update.getValue()
                    )
            ) {

                Files.writeString(
                        update.getKey(),
                        update.getValue()
                );

                changedFiles.add(
                        update.getKey()
                                .toAbsolutePath()
                                .normalize()
                                .toString()
                );
            }
        }

        if (
                changedFiles.isEmpty()
        ) {

            return OpenAIRepairAttempt.unavailable(
                    "OpenAI repair returned no effective file changes."
            );
        }

        GeneratedFeatureRepairService.RepairResult result =
                GeneratedFeatureRepairService.RepairResult.builder()
                        .changed(true)
                        .changedFiles(changedFiles)
                        .changes(
                                stringList(
                                        root.path("changes")
                                )
                        )
                        .failureSummary(
                                firstNonBlank(
                                        root.path("failureSummary")
                                                .asText(),
                                        "OpenAI repaired the latest generated test failure."
                                )
                        )
                        .failureDetails(
                                stringList(
                                        root.path("failureDetails")
                                )
                        )
                        .repairSource("OpenAI")
                        .repairGuidance("")
                        .build();

        return OpenAIRepairAttempt.repaired(result);
    }

    private Map<String, String> collectRepairableFiles(
            Path frameworkRoot
    ) throws IOException {

        Map<String, String> files =
                new LinkedHashMap<>();

        try (
                Stream<Path> paths =
                        Files.walk(frameworkRoot)
        ) {

            paths.filter(Files::isRegularFile)
                    .filter(path -> isRepairableFile(frameworkRoot, path))
                    .sorted()
                    .forEach(path -> files.put(
                            frameworkRoot.relativize(path)
                                    .toString()
                                    .replace("\\", "/"),
                            readFile(path)
                    ));
        }

        return files;
    }

    private boolean isRepairableFile(
            Path frameworkRoot,
            Path path
    ) {

        String relative =
                frameworkRoot.relativize(path)
                        .toString()
                        .replace("\\", "/");

        if (
                relative.startsWith("target/")
                        ||
                        relative.startsWith(".git/")
        ) {

            return false;
        }

        String fileName =
                path.getFileName()
                        .toString()
                        .toLowerCase();

        return REPAIRABLE_EXTENSIONS.stream()
                .anyMatch(fileName::endsWith);
    }

    private String readFile(
            Path path
    ) {

        try {

            return Files.readString(path);

        } catch (IOException e) {

            return "";
        }
    }

    private void validateRepairPath(
            String relativePath,
            Set<String> allowedPaths
    ) throws IOException {

        if (
                relativePath == null
                        ||
                        relativePath.isBlank()
                        ||
                        relativePath.startsWith("/")
                        ||
                        relativePath.contains("..")
                        ||
                        !allowedPaths.contains(relativePath)
        ) {

            throw new IOException(
                    "OpenAI repair returned an unsafe or unknown file path: "
                            + relativePath
            );
        }
    }

    private List<String> stringList(
            JsonNode node
    ) {

        if (
                !node.isArray()
        ) {

            return List.of();
        }

        List<String> values =
                new ArrayList<>();

        for (
                JsonNode item
                : node
        ) {

            String value =
                    item.asText("");

            if (
                    !value.isBlank()
            ) {

                values.add(
                        redactSensitiveText(value)
                );
            }
        }

        return values.stream()
                .distinct()
                .toList();
    }

    private String extractJson(
            String response
    ) {

        String trimmed =
                response == null
                        ? ""
                        : response.trim();

        if (
                trimmed.startsWith("```")
        ) {

            trimmed =
                    trimmed.replaceFirst(
                                    "^```(?:json)?",
                                    ""
                            )
                            .replaceFirst(
                                    "```$",
                                    ""
                            )
                            .trim();
        }

        int firstBrace =
                trimmed.indexOf('{');

        int lastBrace =
                trimmed.lastIndexOf('}');

        if (
                firstBrace >= 0
                        &&
                        lastBrace > firstBrace
        ) {

            return trimmed.substring(
                    firstBrace,
                    lastBrace + 1
            );
        }

        return trimmed;
    }

    private String truncate(
            String value,
            int maxChars
    ) {

        if (
                value == null
        ) {

            return "";
        }

        if (
                value.length() <= maxChars
        ) {

            return value;
        }

        return value.substring(
                0,
                maxChars
        ) + "\n...[truncated]";
    }

    private String redactSensitiveText(
            String value
    ) {

        if (
                value == null
        ) {

            return "";
        }

        String redacted =
                value.replaceAll(
                        "(?i)(authorization\\s*(?:[:=]|\\bis\\b|\\bas\\b)\\s*)(?:Bearer\\s+)?[^\\s,}\\]\"]+",
                        "$1<redacted>"
                );

        redacted =
                SensitiveLogSanitizer.redact(redacted);

        redacted =
                redacted.replaceAll(
                        "(?i)(username|user|email)\\s*(?:[:=]|\\bis\\b|\\bas\\b)\\s*([^\\s,}\\]\"]+|\"[^\"]*\")",
                        "$1=<redacted>"
                );

        redacted =
                redacted.replaceAll(
                        "(?i)(\"(?:username|password|email|token|authorization)\"\\s*:\\s*\")([^\"]*)\"",
                        "$1<redacted>\""
                );

        redacted =
                redactStepInputValues(redacted);

        return redacted;
    }

    private String redactStepInputValues(
            String value
    ) {

        Pattern pattern =
                Pattern.compile(
                        "(?i)(user enters \")([^\"]+)(\" into \"(?:username|password|email|pass|token|otp)\")"
                );

        Matcher matcher =
                pattern.matcher(value);

        StringBuffer redacted =
                new StringBuffer();

        while (
                matcher.find()
        ) {

            String inputValue =
                    matcher.group(2);

            String replacement =
                    inputValue.startsWith("${")
                            ? matcher.group(0)
                            : matcher.group(1)
                            + "<redacted>"
                            + matcher.group(3);

            matcher.appendReplacement(
                    redacted,
                    Matcher.quoteReplacement(replacement)
            );
        }

        matcher.appendTail(redacted);

        return redacted.toString();
    }

    private String firstNonBlank(
            String first,
            String second
    ) {

        if (
                first != null
                        &&
                        !first.isBlank()
        ) {

            return first;
        }

        return second;
    }

    @Getter
    @Builder
    public static class OpenAIRepairAttempt {

        private boolean repaired;

        private GeneratedFeatureRepairService.RepairResult repairResult;

        private String fallbackReason;

        static OpenAIRepairAttempt repaired(
                GeneratedFeatureRepairService.RepairResult result
        ) {

            return OpenAIRepairAttempt.builder()
                    .repaired(true)
                    .repairResult(result)
                    .build();
        }

        static OpenAIRepairAttempt unavailable(
                String reason
        ) {

            return OpenAIRepairAttempt.builder()
                    .repaired(false)
                    .fallbackReason(reason)
                    .build();
        }
    }
}
