package com.axiomai.qa.service;

import com.axiomai.ai.service.OpenAIService;
import com.axiomai.ml.AIFRepairMLContext;
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

        return repair(
                frameworkRoot,
                previousExecutionOutput,
                userInstruction,
                null
        );
    }

    public OpenAIRepairAttempt repair(
            Path frameworkRoot,
            String previousExecutionOutput,
            String userInstruction,
            AIFRepairMLContext mlContext
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
                                    frameworkRoot,
                                    files,
                                    previousExecutionOutput,
                                    userInstruction,
                                    mlContext
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
            Path frameworkRoot,
            Map<String, String> files,
            String previousExecutionOutput,
            String userInstruction,
            AIFRepairMLContext mlContext
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

        if (
                mlContext != null
        ) {

            payload.put(
                    "aifCustomMlContext",
                    redactSensitiveText(
                            truncate(
                                    mlContext.toPromptSection(),
                                    8_000
                            )
                    )
            );
        }

        GeneratedRepairInstructionAnalyzer.guidedRepairInstruction(
                        userInstruction
                )
                .ifPresent(instruction -> payload.put(
                        "guidedRepairInstruction",
                        GeneratedRepairInstructionAnalyzer.toPromptMap(
                                instruction
                        )
                ));

        Map<String, Object> locatorRepairEvidence =
                locatorRepairEvidence(
                        frameworkRoot,
                        files,
                        previousExecutionOutput,
                        userInstruction
                );

        if (
                !locatorRepairEvidence.isEmpty()
        ) {

            payload.put(
                    "locatorRepairEvidence",
                    locatorRepairEvidence
            );
        }

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
                - If the user says the test typed into the wrong textbox/input/field, classify it as a locator/field-target mismatch before considering missing step definitions or assertion changes.
                - If guidedRepairInstruction is present, treat it as the highest-priority user repair target. FIELD_LOCATOR means update the generated page object selector/resolution for that field; ASSERTION_EXPECTATION means update the generated assertion expectation; STEP_REMOVAL means remove the matching invalid generated Gherkin step.
                - For locator/field-target mismatches, inspect locatorRepairEvidence, generated enter targets, runtime action evidence, screenshots/DOM artifacts, selector lists, and weak locator findings. Repair the wrong .feature target or the page-object selector/resolution strategy that selected the wrong element.
                - Do not satisfy a wrong-textbox complaint by only adding missing Cucumber step definitions. Missing steps may also be repaired, but only after the field target or locator mismatch is addressed.
                - If a step clicks a label that is not present in the application (for example the feature says click "search flights" but the crawler/runtime evidence shows the actual submit action is "Continue"), update the .feature step to the real observed label. Do not add a page-object alias for a control that does not exist.
                - For "Unable to resolve element: <target>", first decide whether <target> is a bad generated Gherkin target. If it is, repair the feature file. Only patch GeneratedPage.java when the target is a real UI label/field and the locator strategy is missing.
                - For custom combobox/autocomplete/select widgets, do not treat missing selected text as a pure assertion-text mismatch. Repair the page object to type into the focused combobox input, click a matching visible option when present, press ArrowDown/Enter only when options exist, and assert selected chips/single-value tokens before falling back to body text.
                - If a negative assertion follows a remove/delete action for a selected chip/token, scope the assertion to selected-value tokens. Do not fail only because the removed text still appears elsewhere in option lists, page scripts, or historical page text.
                - If a generated scenario enters several values into a target whose name says "Single", but the scenario and assertions expect multiple retained values, repair the generated feature target to the equivalent multi-value field when such a field is present.
                - If this is runtime data, invalid credentials, CAPTCHA, browser installation, or external service instability, set canRepair=false and explain why.
                - If you repair, include complete replacement content for each changed file.
                - When repairing a locator mismatch, include a locatorRepair object in the JSON response using this shape: {"repairType":"LOCATOR_MISMATCH","fieldIntent":"...","oldTargetOrSelector":"...","newTargetOrSelector":"...","evidence":["..."]}.

                Response schema:
                {
                  "canRepair": true,
                  "failureSummary": "specific failure in one sentence",
                  "failureDetails": ["what failed and why"],
                  "changes": ["exact repair made"],
                  "locatorRepair": {"repairType":"LOCATOR_MISMATCH","fieldIntent":"optional","oldTargetOrSelector":"optional","newTargetOrSelector":"optional","evidence":["optional"]},
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

    private Map<String, Object> locatorRepairEvidence(
            Path frameworkRoot,
            Map<String, String> files,
            String previousExecutionOutput,
            String userInstruction
    ) {

        Map<String, Object> evidence =
                new LinkedHashMap<>();

        String combined =
                String.join(
                        System.lineSeparator(),
                        userInstruction == null ? "" : userInstruction,
                        previousExecutionOutput == null ? "" : previousExecutionOutput,
                        files.values()
                                .stream()
                                .limit(8)
                                .reduce(
                                        "",
                                        (left, right) -> left + System.lineSeparator() + right
                                )
                );

        List<Map<String, Object>> weakLocators =
                new ArrayList<>();

        List<Map<String, String>> enterTargets =
                new ArrayList<>();

        for (
                Map.Entry<String, String> entry
                : files.entrySet()
        ) {

            if (
                    entry.getKey()
                            .endsWith(".feature")
            ) {

                enterTargets.addAll(
                        GeneratedLocatorRepairAnalyzer.enterTargets(
                                entry.getValue()
                        )
                );
            }

            for (
                    GeneratedLocatorRepairAnalyzer.WeakLocatorFinding finding
                    : GeneratedLocatorRepairAnalyzer.weakLocatorFindings(
                            entry.getKey(),
                            entry.getValue()
                    )
            ) {

                Map<String, Object> item =
                        new LinkedHashMap<>();

                item.put(
                        "path",
                        finding.path()
                );
                item.put(
                        "line",
                        finding.line()
                );
                item.put(
                        "snippet",
                        redactSensitiveText(
                                truncate(
                                        finding.snippet(),
                                        800
                                )
                        )
                );
                item.put(
                        "reason",
                        finding.reason()
                );

                weakLocators.add(item);
            }
        }

        boolean hasMismatchEvidence =
                GeneratedLocatorRepairAnalyzer.hasLocatorMismatchEvidence(
                        combined
                );

        if (
                hasMismatchEvidence
        ) {

            evidence.put(
                    "failureClassification",
                    GeneratedLocatorRepairAnalyzer.classifyFailure(combined)
                            .name()
            );
            evidence.put(
                    "recommendedRepair",
                    GeneratedLocatorRepairAnalyzer.recommendedRepair(combined)
                            .name()
            );
            evidence.put(
                    "userComplaintLocatorMismatch",
                    GeneratedLocatorRepairAnalyzer.isLocatorMismatchComplaint(
                            userInstruction
                    )
            );
        }

        if (
                !enterTargets.isEmpty()
        ) {

            evidence.put(
                    "generatedEnterTargets",
                    enterTargets.stream()
                            .limit(80)
                            .toList()
            );
        }

        if (
                !weakLocators.isEmpty()
        ) {

            evidence.put(
                    "weakLocatorFindings",
                    weakLocators.stream()
                            .limit(40)
                            .toList()
            );
        }

        Path target =
                frameworkRoot.resolve("target");

        addTargetEvidenceFile(
                evidence,
                "runtimeActionEvidence",
                target.resolve("aif-runtime/action-evidence.json"),
                16_000
        );

        addTargetEvidenceFile(
                evidence,
                "lastAssertionFailure",
                target.resolve("aif-last-assertion-failure.txt"),
                8_000
        );

        List<String> artifacts =
                runtimeArtifacts(target);

        if (
                !artifacts.isEmpty()
        ) {

            evidence.put(
                    "runtimeArtifacts",
                    artifacts
            );
        }

        if (
                evidence.containsKey("runtimeActionEvidence")
                        ||
                        evidence.containsKey("lastAssertionFailure")
                        ||
                        !weakLocators.isEmpty()
        ) {

            evidence.putIfAbsent(
                    "failureClassification",
                    GeneratedLocatorRepairAnalyzer.classifyFailure(combined)
                            .name()
            );
            evidence.putIfAbsent(
                    "recommendedRepair",
                    GeneratedLocatorRepairAnalyzer.recommendedRepair(combined)
                            .name()
            );
        }

        return evidence;
    }

    private void addTargetEvidenceFile(
            Map<String, Object> evidence,
            String key,
            Path path,
            int maxChars
    ) {

        if (
                path == null
                        ||
                        !Files.isRegularFile(path)
        ) {

            return;
        }

        String value =
                readFile(path);

        if (
                value.isBlank()
        ) {

            return;
        }

        evidence.put(
                key,
                redactSensitiveText(
                        truncate(
                                value,
                                maxChars
                        )
                )
        );
    }

    private List<String> runtimeArtifacts(
            Path target
    ) {

        if (
                target == null
                        ||
                        !Files.isDirectory(target)
        ) {

            return List.of();
        }

        List<String> artifacts =
                new ArrayList<>();

        try (
                Stream<Path> paths =
                        Files.walk(target)
        ) {

            paths.filter(Files::isRegularFile)
                    .filter(path -> {
                        String name =
                                path.getFileName()
                                        .toString()
                                        .toLowerCase();

                        return name.endsWith(".png")
                                ||
                                name.endsWith(".html")
                                ||
                                name.endsWith(".json");
                    })
                    .filter(path -> path.toString()
                            .contains("aif-runtime"))
                    .sorted()
                    .limit(80)
                    .forEach(path -> artifacts.add(
                            target.relativize(path)
                                    .toString()
                                    .replace("\\", "/")
                    ));
        } catch (IOException ignored) {

        }

        return artifacts;
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
