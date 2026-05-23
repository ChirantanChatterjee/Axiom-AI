package com.axiomai.qa.service;

import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class GeneratedFeatureRepairService {

    private static final String PARABANK_HOME =
            "https://parabank.parasoft.com/parabank/index.htm";

    public RepairResult repair(
            Path frameworkRoot
    ) {

        return repair(
                frameworkRoot,
                "",
                ""
        );
    }

    public RepairResult repair(
            Path frameworkRoot,
            String previousExecutionOutput
    ) {

        return repair(
                frameworkRoot,
                previousExecutionOutput,
                ""
        );
    }

    public RepairResult repair(
            Path frameworkRoot,
            String previousExecutionOutput,
            String userInstruction
    ) {

        String latestOutput =
                combinedOutput(
                        previousExecutionOutput,
                        latestExecutionOutput(frameworkRoot)
                                .orElse("")
                );

        List<String> changedFiles =
                new ArrayList<>();

        List<String> changes =
                new ArrayList<>();

        try {

            for (
                    Path featureFile
                    : featureFiles(frameworkRoot)
            ) {

                String original =
                        Files.readString(featureFile);

                FeatureRepair repair =
                        repairFeatureContent(
                                original,
                                latestOutput,
                                userInstruction
                        );

                if (
                        repair.changed()
                ) {

                    Files.writeString(
                            featureFile,
                            repair.content()
                    );

                    changedFiles.add(
                            featureFile.toAbsolutePath()
                                    .normalize()
                                    .toString()
                    );

                    changes.addAll(
                            repair.changes()
                    );
                }
            }

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to repair generated feature files.",
                    e
            );
        }

        return RepairResult.builder()
                .changed(
                        !changedFiles.isEmpty()
                )
                .changedFiles(changedFiles)
                .changes(
                        changes.stream()
                                .distinct()
                                .toList()
                )
                .failureSummary(
                        failureSummary(latestOutput)
                )
                .build();
    }

    FeatureRepair repairFeatureContent(
            String content,
            String latestOutput
    ) {

        return repairFeatureContent(
                content,
                latestOutput,
                ""
        );
    }

    FeatureRepair repairFeatureContent(
            String content,
            String latestOutput,
            String userInstruction
    ) {

        if (
                content == null
        ) {

            return new FeatureRepair(
                    "",
                    List.of()
            );
        }

        FeatureRepair assertionTextRepair =
                repairUserProvidedAssertionText(
                        content,
                        latestOutput,
                        userInstruction
                );

        if (
                assertionTextRepair.changed()
        ) {

            return assertionTextRepair;
        }

        FeatureRepair genericRepair =
                repairMissingIntermediateAssertions(
                        content,
                        latestOutput
                );

        String repairedContent =
                genericRepair.content();

        List<String> allChanges =
                new ArrayList<>(
                        genericRepair.changes()
                );

        if (
                shouldRepairParaBankBillPay(
                        repairedContent,
                        latestOutput
                )
        ) {

            FeatureRepair siteRepair =
                    repairParaBankBillPay(
                            repairedContent
                    );

            repairedContent =
                    siteRepair.content();

            allChanges.addAll(
                    siteRepair.changes()
            );
        }

        return new FeatureRepair(
                repairedContent,
                content.equals(repairedContent)
                        ? List.of()
                        : allChanges.stream()
                        .distinct()
                        .toList()
        );
    }

    private boolean shouldRepairParaBankBillPay(
            String content,
            String latestOutput
    ) {

        String lowerContent =
                content == null
                        ? ""
                        : content.toLowerCase();

        String lowerOutput =
                latestOutput == null
                        ? ""
                        : latestOutput.toLowerCase();

        if (
                !lowerContent.contains("parabank")
                        ||
                        !lowerContent.contains("bill")
                        ||
                        !lowerContent.contains("pay")
        ) {

            return false;
        }

        return lowerOutput.contains("unable to resolve element: bill pay")
                ||
                lowerOutput.contains("unable to resolve element: billpay")
                ||
                lowerOutput.contains("unable to resolve element: bill payment")
                ||
                lowerOutput.contains("unable to resolve element: send payment button");
    }

    private FeatureRepair repairParaBankBillPay(
            String content
    ) {

        List<String> lines =
                content.replace(
                                "https://parabank.parasoft.com/parabank/admin.htm",
                                PARABANK_HOME
                        )
                        .replace(
                                "https://parabank.parasoft.com/parabank/overview.htm",
                                PARABANK_HOME
                        )
                        .lines()
                        .toList();

        List<String> changes =
                new ArrayList<>();

        StringBuilder repaired =
                new StringBuilder();

        for (
                int i = 0;
                i < lines.size();
                i++
        ) {

            String line =
                    lines.get(i);

            Matcher billPayClick =
                    Pattern.compile(
                                    "^(\\s*)(When|And) user clicks \"(?:bill\\s*pay|bill\\s*payment|billpay)\"\\s*$",
                                    Pattern.CASE_INSENSITIVE
                            )
                            .matcher(line);

            if (
                    billPayClick.matches()
            ) {

                String canonicalBillPayClick =
                        billPayClick.group(1)
                                + billPayClick.group(2)
                                + " user clicks \"Bill Pay\"";

                if (
                        !line.equals(canonicalBillPayClick)
                ) {

                    changes.add(
                            "Canonicalized the ParaBank Bill Pay navigation target."
                    );
                }

                line =
                        canonicalBillPayClick;
            }

            repaired.append(line)
                    .append(System.lineSeparator());

            if (
                    line.trim()
                            .startsWith("Given user launches")
                            &&
                            isBillPayScenario(
                                    lines,
                                    i
                            )
            ) {

                if (
                        !scenarioContains(
                                lines,
                                i,
                                "login button"
                        )
                ) {

                    repaired.append("  When user enters \"${username}\" into \"username\"")
                            .append(System.lineSeparator());
                    repaired.append("  And user enters \"${password}\" into \"password\"")
                            .append(System.lineSeparator());
                    repaired.append("  And user clicks \"login button\"")
                            .append(System.lineSeparator());

                    changes.add(
                            "Inserted ParaBank login before Bill Pay navigation."
                    );
                }

                if (
                        !scenarioContainsBillPayClick(
                                lines,
                                i
                        )
                ) {

                    repaired.append("  When user clicks \"Bill Pay\"")
                            .append(System.lineSeparator());

                    changes.add(
                            "Inserted ParaBank Bill Pay navigation before bill-pay form steps."
                    );
                }
            }

            Matcher accountStep =
                    Pattern.compile(
                                    "^(\\s*)(When|And) user enters \"([^\"]+)\" into \"account\"\\s*$",
                                    Pattern.CASE_INSENSITIVE
                            )
                            .matcher(line);

            if (
                    accountStep.matches()
                            &&
                            !scenarioContains(
                                    lines,
                                    i,
                                    "verify account"
                            )
            ) {

                repaired.append(accountStep.group(1))
                        .append("And user enters \"")
                        .append(accountStep.group(3))
                        .append("\" into \"verify account\"")
                        .append(System.lineSeparator());

                changes.add(
                        "Added the ParaBank verify-account step after account entry."
                );
            }
        }

        if (
                content.contains("admin.htm")
                        ||
                        content.contains("overview.htm")
        ) {

            changes.add(
                    "Changed ParaBank bill-pay launch URL to the home page instead of admin/overview."
            );
        }

        String repairedContent =
                repaired.toString();

        return new FeatureRepair(
                repairedContent,
                content.equals(repairedContent)
                        ? List.of()
                        : changes
        );
    }

    private FeatureRepair repairMissingIntermediateAssertions(
            String content,
            String latestOutput
    ) {

        List<String> missingTexts =
                missingExpectedTexts(latestOutput);

        if (
                missingTexts.isEmpty()
        ) {

            return new FeatureRepair(
                    content,
                    List.of()
            );
        }

        List<String> lines =
                content.lines()
                        .toList();

        List<String> changes =
                new ArrayList<>();

        StringBuilder repaired =
                new StringBuilder();

        for (
                int i = 0;
                i < lines.size();
                i++
        ) {

            String line =
                    lines.get(i);

            Matcher assertion =
                    Pattern.compile(
                                    "^(\\s*)(Then|And) user should see \"([^\"]+)\"\\s*$",
                                    Pattern.CASE_INSENSITIVE
                            )
                            .matcher(line);

            if (
                    assertion.matches()
                            &&
                            missingTexts.stream()
                                    .anyMatch(
                                            missingText -> missingText.equalsIgnoreCase(
                                                    assertion.group(3)
                                            )
                                    )
                            &&
                            hasLaterActionStep(
                                    lines,
                                    i
                            )
            ) {

                changes.add(
                        "Removed a failing intermediate assertion that was blocking later actions: "
                                + assertion.group(3)
                );

                continue;
            }

            repaired.append(line)
                    .append(System.lineSeparator());
        }

        String repairedContent =
                repaired.toString();

        return new FeatureRepair(
                repairedContent,
                content.equals(repairedContent)
                        ? List.of()
                        : changes
        );
    }

    private FeatureRepair repairUserProvidedAssertionText(
            String content,
            String latestOutput,
            String userInstruction
    ) {

        List<AssertionReplacement> replacements =
                assertionReplacements(
                        userInstruction,
                        latestOutput
                );

        if (
                replacements.isEmpty()
        ) {

            return new FeatureRepair(
                    content,
                    List.of()
            );
        }

        List<String> lines =
                content.lines()
                        .toList();

        List<String> changes =
                new ArrayList<>();

        StringBuilder repaired =
                new StringBuilder();

        for (
                String line
                : lines
        ) {

            Matcher assertion =
                    Pattern.compile(
                                    "^(\\s*)(Then|And) user should see \"([^\"]+)\"\\s*$",
                                    Pattern.CASE_INSENSITIVE
                            )
                            .matcher(line);

            if (
                    assertion.matches()
            ) {

                Optional<AssertionReplacement> replacement =
                        replacements.stream()
                                .filter(candidate -> candidate.expected()
                                        .equalsIgnoreCase(
                                                assertion.group(3)
                                        ))
                                .findFirst();

                if (
                        replacement.isPresent()
                ) {

                    String actual =
                            sanitizeStepText(
                                    replacement.get()
                                            .actual()
                            );

                    line =
                            assertion.group(1)
                                    + assertion.group(2)
                                    + " user should see \""
                                    + actual
                                    + "\"";

                    changes.add(
                            "Updated assertion text from \""
                                    + assertion.group(3)
                                    + "\" to \""
                                    + actual
                                    + "\"."
                    );
                }
            }

            repaired.append(line)
                    .append(System.lineSeparator());
        }

        String repairedContent =
                repaired.toString();

        return new FeatureRepair(
                repairedContent,
                content.equals(repairedContent)
                        ? List.of()
                        : changes.stream()
                        .distinct()
                        .toList()
        );
    }

    private List<AssertionReplacement> assertionReplacements(
            String userInstruction,
            String latestOutput
    ) {

        if (
                userInstruction == null
                        ||
                        userInstruction.isBlank()
        ) {

            return List.of();
        }

        List<AssertionReplacement> explicitReplacements =
                explicitAssertionReplacements(userInstruction);

        if (
                !explicitReplacements.isEmpty()
        ) {

            return explicitReplacements;
        }

        Optional<String> actualText =
                actualAssertionText(userInstruction);

        if (
                actualText.isEmpty()
        ) {

            return List.of();
        }

        List<String> missingTexts =
                missingExpectedTexts(latestOutput);

        if (
                missingTexts.size() == 1
        ) {

            return List.of(
                    new AssertionReplacement(
                            missingTexts.get(0),
                            actualText.get()
                    )
            );
        }

        Optional<String> expectedText =
                expectedAssertionText(userInstruction);

        if (
                expectedText.isPresent()
        ) {

            return List.of(
                    new AssertionReplacement(
                            expectedText.get(),
                            actualText.get()
                    )
            );
        }

        return inferredExpectedAssertionText(
                actualText.get(),
                missingTexts
        )
                .map(expected -> List.of(
                        new AssertionReplacement(
                                expected,
                                actualText.get()
                        )
                ))
                .orElseGet(List::of);
    }

    private List<AssertionReplacement> explicitAssertionReplacements(
            String userInstruction
    ) {

        List<AssertionReplacement> replacements =
                new ArrayList<>();

        List<Pattern> expectedThenActualPatterns =
                List.of(
                        Pattern.compile(
                                "(?i)(?:expected|assertion|asserted)[^\"\\r\\n]{0,100}\"([^\"]+)\"[^\"\\r\\n]{0,140}(?:actual(?:ly)?|should|instead|but)[^\"\\r\\n]{0,100}\"([^\"]+)\""
                        ),
                        Pattern.compile(
                                "(?i)(?:change|replace|update)[^\"\\r\\n]{0,80}\"([^\"]+)\"[^\"\\r\\n]{0,80}(?:to|with)[^\"\\r\\n]{0,80}\"([^\"]+)\""
                        )
                );

        for (
                Pattern pattern
                : expectedThenActualPatterns
        ) {

            Matcher matcher =
                    pattern.matcher(userInstruction);

            while (
                    matcher.find()
            ) {

                replacements.add(
                        new AssertionReplacement(
                                matcher.group(1)
                                        .trim(),
                                matcher.group(2)
                                        .trim()
                        )
                );
            }
        }

        Matcher actualInsteadOf =
                Pattern.compile(
                                "(?i)(?:actual(?:ly)?|should)[^\"\\r\\n]{0,100}\"([^\"]+)\"[^\"\\r\\n]{0,100}(?:instead of|not)[^\"\\r\\n]{0,80}\"([^\"]+)\""
                        )
                        .matcher(userInstruction);

        while (
                actualInsteadOf.find()
        ) {

            replacements.add(
                    new AssertionReplacement(
                            actualInsteadOf.group(2)
                                    .trim(),
                            actualInsteadOf.group(1)
                                    .trim()
                    )
            );
        }

        return replacements.stream()
                .filter(replacement -> !replacement.expected()
                        .isBlank())
                .filter(replacement -> !replacement.actual()
                        .isBlank())
                .distinct()
                .toList();
    }

    private Optional<String> actualAssertionText(
            String userInstruction
    ) {

        List<Pattern> patterns =
                List.of(
                        Pattern.compile(
                                "(?i)assertion(?:\\s+(?:sentence|text|message))?[^\"\\r\\n]{0,100}actual(?:ly)?\\s+(?:was|is)\\s+\"([^\"]+)\""
                        ),
                        Pattern.compile(
                                "(?i)(?:actual|real)\\s+(?:assertion\\s+)?(?:text|sentence|message)?\\s*(?:was|is)\\s+\"([^\"]+)\""
                        ),
                        Pattern.compile(
                                "(?i)(?:assertion|text|message)[^\"\\r\\n]{0,100}(?:should|must)\\s+(?:be|say|contain|show)\\s+\"([^\"]+)\""
                        )
                );

        for (
                Pattern pattern
                : patterns
        ) {

            Matcher matcher =
                    pattern.matcher(userInstruction);

            if (
                    matcher.find()
            ) {

                return Optional.of(
                                normalizeCapturedAssertionText(
                                        matcher.group(1)
                                )
                        )
                        .filter(actual -> !actual.isBlank());
            }
        }

        List<Pattern> unquotedPatterns =
                List.of(
                        Pattern.compile(
                                "(?i)assertion(?:\\s+(?:sentence|text|message))?[^\\r\\n]{0,100}actual(?:ly)?\\s+(?:was|is)\\s*(?:-->|:|-)?\\s*([^\"\\r\\n]+)"
                        ),
                        Pattern.compile(
                                "(?i)(?:actual|real)\\s+(?:assertion\\s+)?(?:text|sentence|message)\\s*(?:was|is)\\s*(?:-->|:|-)?\\s*([^\"\\r\\n]+)"
                        )
                );

        for (
                Pattern pattern
                : unquotedPatterns
        ) {

            Matcher matcher =
                    pattern.matcher(userInstruction);

            if (
                    matcher.find()
            ) {

                return Optional.of(
                                normalizeCapturedAssertionText(
                                        matcher.group(1)
                                )
                        )
                        .filter(actual -> !actual.isBlank());
            }
        }

        return Optional.empty();
    }

    private String normalizeCapturedAssertionText(
            String text
    ) {

        if (
                text == null
        ) {

            return "";
        }

        String normalized =
                text.trim()
                        .replaceFirst(
                                "^[\\s:=\\-]+>?",
                                ""
                        )
                        .trim();

        normalized =
                normalized.replaceFirst(
                                "(?i)(?:\\s*,\\s*|\\s+)(?:can|could|would)\\s+you\\b.*$",
                                ""
                        )
                        .trim();

        normalized =
                normalized.replaceFirst(
                                "(?i)(?:\\s*,\\s*|\\s+)please\\s+(?:fix|update|repair|change)\\b.*$",
                                ""
                        )
                        .trim();

        return normalized;
    }

    private Optional<String> inferredExpectedAssertionText(
            String actualText,
            List<String> missingTexts
    ) {

        if (
                missingTexts == null
                        ||
                        missingTexts.isEmpty()
        ) {

            return Optional.empty();
        }

        if (
                missingTexts.size() == 1
        ) {

            return Optional.of(
                    missingTexts.get(0)
            );
        }

        String lowerActual =
                actualText == null
                        ? ""
                        : actualText.toLowerCase();

        if (
                lowerActual.contains("valid number")
                        ||
                        lowerActual.contains("number")
        ) {

            Optional<String> accountAssertion =
                    firstMissingTextContaining(
                            missingTexts,
                            "account",
                            "mismatch",
                            "verify"
                    );

            if (
                    accountAssertion.isPresent()
            ) {

                return accountAssertion;
            }

            Optional<String> amountAssertion =
                    firstMissingTextContaining(
                            missingTexts,
                            "amount"
                    );

            if (
                    amountAssertion.isPresent()
            ) {

                return amountAssertion;
            }
        }

        if (
                lowerActual.contains("bill payment complete")
                        ||
                        lowerActual.contains("successful")
        ) {

            Optional<String> amountAssertion =
                    firstMissingTextContaining(
                            missingTexts,
                            "amount",
                            "validation"
                    );

            if (
                    amountAssertion.isPresent()
            ) {

                return amountAssertion;
            }
        }

        if (
                lowerActual.contains("required")
        ) {

            return firstMissingTextContaining(
                    missingTexts,
                    "required"
            );
        }

        if (
                lowerActual.contains("email")
        ) {

            return firstMissingTextContaining(
                    missingTexts,
                    "email"
            );
        }

        if (
                lowerActual.contains("password")
        ) {

            return firstMissingTextContaining(
                    missingTexts,
                    "password"
            );
        }

        if (
                lowerActual.contains("username")
                        ||
                        lowerActual.contains("user name")
                        ||
                        lowerActual.contains("user")
        ) {

            return firstMissingTextContaining(
                    missingTexts,
                    "username",
                    "user"
            );
        }

        return Optional.empty();
    }

    private Optional<String> firstMissingTextContaining(
            List<String> missingTexts,
            String... tokens
    ) {

        for (
                String missingText
                : missingTexts
        ) {

            String lowerMissingText =
                    missingText.toLowerCase();

            for (
                    String token
                    : tokens
            ) {

                if (
                        lowerMissingText.contains(token)
                ) {

                    return Optional.of(
                            missingText
                    );
                }
            }
        }

        return Optional.empty();
    }

    private Optional<String> expectedAssertionText(
            String userInstruction
    ) {

        List<Pattern> patterns =
                List.of(
                        Pattern.compile(
                                "(?i)(?:expected|old|current)\\s+(?:assertion\\s+)?(?:text|sentence|message)?\\s*(?:was|is)?\\s*\"([^\"]+)\""
                        ),
                        Pattern.compile(
                                "(?i)(?:instead of|not)\\s+\"([^\"]+)\""
                        )
                );

        for (
                Pattern pattern
                : patterns
        ) {

            Matcher matcher =
                    pattern.matcher(userInstruction);

            if (
                    matcher.find()
            ) {

                return Optional.of(
                        matcher.group(1)
                                .trim()
                );
            }
        }

        return Optional.empty();
    }

    private String sanitizeStepText(
            String text
    ) {

        if (
                text == null
        ) {

            return "";
        }

        return text.replace(
                "\"",
                "'"
        );
    }

    private boolean hasLaterActionStep(
            List<String> lines,
            int index
    ) {

        for (
                int i = index + 1;
                i < lines.size();
                i++
        ) {

            String trimmed =
                    lines.get(i)
                            .trim();

            if (
                    trimmed.startsWith("Scenario:")
                            ||
                            trimmed.startsWith("Scenario Outline:")
            ) {

                return false;
            }

            if (
                    isActionStep(trimmed)
            ) {

                return true;
            }
        }

        return false;
    }

    private boolean isActionStep(
            String trimmedLine
    ) {

        String lower =
                trimmedLine.toLowerCase();

        return lower.matches(
                "^(when|and) user (clicks|enters|selects|chooses|sets|types|uploads|launches|navigates|opens|submits) .+"
        );
    }

    private boolean scenarioContains(
            List<String> lines,
            int index,
            String needle
    ) {

        String lowerNeedle =
                needle.toLowerCase();

        int start =
                index;

        while (
                start > 0
                        &&
                        !lines.get(start)
                                .trim()
                                .startsWith("Scenario:")
        ) {

            start--;
        }

        for (
                int i = start;
                i < lines.size();
                i++
        ) {

            if (
                    i > start
                            &&
                            lines.get(i)
                                    .trim()
                                    .startsWith("Scenario:")
            ) {

                break;
            }

            if (
                    lines.get(i)
                            .toLowerCase()
                            .contains(lowerNeedle)
            ) {

                return true;
            }
        }

        return false;
    }

    private boolean isBillPayScenario(
            List<String> lines,
            int index
    ) {

        return scenarioContains(
                lines,
                index,
                "bill pay"
        )
                ||
                scenarioContains(
                        lines,
                        index,
                        "billpay"
                )
                ||
                scenarioContains(
                        lines,
                        index,
                        "bill payment"
                )
                ||
                scenarioContains(
                        lines,
                        index,
                        "send payment"
                )
                ||
                scenarioContains(
                        lines,
                        index,
                        "verify account"
                );
    }

    private boolean scenarioContainsBillPayClick(
            List<String> lines,
            int index
    ) {

        int start =
                index;

        while (
                start > 0
                        &&
                        !lines.get(start)
                                .trim()
                                .startsWith("Scenario:")
        ) {

            start--;
        }

        Pattern billPayClick =
                Pattern.compile(
                        "\\buser clicks \"(?:bill\\s*pay|bill\\s*payment|billpay)\"",
                        Pattern.CASE_INSENSITIVE
                );

        for (
                int i = start;
                i < lines.size();
                i++
        ) {

            if (
                    i > start
                            &&
                            lines.get(i)
                                    .trim()
                                    .startsWith("Scenario:")
            ) {

                break;
            }

            if (
                    billPayClick.matcher(
                                    lines.get(i)
                            )
                            .find()
            ) {

                return true;
            }
        }

        return false;
    }

    private String combinedOutput(
            String first,
            String second
    ) {

        StringBuilder combined =
                new StringBuilder();

        if (
                first != null
                        &&
                        !first.isBlank()
        ) {

            combined.append(first);
        }

        if (
                second != null
                        &&
                        !second.isBlank()
        ) {

            if (
                    combined.length() > 0
            ) {

                combined.append(System.lineSeparator());
            }

            combined.append(second);
        }

        return combined.toString();
    }

    private List<Path> featureFiles(
            Path frameworkRoot
    ) throws IOException {

        Path featureRoot =
                frameworkRoot.resolve(
                        "src/test/resources/features"
                );

        if (
                !Files.exists(featureRoot)
        ) {

            return List.of();
        }

        try (
                Stream<Path> paths =
                        Files.walk(featureRoot)
        ) {

            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName()
                            .toString()
                            .endsWith(".feature"))
                    .sorted()
                    .toList();
        }
    }

    private Optional<String> latestExecutionOutput(
            Path frameworkRoot
    ) {

        Path targetRoot =
                frameworkRoot.resolve("target");

        if (
                !Files.exists(targetRoot)
        ) {

            return Optional.empty();
        }

        try (
                Stream<Path> paths =
                        Files.walk(targetRoot)
        ) {

            List<Path> outputFiles =
                    paths.filter(Files::isRegularFile)
                            .filter(this::isExecutionOutput)
                            .sorted(
                                    Comparator.comparingLong(
                                                    this::lastModified
                                            )
                                            .reversed()
                            )
                            .limit(4)
                            .toList();

            if (
                    outputFiles.isEmpty()
            ) {

                return Optional.empty();
            }

            StringBuilder output =
                    new StringBuilder();

            for (
                    Path outputFile
                    : outputFiles
            ) {

                output.append(System.lineSeparator())
                        .append("--- ")
                        .append(outputFile.getFileName())
                        .append(" ---")
                        .append(System.lineSeparator())
                        .append(readString(outputFile));
            }

            return Optional.of(
                    output.toString()
            );

        } catch (IOException e) {

            return Optional.empty();
        }
    }

    private boolean isExecutionOutput(
            Path path
    ) {

        String fileName =
                path.getFileName()
                        .toString()
                        .toLowerCase();

        return fileName.endsWith(".txt")
                ||
                fileName.equals("cucumber-report.xml")
                ||
                fileName.startsWith("test-")
                        &&
                        fileName.endsWith(".xml");
    }

    String failureSummary(
            String output
    ) {

        if (
                output == null
                        ||
                        output.isBlank()
        ) {

            return "No previous generated-test output was available.";
        }

        Matcher unresolvedElement =
                Pattern.compile(
                                "Unable to resolve element: ([^\\r\\n]+)",
                                Pattern.CASE_INSENSITIVE
                        )
                        .matcher(output);

        if (
                unresolvedElement.find()
        ) {

            return "The last generated test could not resolve element: "
                    + unresolvedElement.group(1)
                    .trim();
        }

        Matcher missingData =
                Pattern.compile(
                                "Missing runtime data for generated tests: ([^\\.\\r\\n]+)",
                                Pattern.CASE_INSENSITIVE
                        )
                        .matcher(output);

        if (
                missingData.find()
        ) {

            return "The last generated test was missing runtime data: "
                    + missingData.group(1)
                    .trim();
        }

        String lowerOutput =
                output.toLowerCase();

        if (
                looksLikeAuthenticationFailure(lowerOutput)
        ) {

            return "The last generated test did not complete authentication. This is usually a runtime test-data or application-state issue, not a locator repair issue. Provide valid credentials or satisfy the login precondition, then rerun the same tag.";
        }

        if (
                lowerOutput.contains("failed to create driver")
                        ||
                        lowerOutput.contains("failed to install browsers")
                        ||
                        lowerOutput.contains("target page, context or browser has been closed")
        ) {

            return "The last generated test failed while starting the Playwright browser. This is a browser-runtime issue, not a feature-file issue. Refresh the generated support files so the run uses installed Chrome and skips Playwright browser downloads, then rerun the same tag.";
        }

        List<String> missingTexts =
                missingExpectedTexts(output);

        if (
                !missingTexts.isEmpty()
        ) {

            return "The last generated test failed because the page did not show expected text: "
                    + String.join(
                    ", ",
                    missingTexts
            )
                    + ".";
        }

        return output.lines()
                .filter(line -> {
                    String lower =
                            line.toLowerCase();

                    return lower.contains("failed")
                            ||
                            lower.contains("failure")
                            ||
                            lower.contains("error");
                })
                .findFirst()
                .orElse("The last generated test output did not contain a recognized failure signature.");
    }

    private List<String> missingExpectedTexts(
            String output
    ) {

        if (
                output == null
                        ||
                        output.isBlank()
        ) {

            return List.of();
        }

        Matcher expectedText =
                Pattern.compile(
                                "Expected page to contain text: ([^\\r\\n<]+)",
                                Pattern.CASE_INSENSITIVE
                        )
                        .matcher(output);

        List<String> missingTexts =
                new ArrayList<>();

        while (
                expectedText.find()
        ) {

            String missingText =
                    expectedText.group(1)
                            .trim();

            if (
                    !missingTexts.contains(missingText)
            ) {

                missingTexts.add(missingText);
            }
        }

        return missingTexts;
    }

    private boolean looksLikeAuthenticationFailure(
            String lowerOutput
    ) {

        if (
                lowerOutput == null
                        ||
                        lowerOutput.isBlank()
        ) {

            return false;
        }

        return lowerOutput.contains("authentication did not complete")
                ||
                lowerOutput.contains("authentication failed")
                ||
                lowerOutput.contains("login failed")
                ||
                lowerOutput.contains("sign in failed")
                ||
                lowerOutput.contains("signin failed")
                ||
                lowerOutput.contains("invalid username")
                ||
                lowerOutput.contains("invalid password")
                ||
                lowerOutput.contains("invalid username/password")
                ||
                lowerOutput.contains("incorrect username")
                ||
                lowerOutput.contains("incorrect password")
                ||
                lowerOutput.contains("bad credentials")
                ||
                lowerOutput.contains("invalid credentials")
                ||
                lowerOutput.contains("could not be verified")
                ||
                lowerOutput.contains("user not found")
                ||
                lowerOutput.contains("account locked")
                ||
                (
                        lowerOutput.contains("unauthorized")
                                &&
                                (
                                        lowerOutput.contains("login")
                                                ||
                                                lowerOutput.contains("password")
                                                ||
                                                lowerOutput.contains("credentials")
                                )
                );
    }

    private long lastModified(
            Path path
    ) {

        try {

            return Files.getLastModifiedTime(path)
                    .toMillis();

        } catch (IOException e) {

            return 0;
        }
    }

    private String readString(
            Path path
    ) {

        try {

            return Files.readString(path);

        } catch (IOException e) {

            return "";
        }
    }

    record FeatureRepair(
            String content,
            List<String> changes
    ) {

        boolean changed() {
            return !changes.isEmpty();
        }
    }

    private record AssertionReplacement(
            String expected,
            String actual
    ) {
    }

    @Getter
    @Builder
    public static class RepairResult {

        private boolean changed;

        private List<String> changedFiles;

        private List<String> changes;

        private String failureSummary;
    }
}
