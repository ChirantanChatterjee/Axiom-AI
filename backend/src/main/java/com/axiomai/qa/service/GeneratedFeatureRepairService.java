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

        String latestOutput =
                latestExecutionOutput(frameworkRoot)
                        .orElse("");

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
                                latestOutput
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

        if (
                content == null
        ) {

            return new FeatureRepair(
                    "",
                    List.of()
            );
        }

        String lower =
                (content + "\n" + latestOutput)
                        .toLowerCase();

        if (
                lower.contains("parabank")
                        &&
                        lower.contains("bill")
                        &&
                        lower.contains("pay")
                        &&
                        (
                                lower.contains("admin.htm")
                                        ||
                                        lower.contains("overview.htm")
                                        ||
                                        lower.contains("unable to resolve element: send payment button")
                                        ||
                                        lower.contains("expected page to contain text: bill payment complete")
                                        ||
                                        lower.contains("expected page to contain text: invalid amount")
                                        ||
                                        !lower.contains("verify account")
                        )
        ) {

            return repairParaBankBillPay(
                    content
            );
        }

        return new FeatureRepair(
                content,
                List.of()
        );
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

            repaired.append(line)
                    .append(System.lineSeparator());

            if (
                    line.trim()
                            .startsWith("Given user launches")
                            &&
                            !scenarioContains(
                                    lines,
                                    i,
                                    "user clicks \"Bill Pay\""
                            )
            ) {

                repaired.append("  When user enters \"${username}\" into \"username\"")
                        .append(System.lineSeparator());
                repaired.append("  And user enters \"${password}\" into \"password\"")
                        .append(System.lineSeparator());
                repaired.append("  And user clicks \"login button\"")
                        .append(System.lineSeparator());
                repaired.append("  Then user should see \"Accounts Overview\"")
                        .append(System.lineSeparator());
                repaired.append("  When user clicks \"Bill Pay\"")
                        .append(System.lineSeparator());
                repaired.append("  Then user should see \"Bill Payment Service\"")
                        .append(System.lineSeparator());

                changes.add(
                        "Inserted ParaBank login and Bill Pay navigation before bill-pay form steps."
                );
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
                lowerOutput.contains(
                        "the username and password could not be verified"
                )
                        ||
                        lowerOutput.contains(
                                "invalid username/password"
                        )
        ) {

            return "The last generated test reached ParaBank, but login failed because the supplied username/password were rejected. This is a runtime test-data issue, not a feature-file or locator issue. Update the workspace credentials with valid ParaBank credentials, then rerun the bill-pay tag.";
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

    @Getter
    @Builder
    public static class RepairResult {

        private boolean changed;

        private List<String> changedFiles;

        private List<String> changes;

        private String failureSummary;
    }
}
