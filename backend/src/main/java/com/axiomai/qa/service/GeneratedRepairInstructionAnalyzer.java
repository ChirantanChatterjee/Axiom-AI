package com.axiomai.qa.service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeneratedRepairInstructionAnalyzer {

    private GeneratedRepairInstructionAnalyzer() {
    }

    public static boolean isGuidedRepairInstruction(
            String instruction
    ) {

        return guidedRepairInstruction(instruction)
                .isPresent();
    }

    public static Optional<GuidedRepairInstruction> guidedRepairInstruction(
            String instruction
    ) {

        Optional<String> invalidStep =
                invalidStepText(instruction);

        if (
                invalidStep.isPresent()
        ) {

            return Optional.of(
                    new GuidedRepairInstruction(
                            "STEP_REMOVAL",
                            invalidStep.get(),
                            "",
                            "Remove the invalid generated Gherkin step that matches the quoted text.",
                            "User identified an invalid generated step."
                    )
            );
        }

        Optional<String> actualExpectation =
                actualExpectationText(instruction);

        if (
                actualExpectation.isPresent()
        ) {

            return Optional.of(
                    new GuidedRepairInstruction(
                            "ASSERTION_EXPECTATION",
                            "assertion",
                            actualExpectation.get(),
                            "Replace the failed generated assertion with the quoted actual expectation.",
                            "User provided the actual assertion expectation."
                    )
            );
        }

        Optional<FieldLocatorInstruction> fieldLocator =
                fieldLocatorInstruction(instruction);

        if (
                fieldLocator.isPresent()
        ) {

            return Optional.of(
                    new GuidedRepairInstruction(
                            "FIELD_LOCATOR",
                            fieldLocator.get()
                                    .fieldName(),
                            fieldLocator.get()
                                    .selector(),
                            fieldLocator.get()
                                    .selector()
                                    .isBlank()
                                    ? "Repair the generated page-object locator/resolution strategy for the named field."
                                    : "Update the generated locator for the named field to the quoted selector.",
                            "User identified an incorrect field locator."
                    )
            );
        }

        return Optional.empty();
    }

    public static Optional<String> invalidStepText(
            String instruction
    ) {

        if (
                instruction == null
                        ||
                        instruction.isBlank()
        ) {

            return Optional.empty();
        }

        for (
                Pattern pattern
                : new Pattern[]{
                Pattern.compile(
                        "(?i)(?:remove|delete)\\s+(?:the\\s+)?(?:invalid\\s+|wrong\\s+|bad\\s+|generated\\s+)?step\\s+\"([^\"]+)\""
                ),
                Pattern.compile(
                        "(?i)step\\s+\"([^\"]+)\"[^\\r\\n]{0,160}\\b(?:invalid|wrong|bad|incorrect|unnecessary|not\\s+needed|does\\s+not\\s+exist)\\b[^\\r\\n]{0,160}\\b(?:remove|delete|drop)?\\b"
                )
        }
        ) {

            Matcher matcher =
                    pattern.matcher(instruction);

            if (
                    matcher.find()
            ) {

                return Optional.of(
                                matcher.group(1)
                                        .trim()
                        )
                        .filter(value -> !value.isBlank());
            }
        }

        return Optional.empty();
    }

    public static Optional<String> actualExpectationText(
            String instruction
    ) {

        if (
                instruction == null
                        ||
                        instruction.isBlank()
        ) {

            return Optional.empty();
        }

        for (
                Pattern pattern
                : new Pattern[]{
                Pattern.compile(
                        "(?i)(?:assertion|expectation|expected\\s+text|expected\\s+message)[^\"\\r\\n]{0,160}(?:actual|correct|real)?[^\"\\r\\n]{0,100}(?:should|must)?\\s*(?:be|say|show|contain|equal|equals)\\s+\"([^\"]+)\""
                ),
                Pattern.compile(
                        "(?i)(?:actual|correct|real)\\s+(?:assertion\\s+)?(?:expectation|expected\\s+text|expected\\s+message|assertion|text|message)[^\"\\r\\n]{0,100}(?:is|was|should\\s+be)\\s+\"([^\"]+)\""
                )
        }
        ) {

            Matcher matcher =
                    pattern.matcher(instruction);

            if (
                    matcher.find()
            ) {

                return Optional.of(
                                normalizeCapturedText(
                                        matcher.group(1)
                                )
                        )
                        .filter(value -> !value.isBlank());
            }
        }

        return Optional.empty();
    }

    public static Optional<FieldLocatorInstruction> fieldLocatorInstruction(
            String instruction
    ) {

        if (
                instruction == null
                        ||
                        instruction.isBlank()
        ) {

            return Optional.empty();
        }

        for (
                Pattern pattern
                : new Pattern[]{
                Pattern.compile(
                        "(?i)(?:field\\s+locator|locator\\s+(?:used\\s+)?(?:for|of)|selector\\s+(?:used\\s+)?(?:for|of))[^\"\\r\\n]{0,80}\"([^\"]+)\"[^\\r\\n]{0,180}\\b(?:field|input|textbox|text\\s+box)?[^\\r\\n]{0,180}\\b(?:incorrect|wrong|invalid|bad|stale|broken|fix|repair|update)\\b"
                ),
                Pattern.compile(
                        "(?i)\"([^\"]+)\"\\s+(?:field|input|textbox|text\\s+box)[^\\r\\n]{0,120}\\b(?:locator|selector)[^\\r\\n]{0,120}\\b(?:incorrect|wrong|invalid|bad|stale|broken|fix|repair|update)\\b"
                )
        }
        ) {

            Matcher matcher =
                    pattern.matcher(instruction);

            if (
                    matcher.find()
            ) {

                return Optional.of(
                        new FieldLocatorInstruction(
                                matcher.group(1)
                                        .trim(),
                                explicitSelector(instruction)
                                        .orElse("")
                        )
                );
            }
        }

        return Optional.empty();
    }

    public static Map<String, Object> toPromptMap(
            GuidedRepairInstruction instruction
    ) {

        Map<String, Object> map =
                new LinkedHashMap<>();

        if (
                instruction == null
        ) {

            return map;
        }

        map.put(
                "repairArea",
                instruction.repairArea()
        );
        map.put(
                "target",
                instruction.target()
        );
        map.put(
                "replacement",
                instruction.replacement()
        );
        map.put(
                "requestedAction",
                instruction.requestedAction()
        );
        map.put(
                "summary",
                instruction.summary()
        );

        return map;
    }

    private static Optional<String> explicitSelector(
            String instruction
    ) {

        String lower =
                instruction.toLowerCase(Locale.ROOT);

        if (
                !lower.contains("selector")
                        &&
                        !lower.contains("locator")
        ) {

            return Optional.empty();
        }

        Matcher matcher =
                Pattern.compile(
                                "(?i)(?:use|to|with|should\\s+be|is)\\s+(?:selector\\s+|locator\\s+)?\"([^\"]+)\""
                        )
                        .matcher(instruction);

        String last =
                "";

        while (
                matcher.find()
        ) {

            last =
                    matcher.group(1)
                            .trim();
        }

        return Optional.of(last)
                .filter(value -> !value.isBlank());
    }

    private static String normalizeCapturedText(
            String text
    ) {

        return text == null
                ? ""
                : text.trim()
                .replaceFirst(
                        "^[\\s:=\\-]+>?",
                        ""
                )
                .trim();
    }

    public record GuidedRepairInstruction(
            String repairArea,
            String target,
            String replacement,
            String requestedAction,
            String summary
    ) {
    }

    public record FieldLocatorInstruction(
            String fieldName,
            String selector
    ) {
    }
}
