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
                .repairGuidance(
                        assertionMismatchRepairGuidance(latestOutput)
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

        String repairedContent =
                content;

        List<String> allChanges =
                new ArrayList<>();

        FeatureRepair sortOptionRepair =
                repairGeneratedSortOptionSelection(
                        repairedContent,
                        latestOutput,
                        userInstruction
                );

        if (
                sortOptionRepair.changed()
        ) {

            repairedContent =
                    sortOptionRepair.content();

            allChanges.addAll(
                    sortOptionRepair.changes()
            );
        }

        FeatureRepair credentialFieldRepair =
                repairCredentialFieldValueMismatch(
                        repairedContent,
                        latestOutput,
                        userInstruction
                );

        if (
                credentialFieldRepair.changed()
        ) {

            repairedContent =
                    credentialFieldRepair.content();

            allChanges.addAll(
                    credentialFieldRepair.changes()
            );
        }

        FeatureRepair actionTargetRepair =
                repairIncorrectGeneratedActionTarget(
                        repairedContent,
                        latestOutput,
                        userInstruction
                );

        if (
                actionTargetRepair.changed()
        ) {

            repairedContent =
                    actionTargetRepair.content();

            allChanges.addAll(
                    actionTargetRepair.changes()
            );
        }

        FeatureRepair passengerDetailsRepair =
                repairTravelPassengerDetailsFlow(
                        repairedContent,
                        latestOutput,
                        userInstruction
                );

        if (
                passengerDetailsRepair.changed()
        ) {

            repairedContent =
                    passengerDetailsRepair.content();

            allChanges.addAll(
                    passengerDetailsRepair.changes()
            );
        }

        FeatureRepair assertionTextRepair =
                repairUserProvidedAssertionText(
                        repairedContent,
                        latestOutput,
                        userInstruction
                );

        if (
                assertionTextRepair.changed()
        ) {

            repairedContent =
                    assertionTextRepair.content();

            allChanges.addAll(
                    assertionTextRepair.changes()
            );
        }

        FeatureRepair genericRepair =
                repairMissingIntermediateAssertions(
                        repairedContent,
                        latestOutput
                );

        repairedContent =
                genericRepair.content();

        allChanges.addAll(
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

    private FeatureRepair repairIncorrectGeneratedActionTarget(
            String content,
            String latestOutput,
            String userInstruction
    ) {

        Optional<String> failedTarget =
                unresolvedElementTarget(latestOutput);

        List<ActionTargetReplacement> replacements =
                actionTargetReplacements(
                        failedTarget,
                        latestOutput,
                        userInstruction
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

            Matcher clickStep =
                    Pattern.compile(
                                    "^(\\s*)(When|And) user clicks \"([^\"]+)\"\\s*$",
                                    Pattern.CASE_INSENSITIVE
                            )
                            .matcher(line);

            if (
                    clickStep.matches()
            ) {

                String currentTarget =
                        clickStep.group(3);

                Optional<ActionTargetReplacement> replacement =
                        replacements.stream()
                                .filter(candidate -> candidate.from()
                                        .equalsIgnoreCase(currentTarget))
                                .findFirst();

                if (
                        replacement.isPresent()
                ) {

                    String nextTarget =
                            normalizeGeneratedActionTarget(
                                    replacement.get()
                                            .to()
                            );

                    line =
                            clickStep.group(1)
                                    + clickStep.group(2)
                                    + " user clicks \""
                                    + sanitizeStepText(nextTarget)
                                    + "\"";

                    changes.add(
                            "Updated generated click target from \""
                                    + currentTarget
                                    + "\" to \""
                                    + nextTarget
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

    private FeatureRepair repairCredentialFieldValueMismatch(
            String content,
            String latestOutput,
            String userInstruction
    ) {

        if (
                content == null
                        ||
                        content.isBlank()
        ) {

            return new FeatureRepair(
                    content == null
                            ? ""
                            : content,
                    List.of()
            );
        }

        String evidence =
                combinedOutput(
                        combinedOutput(
                                content,
                                latestOutput
                        ),
                        userInstruction
                )
                        .toLowerCase();

        boolean mismatchEvidence =
                evidence.contains("username field")
                        ||
                        evidence.contains("password field")
                        ||
                        evidence.contains("filled with password")
                        ||
                        evidence.contains("getting filled with password")
                        ||
                        evidence.contains("value provided for password")
                        ||
                        evidence.contains("value provided for username")
                        ||
                        evidence.contains("wrong value")
                        ||
                        evidence.contains("incorrect value");

        Pattern enterPattern =
                Pattern.compile(
                        "^(\\s*)(When|And) user enters \"([^\"]+)\" into \"([^\"]+)\"\\s*(?:#.*)?$",
                        Pattern.CASE_INSENSITIVE
                );

        List<String> changes =
                new ArrayList<>();

        StringBuilder repaired =
                new StringBuilder();

        for (
                String line
                : content.lines()
                .toList()
        ) {

            Matcher enter =
                    enterPattern.matcher(line);

            if (
                    enter.matches()
            ) {

                String value =
                        enter.group(3);

                String target =
                        enter.group(4);

                if (
                        looksLikeUsernameTarget(target)
                                &&
                                looksLikePasswordValue(value)
                                &&
                                (
                                        mismatchEvidence
                                                ||
                                                isRuntimePlaceholder(value)
                                )
                ) {

                    line =
                            enterStep(
                                    enter.group(1),
                                    enter.group(2),
                                    "${username}",
                                    target
                            );

                    changes.add(
                            "Changed username field input from password value to ${username}."
                    );

                } else if (
                        looksLikePasswordTarget(target)
                                &&
                                looksLikeUsernameValue(value)
                                &&
                                (
                                        mismatchEvidence
                                                ||
                                                isRuntimePlaceholder(value)
                                )
                ) {

                    line =
                            enterStep(
                                    enter.group(1),
                                    enter.group(2),
                                    "${password}",
                                    target
                            );

                    changes.add(
                            "Changed password field input from username value to ${password}."
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

    private String enterStep(
            String indent,
            String keyword,
            String value,
            String target
    ) {

        return indent
                + keyword
                + " user enters \""
                + sanitizeStepText(value)
                + "\" into \""
                + sanitizeStepText(target)
                + "\"";
    }

    private boolean looksLikeUsernameTarget(
            String target
    ) {

        if (
                target == null
        ) {

            return false;
        }

        String lower =
                target.toLowerCase();

        return lower.contains("username")
                ||
                lower.equals("user")
                ||
                lower.contains("login id")
                ||
                lower.contains("email");
    }

    private boolean looksLikePasswordTarget(
            String target
    ) {

        return target != null
                &&
                target.toLowerCase()
                        .contains("password");
    }

    private boolean looksLikePasswordValue(
            String value
    ) {

        if (
                value == null
        ) {

            return false;
        }

        String lower =
                value.toLowerCase();

        return lower.equals("${password}")
                ||
                lower.equals("${pass}")
                ||
                lower.contains("password");
    }

    private boolean looksLikeUsernameValue(
            String value
    ) {

        if (
                value == null
        ) {

            return false;
        }

        String lower =
                value.toLowerCase();

        return lower.equals("${username}")
                ||
                lower.equals("${user}")
                ||
                lower.contains("username");
    }

    private boolean isRuntimePlaceholder(
            String value
    ) {

        return value != null
                &&
                value.matches("\\$\\{[A-Za-z][A-Za-z0-9_]*}");
    }

    private FeatureRepair repairGeneratedSortOptionSelection(
            String content,
            String latestOutput,
            String userInstruction
    ) {

        if (
                !containsGeneratedSortEvidence(
                        combinedOutput(
                                combinedOutput(
                                        content,
                                        latestOutput
                                ),
                                userInstruction
                        )
                )
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

        Pattern clickPattern =
                Pattern.compile(
                        "^(\\s*)(When|And) user clicks \"([^\"]+)\"\\s*(?:#.*)?$",
                        Pattern.CASE_INSENSITIVE
                );

        Pattern selectPattern =
                Pattern.compile(
                        "^(\\s*)(When|And) user selects \"([^\"]+)\" from \"([^\"]+)\"\\s*(?:#.*)?$",
                        Pattern.CASE_INSENSITIVE
                );

        Pattern enterPattern =
                Pattern.compile(
                        "^(\\s*)(When|And) user enters \"([^\"]+)\" into \"([^\"]+)\"\\s*(?:#.*)?$",
                        Pattern.CASE_INSENSITIVE
                );

        for (
                String line
                : lines
        ) {

            Matcher click =
                    clickPattern.matcher(line);

            Matcher select =
                    selectPattern.matcher(line);

            Matcher enter =
                    enterPattern.matcher(line);

            if (
                    click.matches()
            ) {

                Optional<String> sortValue =
                        sortOptionValue(
                                click.group(3)
                        );

                if (
                        sortValue.isPresent()
                ) {

                    line =
                            sortEntryStep(
                                    click.group(1),
                                    click.group(2),
                                    sortValue.get()
                            );

                    changes.add(
                            "Changed generated sort action from click \""
                                    + click.group(3)
                                    + "\" to select value \""
                                    + sortValue.get()
                                    + "\" in the sort dropdown."
                    );
                }

            } else if (
                    select.matches()
            ) {

                Optional<String> sortValue =
                        sortOptionValue(
                                select.group(3)
                        );

                if (
                        sortValue.isPresent()
                                &&
                                looksLikeSortControl(
                                        select.group(4)
                                )
                ) {

                    line =
                            sortEntryStep(
                                    select.group(1),
                                    select.group(2),
                                    sortValue.get()
                            );

                    changes.add(
                            "Changed generated sort dropdown step for \""
                                    + select.group(3)
                                    + "\" to supported value \""
                                    + sortValue.get()
                                    + "\"."
                    );
                }

            } else if (
                    enter.matches()
                            &&
                            looksLikeSortControl(
                                    enter.group(4)
                            )
            ) {

                Optional<String> sortValue =
                        sortOptionValue(
                                enter.group(3)
                        );

                if (
                        sortValue.isPresent()
                                &&
                                !sortValue.get()
                                        .equals(enter.group(3))
                ) {

                    line =
                            sortEntryStep(
                                    enter.group(1),
                                    enter.group(2),
                                    sortValue.get()
                            );

                    changes.add(
                            "Changed generated sort option \""
                                    + enter.group(3)
                                    + "\" to supported value \""
                                    + sortValue.get()
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

    private boolean containsGeneratedSortEvidence(
            String text
    ) {

        if (
                text == null
                        ||
                        text.isBlank()
        ) {

            return false;
        }

        String lower =
                normalizeSortOptionText(text);

        return lower.contains("product list should be sorted")
                ||
                lower.contains("@product_sorting")
                ||
                lower.contains("product_sorting")
                ||
                lower.contains("inventory page")
                ||
                lower.contains("saucedemo")
                ||
                lower.contains("unable to resolve element: a-z")
                ||
                lower.contains("unable to resolve element: z-a")
                ||
                lower.contains("unable to resolve element: price low-high")
                ||
                lower.contains("unable to resolve element: price high-low")
                ||
                sortOptionValue(text)
                        .isPresent();
    }

    private String sortEntryStep(
            String indent,
            String keyword,
            String sortValue
    ) {

        return indent
                + keyword
                + " user enters \""
                + sortValue
                + "\" into \"sort\"";
    }

    private boolean looksLikeSortControl(
            String target
    ) {

        if (
                target == null
        ) {

            return false;
        }

        String lower =
                target.trim()
                        .toLowerCase();

        return lower.equals("sort")
                ||
                lower.contains("sort")
                ||
                lower.contains("order")
                ||
                lower.contains("filter")
                ||
                lower.contains("dropdown")
                ||
                lower.contains("drop-down")
                ||
                lower.contains("select");
    }

    private Optional<String> sortOptionValue(
            String label
    ) {

        String normalized =
                normalizeSortOptionText(label);

        if (
                normalized.isBlank()
        ) {

            return Optional.empty();
        }

        if (
                normalized.equals("az")
                        ||
                        normalized.equals("a-z")
                        ||
                        normalized.equals("a to z")
                        ||
                        normalized.equals("name a-z")
                        ||
                        normalized.equals("name a to z")
                        ||
                        (
                                normalized.contains("name")
                                        &&
                                        (
                                                normalized.contains("a-z")
                                                        ||
                                                        normalized.contains("a to z")
                                        )
                        )
        ) {

            return Optional.of("az");
        }

        if (
                normalized.equals("za")
                        ||
                        normalized.equals("z-a")
                        ||
                        normalized.equals("z to a")
                        ||
                        normalized.equals("name z-a")
                        ||
                        normalized.equals("name z to a")
                        ||
                        (
                                normalized.contains("name")
                                        &&
                                        (
                                                normalized.contains("z-a")
                                                        ||
                                                        normalized.contains("z to a")
                                        )
                        )
        ) {

            return Optional.of("za");
        }

        if (
                normalized.equals("lohi")
                        ||
                        normalized.equals("low-high")
                        ||
                        normalized.equals("low to high")
                        ||
                        normalized.equals("price low-high")
                        ||
                        normalized.equals("price low to high")
                        ||
                        (
                                normalized.contains("price")
                                        &&
                                        (
                                                normalized.contains("low-high")
                                                        ||
                                                        normalized.contains("low to high")
                                        )
                        )
        ) {

            return Optional.of("lohi");
        }

        if (
                normalized.equals("hilo")
                        ||
                        normalized.equals("high-low")
                        ||
                        normalized.equals("high to low")
                        ||
                        normalized.equals("price high-low")
                        ||
                        normalized.equals("price high to low")
                        ||
                        (
                                normalized.contains("price")
                                        &&
                                        (
                                                normalized.contains("high-low")
                                                        ||
                                                        normalized.contains("high to low")
                                        )
                        )
        ) {

            return Optional.of("hilo");
        }

        return Optional.empty();
    }

    private String normalizeSortOptionText(
            String text
    ) {

        if (
                text == null
        ) {

            return "";
        }

        return text.trim()
                .toLowerCase()
                .replaceAll(
                        "[\\u2010-\\u2015]",
                        "-"
                )
                .replaceAll(
                        "[()_]+",
                        " "
                )
                .replaceAll(
                        "\\s*-\\s*",
                        "-"
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    private List<ActionTargetReplacement> actionTargetReplacements(
            Optional<String> failedTarget,
            String latestOutput,
            String userInstruction
    ) {

        List<ActionTargetReplacement> replacements =
                new ArrayList<>();

        replacements.addAll(
                explicitActionTargetReplacements(
                        userInstruction
                )
        );

        if (
                failedTarget.isPresent()
                        &&
                        replacements.stream()
                                .noneMatch(replacement -> replacement.from()
                                        .equalsIgnoreCase(
                                                failedTarget.get()
                                        ))
        ) {

            explicitActualActionTarget(userInstruction)
                    .filter(observed -> shouldReplaceGeneratedActionTarget(
                            failedTarget.get(),
                            observed
                    ))
                    .ifPresent(observed -> replacements.add(
                            new ActionTargetReplacement(
                                    failedTarget.get(),
                                    observed
                            )
                    ));
        }

        if (
                failedTarget.isPresent()
                        &&
                        replacements.stream()
                                .noneMatch(replacement -> replacement.from()
                                        .equalsIgnoreCase(
                                                failedTarget.get()
                                        ))
        ) {

            observedActionTarget(
                    combinedOutput(
                            latestOutput,
                            userInstruction
                    )
            )
                    .filter(observed -> shouldReplaceGeneratedActionTarget(
                            failedTarget.get(),
                            observed
                    ))
                    .ifPresent(observed -> replacements.add(
                            new ActionTargetReplacement(
                                    failedTarget.get(),
                                    observed
                            )
                    ));
        }

        if (
                failedTarget.isEmpty()
        ) {

            return replacements.stream()
                    .filter(replacement -> !replacement.from()
                            .isBlank())
                    .filter(replacement -> !replacement.to()
                            .isBlank())
                    .distinct()
                    .toList();
        }

        return replacements.stream()
                .filter(replacement -> replacement.from()
                        .equalsIgnoreCase(
                                failedTarget.get()
                        ))
                .filter(replacement -> !replacement.to()
                        .isBlank())
                .distinct()
                .toList();
    }

    private List<ActionTargetReplacement> explicitActionTargetReplacements(
            String userInstruction
    ) {

        if (
                userInstruction == null
                        ||
                        userInstruction.isBlank()
        ) {

            return List.of();
        }

        List<ActionTargetReplacement> replacements =
                new ArrayList<>();

        List<Pattern> patterns =
                List.of(
                        Pattern.compile(
                                "(?i)(?:replace|change|update|rewrite)\\s+\"([^\"]+)\"\\s+(?:to|with|as)\\s+\"([^\"]+)\""
                        ),
                        Pattern.compile(
                                "(?i)(?:no|not|without|missing|there\\s+(?:is|are)\\s+no)[^\\r\\n\"]{0,180}\"([^\"]+)\"[^\\r\\n\"]{0,180}(?:just\\s+says|says|shows|actual(?:ly)?\\s+(?:says|is)|should\\s+be|is\\s+(?:called|label(?:led|ed)?))[^\\r\\n\"]{0,80}\"([^\"]+)\""
                        ),
                        Pattern.compile(
                                "(?i)\"([^\"]+)\"[^\\r\\n\"]{0,180}(?:just\\s+says|says|shows|actual(?:ly)?\\s+(?:says|is)|should\\s+be|is\\s+(?:called|label(?:led|ed)?))[^\\r\\n\"]{0,80}\"([^\"]+)\""
                        )
                );

        for (
                Pattern pattern
                : patterns
        ) {

            Matcher matcher =
                    pattern.matcher(userInstruction);

            while (
                    matcher.find()
            ) {

                String from =
                        matcher.group(1)
                                .trim();

                String to =
                        matcher.group(2)
                                .trim();

                if (
                        !from.equalsIgnoreCase(to)
                ) {

                    replacements.add(
                            new ActionTargetReplacement(
                                    from,
                                    to
                            )
                    );
                }
            }
        }

        return replacements.stream()
                .filter(replacement -> !replacement.from()
                        .isBlank())
                .filter(replacement -> !replacement.to()
                        .isBlank())
                .distinct()
                .toList();
    }

    private Optional<String> explicitActualActionTarget(
            String userInstruction
    ) {

        if (
                userInstruction == null
                        ||
                        userInstruction.isBlank()
        ) {

            return Optional.empty();
        }

        List<Pattern> patterns =
                List.of(
                        Pattern.compile(
                                "(?i)(?:actual|real|only|correct)[^\"\\r\\n]{0,80}(?:button|control|submit|action|label)[^\"\\r\\n]{0,80}(?:says|is|should\\s+be|label(?:led|ed)?|called)\\s*\"([^\"]+)\""
                        ),
                        Pattern.compile(
                                "(?i)(?:button|control|submit|action|label)[^\"\\r\\n]{0,80}(?:actually\\s+)?(?:says|is|should\\s+be|label(?:led|ed)?|called)\\s*\"([^\"]+)\""
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
                        )
                        .filter(target -> !target.isBlank());
            }
        }

        return Optional.empty();
    }

    private Optional<String> unresolvedElementTarget(
            String output
    ) {

        if (
                output == null
                        ||
                        output.isBlank()
        ) {

            return Optional.empty();
        }

        Matcher matcher =
                Pattern.compile(
                                "Unable to resolve element: ([^\\r\\n]+)",
                                Pattern.CASE_INSENSITIVE
                        )
                        .matcher(output);

        if (
                !matcher.find()
        ) {

            return Optional.empty();
        }

        String target =
                matcher.group(1)
                        .trim();

        return target.isBlank()
                ? Optional.empty()
                : Optional.of(target);
    }

    private Optional<String> observedActionTarget(
            String output
    ) {

        if (
                output == null
                        ||
                        output.isBlank()
        ) {

            return Optional.empty();
        }

        Pattern elementLine =
                Pattern.compile(
                        "ELEMENT\\s*->\\s*TAG=([^|]+)\\|\\s*TEXT=([^|]*)\\|[^\\r\\n]*?ROLE=([^|\\r\\n]+)",
                        Pattern.CASE_INSENSITIVE
                );

        for (
                String line
                : output.lines()
                .toList()
        ) {

            Matcher matcher =
                    elementLine.matcher(line);

            if (
                    !matcher.find()
            ) {

                continue;
            }

            String tag =
                    matcher.group(1)
                            .trim()
                            .toLowerCase();

            String text =
                    matcher.group(2)
                            .trim();

            String role =
                    matcher.group(3)
                            .trim()
                            .toUpperCase();

            if (
                    text.isBlank()
                            ||
                            role.equals("LOGIN_BUTTON")
            ) {

                continue;
            }

            if (
                    role.equals("NEXT_BUTTON")
                            ||
                            role.equals("PRIMARY_ACTION_BUTTON")
                            ||
                            role.equals("SEARCH_BUTTON")
                            ||
                            role.equals("SUBMIT_BUTTON")
                            ||
                            tag.equals("button")
                            ||
                            tag.equals("input")
                            ||
                            tag.equals("a")
            ) {

                return Optional.of(text);
            }
        }

        return Optional.empty();
    }

    private boolean shouldReplaceGeneratedActionTarget(
            String failedTarget,
            String observedTarget
    ) {

        if (
                failedTarget == null
                        ||
                        observedTarget == null
        ) {

            return false;
        }

        String failed =
                failedTarget.trim()
                        .toLowerCase();

        String observed =
                observedTarget.trim()
                        .toLowerCase();

        if (
                failed.isBlank()
                        ||
                        observed.isBlank()
                        ||
                        failed.equals(observed)
        ) {

            return false;
        }

        return looksLikeGeneratedActionPhrase(failed)
                &&
                looksLikeObservedActionLabel(observed);
    }

    private boolean looksLikeGeneratedActionPhrase(
            String target
    ) {

        return target.contains("search")
                ||
                target.contains("submit")
                ||
                target.contains("continue")
                ||
                target.contains("proceed")
                ||
                target.contains("next")
                ||
                target.contains("find")
                ||
                target.contains("save")
                ||
                target.contains("send")
                ||
                target.contains("pay")
                ||
                target.contains("book")
                ||
                target.contains("create")
                ||
                target.contains("finish");
    }

    private boolean looksLikeObservedActionLabel(
            String target
    ) {

        return target.equals("continue")
                ||
                target.equals("next")
                ||
                target.equals("submit")
                ||
                target.equals("search")
                ||
                target.equals("find")
                ||
                target.equals("save")
                ||
                target.equals("send")
                ||
                target.equals("pay")
                ||
                target.equals("book")
                ||
                target.equals("finish")
                ||
                target.equals("checkout")
                ||
                target.equals("confirm")
                ||
                target.equals("apply")
                ||
                target.equals("go")
                ||
                target.length() <= 32;
    }

    private String normalizeGeneratedActionTarget(
            String target
    ) {

        if (
                target == null
        ) {

            return "";
        }

        String normalized =
                target.trim()
                        .replaceAll(
                                "\\s+",
                                " "
                        );

        String lower =
                normalized.toLowerCase();

        if (
                lower.equals("continue")
                        ||
                        lower.equals("next")
                        ||
                        lower.equals("submit")
                        ||
                        lower.equals("search")
                        ||
                        lower.equals("find")
                        ||
                        lower.equals("save")
                        ||
                        lower.equals("send")
                        ||
                        lower.equals("pay")
                        ||
                        lower.equals("book")
                        ||
                        lower.equals("finish")
                        ||
                        lower.equals("checkout")
                        ||
                        lower.equals("confirm")
                        ||
                        lower.equals("apply")
                        ||
                        lower.equals("go")
        ) {

            return lower;
        }

        return normalized;
    }

    private FeatureRepair repairTravelPassengerDetailsFlow(
            String content,
            String latestOutput,
            String userInstruction
    ) {

        if (
                !shouldRepairTravelPassengerDetailsFlow(
                        content,
                        latestOutput,
                        userInstruction
                )
        ) {

            return new FeatureRepair(
                    content,
                    List.of()
            );
        }

        List<String> lines =
                content.lines()
                        .toList();

        PassengerNames passengerNames =
                passengerNames(userInstruction);

        List<String> changes =
                new ArrayList<>();

        StringBuilder repaired =
                new StringBuilder();

        boolean passengerDetailsInsertedInScenario =
                false;

        boolean passengerNameEntrySeenInScenario =
                false;

        for (
                int i = 0;
                i < lines.size();
                i++
        ) {

            String line =
                    lines.get(i);

            if (
                    isScenarioStart(line)
            ) {

                passengerDetailsInsertedInScenario =
                        false;

                passengerNameEntrySeenInScenario =
                        false;
            }

            if (
                    shouldRepairTravelScenario(
                            lines,
                            i,
                            userInstruction
                    )
                            &&
                            isBadTravelDepartureAssertion(line)
            ) {

                changes.add(
                        "Removed invalid select-flight assertion for \"Select your departure flight\" because the next page asks for passenger details."
                );

                continue;
            }

            if (
                    shouldRepairTravelScenario(
                            lines,
                            i,
                            userInstruction
                    )
                            &&
                            isGeneratedFlightSelectionClick(line)
            ) {

                changes.add(
                        "Removed generated click for nonexistent flight selector \""
                                + clickedTarget(line)
                                + "\"."
                );

                continue;
            }

            PassengerFieldEntry passengerFieldEntry =
                    passengerFieldEntry(line);

            if (
                    passengerFieldEntry != null
                            &&
                            shouldRepairTravelScenario(
                                    lines,
                                    i,
                                    userInstruction
                            )
            ) {

                String nextValue =
                        passengerFieldEntry.field()
                                .equalsIgnoreCase("First Name")
                                ? passengerNames.firstName()
                                : passengerNames.lastName();

                boolean shouldReplaceValue =
                        passengerFieldEntry.field()
                                .equalsIgnoreCase("First Name")
                                ? passengerNames.firstNameProvided()
                                : passengerNames.lastNameProvided();

                if (
                        shouldReplaceValue
                                &&
                                !passengerFieldEntry.value()
                                        .equals(nextValue)
                ) {

                    line =
                            passengerFieldEntry.indent()
                                    + passengerFieldEntry.keyword()
                                    + " user enters \""
                                    + sanitizeStepText(nextValue)
                                    + "\" into \""
                                    + passengerFieldEntry.field()
                                    + "\"";

                    changes.add(
                            "Updated passenger "
                                    + passengerFieldEntry.field()
                                    + " value to \""
                                    + nextValue
                                    + "\"."
                    );
                }
            }

            if (
                    isContinueClick(line)
                            &&
                            shouldRepairTravelScenario(
                                    lines,
                                    i,
                                    userInstruction
                            )
                            &&
                            (
                                    passengerDetailsInsertedInScenario
                                            ||
                                            passengerNameEntrySeenInScenario
                                            ||
                                            scenarioHasPassengerNameEntryBefore(
                                                    lines,
                                                    i,
                                                    "First Name"
                                            )
                                            ||
                                            scenarioHasPassengerNameEntryBefore(
                                                    lines,
                                                    i,
                                                    "Last Name"
                                            )
                            )
            ) {

                line =
                        stepIndent(line)
                                + clickKeyword(line)
                                + " user clicks \"next\"";

                changes.add(
                        "Changed passenger-details submit action from \"continue\" to \"next\"."
                );
            }

            repaired.append(line)
                    .append(System.lineSeparator());

            if (
                    !passengerDetailsInsertedInScenario
                            &&
                            !passengerNameEntrySeenInScenario
                            &&
                    isTravelFlightSearchContinue(
                            lines,
                            i,
                            userInstruction
                    )
                            &&
                            shouldInsertPassengerDetailsSteps(
                                    lines,
                                    i,
                                    userInstruction,
                                    passengerNames
                            )
            ) {

                appendPassengerDetailsBlock(
                        repaired,
                        stepIndent(line),
                        passengerNames,
                        !hasLaterPassengerSubmitBeforeScenario(
                                lines,
                                i
                        )
                );

                changes.add(
                        "Inserted passenger First Name and Last Name steps after the flight-search Continue action."
                );

                passengerDetailsInsertedInScenario =
                        true;

                passengerNameEntrySeenInScenario =
                        true;
            }

            if (
                    passengerFieldEntry != null
                            &&
                            passengerFieldEntry.field()
                                    .equalsIgnoreCase("Last Name")
                            &&
                            shouldRepairTravelScenario(
                                    lines,
                                    i,
                                    userInstruction
                            )
                            &&
                            !hasLaterPassengerSubmitBeforeScenario(
                                    lines,
                                    i
                            )
            ) {

                repaired.append(passengerFieldEntry.indent())
                        .append("And user clicks \"next\"")
                        .append(System.lineSeparator());

                changes.add(
                        "Added Next after passenger details."
                );
            }

            if (
                    passengerFieldEntry != null
            ) {

                passengerNameEntrySeenInScenario =
                        true;
            }
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

    private boolean shouldRepairTravelPassengerDetailsFlow(
            String content,
            String latestOutput,
            String userInstruction
    ) {

        String lowerContent =
                content == null
                        ? ""
                        : content.toLowerCase();

        if (
                !looksLikeTravelSelectFlightFeature(lowerContent)
        ) {

            return false;
        }

        String evidence =
                combinedOutput(
                        combinedOutput(
                                content,
                                latestOutput
                        ),
                        userInstruction
                )
                        .toLowerCase();

        return evidence.contains("outbound flight")
                ||
                evidence.contains("return flight")
                ||
                evidence.contains("select your departure flight")
                ||
                mentionsPassengerDetails(evidence);
    }

    private boolean looksLikeTravelSelectFlightFeature(
            String lowerContent
    ) {

        return lowerContent.contains("select_flight")
                ||
                lowerContent.contains("select flight")
                ||
                lowerContent.contains("return journey")
                ||
                lowerContent.contains("travel.agileway.net");
    }

    private boolean mentionsPassengerDetails(
            String lowerText
    ) {

        return lowerText != null
                &&
                lowerText.contains("first name")
                &&
                lowerText.contains("last name");
    }

    private boolean shouldRepairTravelScenario(
            List<String> lines,
            int index,
            String userInstruction
    ) {

        if (
                !isTravelScenario(
                        lines,
                        index
                )
        ) {

            return false;
        }

        return scenarioContainsGeneratedFlightSelectionClick(
                lines,
                index
        )
                ||
                scenarioContainsBadTravelDepartureAssertion(
                        lines,
                        index
                )
                ||
                scenarioContains(
                        lines,
                        index,
                        "First Name"
                )
                ||
                scenarioContains(
                        lines,
                        index,
                        "Last Name"
                )
                ||
                (
                        mentionsPassengerDetails(
                                userInstruction == null
                                        ? ""
                                        : userInstruction.toLowerCase()
                        )
                                &&
                                isPositiveTravelScenario(
                                        lines,
                                        index
                                )
                );
    }

    private boolean scenarioContainsGeneratedFlightSelectionClick(
            List<String> lines,
            int index
    ) {

        int start =
                scenarioStart(
                        lines,
                        index
                );

        for (
                int i = start;
                i < lines.size();
                i++
        ) {

            if (
                    i > start
                            &&
                            isScenarioStart(lines.get(i))
            ) {

                break;
            }

            if (
                    isGeneratedFlightSelectionClick(lines.get(i))
            ) {

                return true;
            }
        }

        return false;
    }

    private boolean scenarioContainsBadTravelDepartureAssertion(
            List<String> lines,
            int index
    ) {

        int start =
                scenarioStart(
                        lines,
                        index
                );

        for (
                int i = start;
                i < lines.size();
                i++
        ) {

            if (
                    i > start
                            &&
                            isScenarioStart(lines.get(i))
            ) {

                break;
            }

            if (
                    isBadTravelDepartureAssertion(lines.get(i))
            ) {

                return true;
            }
        }

        return false;
    }

    private boolean isTravelScenario(
            List<String> lines,
            int index
    ) {

        return scenarioContains(
                lines,
                index,
                "select_flight"
        )
                ||
                scenarioContains(
                        lines,
                        index,
                        "select flight"
                )
                ||
                scenarioContains(
                        lines,
                        index,
                        "return journey"
                )
                ||
                scenarioContains(
                        lines,
                        index,
                        "travel.agileway.net"
                );
    }

    private boolean isPositiveTravelScenario(
            List<String> lines,
            int index
    ) {

        return !scenarioContains(
                lines,
                index,
                "@negative"
        )
                &&
                (
                        scenarioContains(
                                lines,
                                index,
                                "@positive"
                        )
                                ||
                                scenarioContains(
                                        lines,
                                        index,
                                        "successfully"
                                )
                                ||
                                scenarioContains(
                                        lines,
                                        index,
                                        "valid"
                                )
                );
    }

    private boolean isTravelFlightSearchContinue(
            List<String> lines,
            int index,
            String userInstruction
    ) {

        if (
                !isContinueClick(
                        lines.get(index)
                )
        ) {

            return false;
        }

        if (
                !shouldRepairTravelScenario(
                        lines,
                        index,
                        userInstruction
                )
        ) {

            return false;
        }

        return !scenarioHasPassengerNameEntryBefore(
                lines,
                index,
                "First Name"
        )
                &&
                !scenarioHasPassengerNameEntryBefore(
                        lines,
                        index,
                        "Last Name"
                );
    }

    private boolean shouldInsertPassengerDetailsSteps(
            List<String> lines,
            int index,
            String userInstruction,
            PassengerNames passengerNames
    ) {

        if (
                scenarioHasPassengerNameEntries(
                        lines,
                        index
                )
        ) {

            return false;
        }

        return scenarioContainsGeneratedFlightSelectionClick(
                lines,
                index
        )
                ||
                scenarioContainsBadTravelDepartureAssertion(
                        lines,
                        index
                )
                ||
                passengerNames.firstNameProvided()
                ||
                passengerNames.lastNameProvided()
                ||
                (
                        mentionsPassengerDetails(
                                userInstruction == null
                                        ? ""
                                        : userInstruction.toLowerCase()
                        )
                                &&
                                isPositiveTravelScenario(
                                        lines,
                                        index
                                )
                );
    }

    private boolean isBadTravelDepartureAssertion(
            String line
    ) {

        Matcher assertion =
                Pattern.compile(
                                "^(\\s*)(Then|And) user should see \"Select your departure flight\"\\s*$",
                                Pattern.CASE_INSENSITIVE
                        )
                        .matcher(line);

        return assertion.matches();
    }

    private boolean isGeneratedFlightSelectionClick(
            String line
    ) {

        Matcher click =
                Pattern.compile(
                                "^(\\s*)(When|And) user clicks \"(outbound flight|return flight)\"\\s*$",
                                Pattern.CASE_INSENSITIVE
                        )
                        .matcher(line);

        return click.matches();
    }

    private String clickedTarget(
            String line
    ) {

        Matcher click =
                Pattern.compile(
                                "^(\\s*)(When|And) user clicks \"([^\"]+)\"\\s*$",
                                Pattern.CASE_INSENSITIVE
                        )
                        .matcher(line);

        if (
                click.matches()
        ) {

            return click.group(3);
        }

        return "";
    }

    private boolean isContinueClick(
            String line
    ) {

        Matcher click =
                Pattern.compile(
                                "^(\\s*)(When|And) user clicks \"continue\"\\s*$",
                                Pattern.CASE_INSENSITIVE
                        )
                        .matcher(line);

        return click.matches();
    }

    private boolean isNextClick(
            String line
    ) {

        Matcher click =
                Pattern.compile(
                                "^(\\s*)(When|And) user clicks \"next\"\\s*$",
                                Pattern.CASE_INSENSITIVE
                        )
                        .matcher(line);

        return click.matches();
    }

    private boolean scenarioHasPassengerNameEntries(
            List<String> lines,
            int index
    ) {

        return scenarioHasPassengerNameEntry(
                lines,
                index,
                "First Name"
        )
                &&
                scenarioHasPassengerNameEntry(
                        lines,
                        index,
                        "Last Name"
                );
    }

    private boolean scenarioHasPassengerNameEntry(
            List<String> lines,
            int index,
            String field
    ) {

        int start =
                scenarioStart(
                        lines,
                        index
                );

        for (
                int i = start;
                i < lines.size();
                i++
        ) {

            if (
                    i > start
                            &&
                            isScenarioStart(lines.get(i))
            ) {

                break;
            }

            PassengerFieldEntry entry =
                    passengerFieldEntry(lines.get(i));

            if (
                    entry != null
                            &&
                            entry.field()
                                    .equalsIgnoreCase(field)
            ) {

                return true;
            }
        }

        return false;
    }

    private boolean scenarioHasPassengerNameEntryBefore(
            List<String> lines,
            int index,
            String field
    ) {

        int start =
                scenarioStart(
                        lines,
                        index
                );

        for (
                int i = start;
                i < index;
                i++
        ) {

            PassengerFieldEntry entry =
                    passengerFieldEntry(lines.get(i));

            if (
                    entry != null
                            &&
                            entry.field()
                                    .equalsIgnoreCase(field)
            ) {

                return true;
            }
        }

        return false;
    }

    private boolean hasLaterPassengerSubmitBeforeScenario(
            List<String> lines,
            int index
    ) {

        for (
                int i = index + 1;
                i < lines.size();
                i++
        ) {

            if (
                    isScenarioStart(lines.get(i))
            ) {

                return false;
            }

            if (
                    isContinueClick(lines.get(i))
                            ||
                            isNextClick(lines.get(i))
            ) {

                return true;
            }
        }

        return false;
    }

    private void appendPassengerDetailsBlock(
            StringBuilder repaired,
            String indent,
            PassengerNames passengerNames,
            boolean includeSubmit
    ) {

        repaired.append(indent)
                .append("Then user should see \"First Name\"")
                .append(System.lineSeparator());
        repaired.append(indent)
                .append("And user should see \"Last Name\"")
                .append(System.lineSeparator());
        repaired.append(indent)
                .append("And user enters \"")
                .append(sanitizeStepText(passengerNames.firstName()))
                .append("\" into \"First Name\"")
                .append(System.lineSeparator());
        repaired.append(indent)
                .append("And user enters \"")
                .append(sanitizeStepText(passengerNames.lastName()))
                .append("\" into \"Last Name\"")
                .append(System.lineSeparator());

        if (
                includeSubmit
        ) {

            repaired.append(indent)
                    .append("And user clicks \"next\"")
                    .append(System.lineSeparator());
        }
    }

    private String clickKeyword(
            String line
    ) {

        Matcher click =
                Pattern.compile(
                                "^(\\s*)(When|And) user clicks \"[^\"]+\"\\s*$",
                                Pattern.CASE_INSENSITIVE
                        )
                        .matcher(line);

        if (
                click.matches()
        ) {

            return click.group(2);
        }

        return "And";
    }

    private PassengerFieldEntry passengerFieldEntry(
            String line
    ) {

        Matcher entry =
                Pattern.compile(
                                "^(\\s*)(When|And) user enters \"([^\"]+)\" into \"(First Name|Last Name)\"\\s*$",
                                Pattern.CASE_INSENSITIVE
                        )
                        .matcher(line);

        if (
                !entry.matches()
        ) {

            return null;
        }

        return new PassengerFieldEntry(
                entry.group(1),
                entry.group(2),
                entry.group(3),
                canonicalPassengerField(entry.group(4))
        );
    }

    private String canonicalPassengerField(
            String field
    ) {

        return field.equalsIgnoreCase("First Name")
                ? "First Name"
                : "Last Name";
    }

    private PassengerNames passengerNames(
            String userInstruction
    ) {

        Optional<String> firstName =
                passengerName(
                        userInstruction,
                        "first"
                );

        Optional<String> lastName =
                passengerName(
                        userInstruction,
                        "last"
                );

        return new PassengerNames(
                firstName.orElse("${firstName}"),
                lastName.orElse("${lastName}"),
                firstName.isPresent(),
                lastName.isPresent()
        );
    }

    private Optional<String> passengerName(
            String userInstruction,
            String namePart
    ) {

        if (
                userInstruction == null
                        ||
                        userInstruction.isBlank()
        ) {

            return Optional.empty();
        }

        Matcher matcher =
                Pattern.compile(
                                "(?i)\\b"
                                        + Pattern.quote(namePart)
                                        + "\\s+name\\s*(?:=|:|is)\\s*([A-Za-z][A-Za-z'\\-]*)"
                        )
                        .matcher(userInstruction);

        if (
                matcher.find()
        ) {

            return Optional.of(
                    matcher.group(1)
                            .trim()
            );
        }

        return Optional.empty();
    }

    private String stepIndent(
            String line
    ) {

        Matcher matcher =
                Pattern.compile("^(\\s*)")
                        .matcher(line);

        if (
                matcher.find()
        ) {

            return matcher.group(1);
        }

        return "";
    }

    private int scenarioStart(
            List<String> lines,
            int index
    ) {

        int start =
                index;

        while (
                start > 0
                        &&
                        !isScenarioStart(lines.get(start))
        ) {

            start--;
        }

        return start;
    }

    private boolean isScenarioStart(
            String line
    ) {

        String trimmed =
                line.trim();

        return trimmed.startsWith("Scenario:")
                ||
                trimmed.startsWith("Scenario Outline:");
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

        Optional<String> scenarioText =
                scenarioAssertionText(userInstruction);

        if (
                scenarioText.isEmpty()
        ) {

            return List.of();
        }

        List<MissingAssertionFailure> matchingFailures =
                missingExpectedTextFailures(latestOutput)
                        .stream()
                        .filter(failure -> failure.scenario()
                                .map(scenario -> scenario.toLowerCase()
                                        .contains(
                                                scenarioText.get()
                                                        .toLowerCase()
                                        ))
                                .orElse(false))
                        .toList();

        if (
                matchingFailures.size() != 1
        ) {

            return List.of();
        }

        return List.of(
                new AssertionReplacement(
                        matchingFailures.get(0)
                                .expectedText(),
                        actualText.get()
                )
        );
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

    private Optional<String> scenarioAssertionText(
            String userInstruction
    ) {

        List<Pattern> patterns =
                List.of(
                        Pattern.compile(
                                "(?i)(?:scenario|test)\\s+\"([^\"]+)\""
                        ),
                        Pattern.compile(
                                "(?i)(?:scenario|test)\\s+([^\"\\r\\n,]+)"
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
                        )
                        .filter(scenario -> !scenario.isBlank());
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

        List<String> undefinedSteps =
                undefinedCucumberSteps(output);

        if (
                !undefinedSteps.isEmpty()
                        ||
                        containsUndefinedCucumberFailure(lowerOutput)
        ) {

            String detail =
                    undefinedSteps.isEmpty()
                            ? ""
                            : " Missing support: "
                            + String.join(
                            ", ",
                            undefinedSteps
                    )
                            + ".";

            return "The last generated test has Cucumber undefined generated steps."
                    + detail
                    + " This means the feature contains steps that generated Java support did not handle yet.";
        }

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

    List<String> missingExpectedTextsForExecution(
            String output
    ) {

        return missingExpectedTexts(output);
    }

    private List<String> missingExpectedTexts(
            String output
    ) {

        return missingExpectedTextFailures(output)
                .stream()
                .map(MissingAssertionFailure::expectedText)
                .distinct()
                .toList();
    }

    String assertionMismatchRepairGuidance(
            String output
    ) {

        List<MissingAssertionFailure> failures =
                missingExpectedTextFailures(output);

        if (
                failures.isEmpty()
        ) {

            return "";
        }

        if (
                failures.size() == 1
        ) {

            return "\n\nThis looks like an assertion-text mismatch, not a locator or URL problem. Tell me the exact UI sentence for this failed assertion, for example: `replace assertion \""
                    + failures.get(0)
                    .expectedText()
                    + "\" with \"Please enter a valid number.\"`.";
        }

        StringBuilder guidance =
                new StringBuilder(
                        "\n\nThis looks like multiple assertion-text mismatches, not a locator or URL problem. I did not change the feature file because one actual sentence may not apply to every failed assertion.\n\nFailed assertions:"
                );

        for (
                MissingAssertionFailure failure
                : failures
        ) {

            guidance.append("\n- ");

            failure.scenario()
                    .ifPresent(scenario -> guidance.append("\"")
                            .append(scenario)
                            .append("\" expected "));

            guidance.append("\"")
                    .append(failure.expectedText())
                    .append("\"");
        }

        guidance.append(
                "\n\nTell me the specific mapping, for example: `replace assertion \""
        );

        guidance.append(
                failures.get(0)
                        .expectedText()
        );

        guidance.append(
                "\" with \"Please enter a valid number.\"` or `In scenario \""
        );

        guidance.append(
                failures.get(0)
                        .scenario()
                        .orElse("scenario name")
        );

        guidance.append(
                "\", the actual sentence is \"Please enter a valid number.\"`."
        );

        return guidance.toString();
    }

    private List<MissingAssertionFailure> missingExpectedTextFailures(
            String output
    ) {

        if (
                output == null
                        ||
                        output.isBlank()
        ) {

            return List.of();
        }

        Pattern failureHeader =
                Pattern.compile(
                        "^\\s*(.+?)\\s*<<<\\s*FAILURE!.*$",
                        Pattern.CASE_INSENSITIVE
                );

        Pattern expectedText =
                Pattern.compile(
                        "Expected page to contain text: ([^\\r\\n<]+)",
                        Pattern.CASE_INSENSITIVE
                );

        List<MissingAssertionFailure> failures =
                new ArrayList<>();

        String currentScenario =
                "";

        for (
                String line
                : output.lines()
                .toList()
        ) {

            Matcher headerMatcher =
                    failureHeader.matcher(line);

            if (
                    headerMatcher.find()
            ) {

                currentScenario =
                        cleanScenarioName(
                                headerMatcher.group(1)
                        );
            }

            Matcher expectedTextMatcher =
                    expectedText.matcher(line);

            if (
                    expectedTextMatcher.find()
            ) {

                MissingAssertionFailure failure =
                        new MissingAssertionFailure(
                                currentScenario.isBlank()
                                        ? Optional.empty()
                                        : Optional.of(currentScenario),
                                expectedTextMatcher.group(1)
                                        .trim()
                        );

                if (
                        !failures.contains(failure)
                ) {

                    failures.add(failure);
                }
            }
        }

        return failures;
    }

    private String cleanScenarioName(
            String rawScenario
    ) {

        if (
                rawScenario == null
        ) {

            return "";
        }

        String scenario =
                rawScenario.trim();

        int dotIndex =
                scenario.lastIndexOf('.');

        if (
                dotIndex >= 0
                        &&
                        dotIndex < scenario.length() - 1
        ) {

            scenario =
                    scenario.substring(dotIndex + 1)
                            .trim();
        }

        return scenario;
    }

    private boolean containsUndefinedCucumberFailure(
            String lowerOutput
    ) {

        return lowerOutput.contains("undefined step")
                ||
                lowerOutput.contains("undefined steps")
                ||
                lowerOutput.contains("undefined scenario")
                ||
                lowerOutput.contains("undefined scenarios")
                ||
                lowerOutput.contains("you can implement missing steps");
    }

    private List<String> undefinedCucumberSteps(
            String output
    ) {

        if (
                output == null
                        ||
                        output.isBlank()
        ) {

            return List.of();
        }

        List<String> steps =
                new ArrayList<>();

        collectUndefinedStepMatches(
                output,
                Pattern.compile(
                        "The step ['\"]([^'\"]+)['\"] is undefined",
                        Pattern.CASE_INSENSITIVE
                ),
                steps
        );

        collectUndefinedStepMatches(
                output,
                Pattern.compile(
                        "Undefined step:?\\s*([^\\r\\n]+)",
                        Pattern.CASE_INSENSITIVE
                ),
                steps
        );

        collectUndefinedStepMatches(
                output,
                Pattern.compile(
                        "@(?:Given|When|Then|And|But)\\(\"([^\"]+)\"\\)",
                        Pattern.CASE_INSENSITIVE
                ),
                steps
        );

        return steps.stream()
                .distinct()
                .limit(5)
                .toList();
    }

    private void collectUndefinedStepMatches(
            String output,
            Pattern pattern,
            List<String> matches
    ) {

        Matcher matcher =
                pattern.matcher(output);

        while (
                matcher.find()
        ) {

            String value =
                    matcher.group(1)
                            .trim();

            if (
                    !value.isBlank()
            ) {

                matches.add(value);
            }
        }
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

    private record ActionTargetReplacement(
            String from,
            String to
    ) {
    }

    private record PassengerNames(
            String firstName,
            String lastName,
            boolean firstNameProvided,
            boolean lastNameProvided
    ) {
    }

    private record PassengerFieldEntry(
            String indent,
            String keyword,
            String value,
            String field
    ) {
    }

    private record MissingAssertionFailure(
            Optional<String> scenario,
            String expectedText
    ) {
    }

    @Getter
    @Builder
    public static class RepairResult {

        private boolean changed;

        private List<String> changedFiles;

        private List<String> changes;

        private String failureSummary;

        private List<String> failureDetails;

        private String repairGuidance;

        private String repairSource;

        private String fallbackReason;
    }
}
