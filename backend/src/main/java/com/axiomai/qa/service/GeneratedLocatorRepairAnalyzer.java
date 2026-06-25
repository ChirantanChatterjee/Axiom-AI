package com.axiomai.qa.service;

import com.axiomai.ml.FailureClassificationLabel;
import com.axiomai.ml.RepairRecommendationLabel;
import lombok.Builder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeneratedLocatorRepairAnalyzer {

    private static final Pattern ENTER_STEP =
            Pattern.compile(
                    "(?i)user enters \"([^\"]+)\" into \"([^\"]+)\""
            );

    private static final Pattern WEAK_LOCATOR =
            Pattern.compile(
                    "(?i)(page\\.locator\\(\\s*\"(?:input|textarea|select|\\.form-control|\\[role='textbox']|\\[role='combobox'])\"\\s*\\)\\.(?:first|nth)\\(|locator\\.first\\(\\)|:nth-child\\(|nth-of-type\\(|\\binput:visible\\b|\\btextarea:visible\\b)"
            );

    private GeneratedLocatorRepairAnalyzer() {
    }

    public static boolean isLocatorMismatchComplaint(
            String text
    ) {

        String lower =
                lower(text);

        return lower.contains("wrong textbox")
                ||
                lower.contains("wrong text box")
                ||
                lower.contains("wrong input")
                ||
                lower.contains("wrong field")
                ||
                lower.contains("incorrect field")
                ||
                lower.contains("incorrect textbox")
                ||
                lower.contains("typed into")
                ||
                lower.contains("typing into")
                ||
                lower.contains("entered into")
                ||
                lower.contains("locator mismatch")
                ||
                lower.contains("target mismatch");
    }

    public static boolean hasLocatorMismatchEvidence(
            String text
    ) {

        String lower =
                lower(text);

        return isLocatorMismatchComplaint(lower)
                ||
                lower.contains("intendedfieldname")
                ||
                lower.contains("finalresolvedselector")
                ||
                lower.contains("action-evidence.json")
                ||
                lower.contains("weak locator")
                ||
                lower.contains("using first visible input")
                ||
                lower.contains("multiple elements found, using first");
    }

    public static FailureClassificationLabel classifyFailure(
            String text
    ) {

        if (hasLocatorMismatchEvidence(text)) {
            return FailureClassificationLabel.LOCATOR_MISMATCH;
        }

        return FailureClassificationLabel.LOCATOR_FAILURE;
    }

    public static RepairRecommendationLabel recommendedRepair(
            String text
    ) {

        if (hasLocatorMismatchEvidence(text)) {
            return RepairRecommendationLabel.REPAIR_LOCATORS_WITH_RUNTIME_EVIDENCE;
        }

        return RepairRecommendationLabel.UPDATE_LOCATOR;
    }

    public static List<WeakLocatorFinding> weakLocatorFindings(
            String path,
            String source
    ) {

        if (
                source == null
                        ||
                        source.isBlank()
        ) {

            return List.of();
        }

        List<WeakLocatorFinding> findings =
                new ArrayList<>();

        List<String> lines =
                source.lines()
                        .toList();

        for (
                int index = 0;
                index < lines.size();
                index++
        ) {

            String line =
                    lines.get(index);

            Matcher matcher =
                    WEAK_LOCATOR.matcher(line);

            if (
                    matcher.find()
            ) {

                findings.add(
                        WeakLocatorFinding.builder()
                                .path(path)
                                .line(index + 1)
                                .snippet(line.trim())
                                .reason("Broad positional locator can select the wrong editable element when multiple similar controls are visible.")
                                .build()
                );
            }
        }

        return findings;
    }

    public static List<Map<String, String>> enterTargets(
            String feature
    ) {

        if (
                feature == null
                        ||
                        feature.isBlank()
        ) {

            return List.of();
        }

        List<Map<String, String>> entries =
                new ArrayList<>();

        Matcher matcher =
                ENTER_STEP.matcher(feature);

        while (
                matcher.find()
        ) {

            Map<String, String> entry =
                    new LinkedHashMap<>();

            entry.put(
                    "value",
                    matcher.group(1)
            );
            entry.put(
                    "target",
                    matcher.group(2)
            );

            entries.add(entry);
        }

        return entries;
    }

    public static double semanticScore(
            String fieldIntent,
            Map<String, String> metadata
    ) {

        String intent =
                normalizeTokens(fieldIntent);

        if (
                intent.isBlank()
                        ||
                        metadata == null
                        ||
                        metadata.isEmpty()
        ) {

            return 0.0;
        }

        String haystack =
                normalizeTokens(
                        String.join(
                                " ",
                                metadata.values()
                        )
                );

        if (
                haystack.isBlank()
        ) {

            return 0.0;
        }

        List<String> intentTokens =
                significantTokens(intent);

        if (
                intentTokens.isEmpty()
        ) {

            return 0.0;
        }

        double score =
                0.0;

        for (
                String token
                : intentTokens
        ) {

            if (
                    haystack.contains(token)
            ) {

                score += 1.0;
            }
        }

        if (
                haystack.contains(intent)
        ) {

            score += 2.0;
        }

        if (
                intent.contains("multi")
                        &&
                        (
                                haystack.contains("multiple")
                                        ||
                                        haystack.contains("multi")
                        )
        ) {

            score += 2.0;
        }

        if (
                intent.contains("single")
                        &&
                        haystack.contains("single")
        ) {

            score += 2.0;
        }

        if (
                intent.contains("color")
                        &&
                        haystack.contains("auto complete")
        ) {

            score += 0.5;
        }

        return score;
    }

    private static List<String> significantTokens(
            String value
    ) {

        List<String> tokens =
                new ArrayList<>();

        for (
                String token
                : value.split(" ")
        ) {

            if (
                    token.length() >= 3
                            &&
                            !token.equals("the")
                            &&
                            !token.equals("into")
                            &&
                            !token.equals("field")
                            &&
                            !token.equals("input")
                            &&
                            !token.equals("name")
            ) {

                tokens.add(token);
            }
        }

        return tokens;
    }

    private static String normalizeTokens(
            String value
    ) {

        return lower(value)
                .replaceAll(
                        "[^a-z0-9]+",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    private static String lower(
            String value
    ) {

        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT);
    }

    @Builder
    public record WeakLocatorFinding(
            String path,
            int line,
            String snippet,
            String reason
    ) {
    }
}
