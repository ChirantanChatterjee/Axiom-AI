package com.axiomai.qa.service;

import com.axiomai.qa.models.RequirementTestCase;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class RequirementDocumentAnalyzer {

    private static final Pattern STORY_HEADER =
            Pattern.compile(
                    "\\b(?:(US[-_]?\\d+)\\s*:|User\\s+Story\\s+(\\d+)\\s*(?::|[-\\u2013\\u2014])?)",
                    Pattern.CASE_INSENSITIVE
            );

    Analysis analyze(
            String requirement,
            String featureName,
            String url
    ) {

        List<RequirementStory> stories =
                parseStories(requirement);

        if (
                stories.isEmpty()
        ) {

            return Analysis.empty();
        }

        List<RequirementTestCase> testCases =
                generateTestCases(stories);

        return new Analysis(
                testCases,
                generateFeature(
                        testCases,
                        featureName,
                        url
                )
        );
    }

    private List<RequirementStory> parseStories(
            String requirement
    ) {

        if (
                requirement == null
                        ||
                        requirement.isBlank()
        ) {

            return List.of();
        }

        Matcher matcher =
                STORY_HEADER.matcher(requirement);

        List<StoryMatch> matches =
                new ArrayList<>();

        while (
                matcher.find()
        ) {

            String storyId =
                    matcher.group(1) != null
                            ? matcher.group(1)
                            : numberedStoryId(
                                    matcher.group(2)
                            );

            matches.add(
                    new StoryMatch(
                            storyId,
                            matcher.end(),
                            matcher.start(),
                            matcher.group(2) != null
                    )
            );
        }

        if (
                matches.isEmpty()
                        &&
                        looksLikeRequirementText(requirement)
        ) {

            String title =
                    extractFallbackTitle(requirement);

            return List.of(
                    new RequirementStory(
                            "REQ-001",
                            title.isBlank()
                                    ? "Requirement"
                                    : title,
                            requirement,
                            extractCriteria(requirement),
                            false
                    )
            );
        }

        List<RequirementStory> stories =
                new ArrayList<>();

        for (
                int i = 0;
                i < matches.size();
                i++
        ) {

            StoryMatch current =
                    matches.get(i);

            int end =
                    i + 1 < matches.size()
                            ? matches.get(i + 1).start()
                            : requirement.length();

            String section =
                    requirement.substring(
                            current.contentStart(),
                            end
                    )
                            .trim();

            String title =
                    extractTitle(section);

            String content =
                    section.substring(
                            Math.min(
                                    title.length(),
                                    section.length()
                            )
                    )
                            .trim();

            stories.add(
                    new RequirementStory(
                            normalizeStoryId(
                                    current.id()
                            ),
                            title.isBlank()
                                    ? normalizeStoryId(current.id())
                                    : title,
                            content,
                            extractCriteria(content),
                            current.narrativeStory()
                    )
            );
        }

        return stories;
    }

    private boolean looksLikeRequirementText(
            String value
    ) {

        String lower =
                safe(value)
                        .toLowerCase(Locale.ROOT);

        return lower.contains("acceptance criteria")
                &&
                (
                        lower.contains("user story")
                                ||
                                lower.contains("as a ")
                                ||
                                lower.contains("as an ")
                );
    }

    private String extractFallbackTitle(
            String requirement
    ) {

        String cleaned =
                safe(requirement)
                        .replaceAll("\\s+", " ")
                        .trim();

        Matcher titledStory =
                Pattern.compile(
                        "(?i)\\bUser\\s+Story\\s*[:\\-]\\s*(.+?)(?=\\s+As\\s+(?:a|an|the)\\b|\\s+Acceptance\\s+Criteria\\b|$)"
                )
                        .matcher(cleaned);

        if (
                titledStory.find()
        ) {

            return titledStory.group(1)
                    .trim();
        }

        Matcher acceptanceMarker =
                Pattern.compile(
                        "(?i)\\bAcceptance\\s+Criteria\\b"
                )
                        .matcher(cleaned);

        if (
                acceptanceMarker.find()
        ) {

            cleaned =
                    cleaned.substring(
                            0,
                            acceptanceMarker.start()
                    )
                            .trim();
        }

        if (
                cleaned.length() > 70
        ) {

            cleaned =
                    cleaned.substring(0, 70)
                            .trim();
        }

        return cleaned;
    }

    private String extractTitle(
            String section
    ) {

        if (
                section == null
        ) {

            return "";
        }

        String cleaned =
                section.trim();

        if (
                cleaned.isBlank()
        ) {

            return "";
        }

        int end =
                cleaned.length();

        Matcher marker =
                Pattern.compile(
                        "(?i)\\s+(?=As\\s+(?:a|an|the)\\b|Acceptance\\s+Criteria\\b)"
                )
                        .matcher(cleaned);

        if (
                marker.find()
        ) {

            end =
                    Math.min(
                            end,
                            marker.start()
                    );
        }

        int newline =
                cleaned.indexOf('\n');

        if (
                newline >= 0
        ) {

            end =
                    Math.min(
                            end,
                            newline
                    );
        }

        return cleaned.substring(0, end)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<String> extractCriteria(
            String content
    ) {

        if (
                content == null
                        ||
                        content.isBlank()
        ) {

            return List.of();
        }

        String criteriaText =
                content;

        Matcher marker =
                Pattern.compile(
                        "(?i)Acceptance\\s+Criteria"
                )
                        .matcher(criteriaText);

        if (
                marker.find()
        ) {

            criteriaText =
                    criteriaText.substring(
                            marker.end()
                    );
        }

        String normalized =
                criteriaText.replaceAll("\\s+", " ")
                        .trim();

        if (
                normalized.isBlank()
        ) {

            return List.of();
        }

        String[] parts =
                normalized.split("(?<=[.;])\\s+");

        List<String> criteria =
                new ArrayList<>();

        for (
                String part
                : parts
        ) {

            String cleaned =
                    part.replaceAll("^[\\-\\u2022\\s]+", "")
                            .replaceAll("[.;]\\s*$", "")
                            .trim();

            if (
                    !cleaned.isBlank()
                            &&
                            !cleaned.toLowerCase(Locale.ROOT)
                                    .startsWith("as a ")
                            &&
                            !cleaned.toLowerCase(Locale.ROOT)
                                    .startsWith("as an ")
            ) {

                criteria.add(cleaned);
            }
        }

        return criteria;
    }

    private List<RequirementTestCase> generateTestCases(
            List<RequirementStory> stories
    ) {

        List<RequirementTestCase> testCases =
                new ArrayList<>();

        boolean hasRegistration =
                false;

        boolean hasLogout =
                false;

        boolean hasSecurePages =
                false;

        for (
                RequirementStory story
                : stories
        ) {

            String lower =
                    story.searchText();

            String titleLower =
                    safe(story.title())
                            .toLowerCase(Locale.ROOT);

            boolean classifyFromFullText =
                    titleLower.isBlank()
                            ||
                            titleLower.equals(
                                    safe(story.id())
                                            .toLowerCase(Locale.ROOT)
                            );

            int before =
                    testCases.size();

            if (
                    story.narrativeStory()
            ) {

                addNarrativeStoryCase(
                        testCases,
                        story
                );

                continue;
            }

            if (
                    containsAny(
                            titleLower,
                            "registration",
                            "register",
                            "sign up"
                    )
                            ||
                            (
                                    classifyFromFullText
                                            &&
                                            containsAny(
                                                    lower,
                                                    "registration",
                                                    "register",
                                                    "sign up"
                                            )
                            )
            ) {

                hasRegistration =
                        true;

                addRegistrationCases(
                        testCases,
                        story
                );
            }

            if (
                    containsAny(
                            titleLower,
                            "mandatory",
                            "required",
                            "field validation"
                    )
                            ||
                            lower.contains("required registration fields")
                            ||
                            lower.contains("empty required fields")
            ) {

                addMandatoryFieldCases(
                        testCases,
                        story
                );
            }

            if (
                    containsAny(
                            titleLower,
                            "password confirmation",
                            "mismatched password"
                    )
                            ||
                            lower.contains("password and confirm password must match")
                            ||
                            lower.contains("password must match")
            ) {

                addCase(
                        testCases,
                        story.id(),
                        "Register with mismatched passwords",
                        "password: Test123, confirm: Test456",
                        "Password mismatch error appears",
                        "negative",
                        "validation",
                        "registration"
                );
            }

            if (
                    containsAny(
                            titleLower,
                            "login",
                            "log in",
                            "sign in"
                    )
                            ||
                            (
                                    classifyFromFullText
                                            &&
                                            containsAny(
                                                    lower,
                                                    "login",
                                                    "log in",
                                                    "sign in"
                                            )
                            )
            ) {

                addLoginCases(
                        testCases,
                        story
                );
            }

            if (
                    containsAny(
                            titleLower,
                            "account overview",
                            "accounts are listed",
                            "balances"
                    )
                            ||
                            (
                                    classifyFromFullText
                                            &&
                                            containsAny(
                                                    lower,
                                                    "account overview",
                                                    "accounts are listed",
                                                    "balances"
                                            )
                            )
            ) {

                hasSecurePages =
                        true;

                addCase(
                        testCases,
                        story.id(),
                        "View account overview after login",
                        "Valid user",
                        "Accounts and balances are displayed",
                        "positive",
                        "account_overview"
                );
            }

            if (
                    containsAny(
                            titleLower,
                            "open new account",
                            "checking",
                            "savings"
                    )
                            ||
                            (
                                    classifyFromFullText
                                            &&
                                            containsAny(
                                                    lower,
                                                    "open new account",
                                                    "checking",
                                                    "savings"
                                            )
                            )
            ) {

                hasSecurePages =
                        true;

                addCase(
                        testCases,
                        story.id(),
                        "Open new checking account",
                        "Account type: CHECKING",
                        "New account is created",
                        "positive",
                        "open_account"
                );

                addCase(
                        testCases,
                        story.id(),
                        "Open new savings account",
                        "Account type: SAVINGS",
                        "New account is created",
                        "positive",
                        "open_account"
                );
            }

            if (
                    containsAny(
                            titleLower,
                            "transfer",
                            "from-account",
                            "to-account"
                    )
                            ||
                            (
                                    classifyFromFullText
                                            &&
                                            containsAny(
                                                    lower,
                                                    "transfer",
                                                    "from-account",
                                                    "to-account"
                                            )
                            )
            ) {

                hasSecurePages =
                        true;

                addCase(
                        testCases,
                        story.id(),
                        "Transfer valid amount between accounts",
                        "Amount: 50",
                        "Transfer confirmation displayed",
                        "positive",
                        "transfer"
                );

                addCase(
                        testCases,
                        story.id(),
                        "Transfer with blank amount",
                        "Blank amount",
                        "Validation/error message shown",
                        "negative",
                        "required_field",
                        "transfer"
                );
            }

            if (
                    containsAny(
                            titleLower,
                            "bill payment",
                            "bill pay",
                            "pay a bill",
                            "pay bill"
                    )
                            ||
                            (
                                    classifyFromFullText
                                            &&
                                            containsAny(
                                                    lower,
                                                    "bill payment",
                                                    "bill pay",
                                                    "pay a bill",
                                                    "pay bill"
                                            )
                            )
            ) {

                hasSecurePages =
                        true;

                addCase(
                        testCases,
                        story.id(),
                        "Pay bill with valid payee data",
                        "Payee + amount",
                        "Bill payment confirmation displayed",
                        "positive",
                        "bill_pay"
                );

                addCase(
                        testCases,
                        story.id(),
                        "Pay bill with mismatched account verification",
                        "Different account numbers",
                        "Validation/error shown",
                        "negative",
                        "validation",
                        "bill_pay"
                );

                addCase(
                        testCases,
                        story.id(),
                        "Pay bill with blank amount",
                        "Blank amount",
                        "Validation/error shown",
                        "negative",
                        "required_field",
                        "bill_pay"
                );
            }

            if (
                    containsAny(
                            titleLower,
                            "find transactions",
                            "search account transactions",
                            "transaction id",
                            "date range"
                    )
                            ||
                            (
                                    classifyFromFullText
                                            &&
                                            containsAny(
                                                    lower,
                                                    "find transactions",
                                                    "search account transactions",
                                                    "transaction id",
                                                    "date range"
                                            )
                            )
            ) {

                hasSecurePages =
                        true;

                addTransactionCases(
                        testCases,
                        story
                );
            }

            if (
                    containsAny(
                            titleLower,
                            "logout",
                            "log out"
                    )
                            ||
                            (
                                    classifyFromFullText
                                            &&
                                            containsAny(
                                                    lower,
                                                    "logout",
                                                    "log out"
                                            )
                            )
            ) {

                hasLogout =
                        true;

                addCase(
                        testCases,
                        story.id(),
                        "Logout after login",
                        "Logged-in user",
                        "User returns to login page",
                        "positive",
                        "logout"
                );
            }

            if (
                    testCases.size() == before
            ) {

                addGenericCases(
                        testCases,
                        story
                );
            }
        }

        if (
                hasLogout
                        &&
                        hasSecurePages
        ) {

            addCase(
                    testCases,
                    "Security",
                    "Access account overview after logout",
                    "Browser back button",
                    "Secure page should not be accessible",
                    "negative",
                    "security"
            );
        }

        if (
                hasRegistration
        ) {

            addCase(
                    testCases,
                    "UI",
                    "Verify register page field labels",
                    "Registration page",
                    "All labels and input fields are visible",
                    "ui",
                    "registration"
            );
        }

        return testCases;
    }

    private void addNarrativeStoryCase(
            List<RequirementTestCase> testCases,
            RequirementStory story
    ) {

        String lower =
                story.searchText();

        if (
                containsAny(
                        lower,
                        "successful login",
                        "valid credentials",
                        "inventory page"
                )
                        &&
                        containsAny(
                                lower,
                                "login",
                                "log in"
                        )
        ) {

            addCase(
                    testCases,
                    story.id(),
                    "Successful login redirects to inventory",
                    "standard_user / secret_sauce",
                    "Inventory page and shopping cart are available after login",
                    "positive",
                    "login"
            );

            return;
        }

        if (
                containsAny(
                        lower,
                        "invalid login",
                        "incorrect",
                        "invalid username",
                        "invalid password",
                        "empty username",
                        "empty password",
                        "both fields empty"
                )
        ) {

            addCase(
                    testCases,
                    story.id(),
                    "Invalid login shows credential validation",
                    "Invalid and empty credentials",
                    "Error message is visible and user remains on login page",
                    "negative",
                    "validation",
                    "login"
            );

            return;
        }

        if (
                containsAny(
                        lower,
                        "locked user",
                        "locked out",
                        "locked users cannot"
                )
        ) {

            addCase(
                    testCases,
                    story.id(),
                    "Locked user cannot access inventory",
                    "locked_out_user / secret_sauce",
                    "Locked user error appears and inventory is not accessible",
                    "negative",
                    "security",
                    "login"
            );

            return;
        }

        if (
                containsAny(
                        lower,
                        "product sorting",
                        "sort products",
                        "name a-z",
                        "price low-high"
                )
        ) {

            addCase(
                    testCases,
                    story.id(),
                    "Sort products by name and price",
                    "Sort options: az, za, lohi, hilo",
                    "Product order matches the selected sort option",
                    "positive",
                    "sorting",
                    "inventory"
            );

            return;
        }

        if (
                containsAny(
                        lower,
                        "view product list",
                        "browse available products",
                        "product name visible",
                        "product image visible"
                )
        ) {

            addCase(
                    testCases,
                    story.id(),
                    "View product inventory list",
                    "Logged-in customer",
                    "Product names, descriptions, prices, images, and add-to-cart controls are visible",
                    "positive",
                    "inventory"
            );

            return;
        }

        if (
                containsAny(
                        lower,
                        "remove product",
                        "remove unwanted products",
                        "cart count decreases"
                )
        ) {

            addCase(
                    testCases,
                    story.id(),
                    "Remove product from cart",
                    "Cart with one product",
                    "Removed product is no longer in cart and cart count decreases",
                    "positive",
                    "cart"
            );

            return;
        }

        if (
                containsAny(
                        lower,
                        "add product to cart",
                        "add products into cart",
                        "clicking add to cart"
                )
        ) {

            addCase(
                    testCases,
                    story.id(),
                    "Add product to cart",
                    "Sauce Labs Backpack",
                    "Cart badge updates and selected product is present in cart",
                    "positive",
                    "cart"
            );

            return;
        }

        if (
                containsAny(
                        lower,
                        "cart persistence",
                        "cart data to persist",
                        "refresh should not clear cart"
                )
        ) {

            addCase(
                    testCases,
                    story.id(),
                    "Cart persists during navigation and refresh",
                    "Cart with one product",
                    "Cart item remains after navigation and refresh",
                    "positive",
                    "cart",
                    "persistence"
            );

            return;
        }

        if (
                containsAny(
                        lower,
                        "checkout overview",
                        "review my order",
                        "tax calculation",
                        "total price"
                )
        ) {

            addCase(
                    testCases,
                    story.id(),
                    "Checkout overview shows accurate totals",
                    "One product in cart and valid checkout information",
                    "Overview lists product, tax, and accurate total price",
                    "positive",
                    "checkout"
            );

            return;
        }

        if (
                containsAny(
                        lower,
                        "checkout information",
                        "shipping information",
                        "first name mandatory",
                        "postal code mandatory",
                        "missing fields"
                )
        ) {

            addCase(
                    testCases,
                    story.id(),
                    "Checkout information validates required fields",
                    "Blank and valid checkout information",
                    "Missing field validation appears before checkout can continue",
                    "negative",
                    "required_field",
                    "checkout"
            );

            return;
        }

        if (
                containsAny(
                        lower,
                        "logout",
                        "log out"
                )
        ) {

            addCase(
                    testCases,
                    story.id(),
                    "Logout ends the active session",
                    "Logged-in user",
                    "User returns to login page",
                    "positive",
                    "logout"
            );

            return;
        }

        addCase(
                testCases,
                story.id(),
                "Validate " + concise(story.title()),
                "Requirement-derived data",
                story.title() + " behavior is available",
                containsAny(
                        lower,
                        "invalid",
                        "empty",
                        "cannot",
                        "not able",
                        "locked",
                        "mandatory",
                        "missing"
                )
                        ? "negative"
                        : "positive"
        );
    }

    private void addRegistrationCases(
            List<RequirementTestCase> testCases,
            RequirementStory story
    ) {

        addCase(
                testCases,
                story.id(),
                "Register with valid customer details",
                "Unique username, valid password",
                "Account is created successfully",
                "positive",
                "registration"
        );

        addCase(
                testCases,
                story.id(),
                "Register with already used username",
                "Existing username",
                "Duplicate username error appears",
                "negative",
                "validation",
                "registration"
        );
    }

    private void addMandatoryFieldCases(
            List<RequirementTestCase> testCases,
            RequirementStory story
    ) {

        addCase(
                testCases,
                story.id(),
                "Submit registration form with all fields blank",
                "Blank form",
                "Required field validation errors appear",
                "negative",
                "required_field",
                "registration"
        );

        for (
                String field
                : mandatoryFields(story)
        ) {

            addCase(
                    testCases,
                    story.id(),
                    "Register without " + field,
                    toTitle(field) + " blank",
                    toTitle(field) + " error appears",
                    "negative",
                    "required_field",
                    "registration"
            );
        }
    }

    private List<String> mandatoryFields(
            RequirementStory story
    ) {

        String lower =
                story.searchText();

        List<String> known =
                List.of(
                        "first name",
                        "last name",
                        "address",
                        "city",
                        "state",
                        "zip code",
                        "phone",
                        "ssn",
                        "username",
                        "password",
                        "confirmation password"
                );

        List<String> fields =
                new ArrayList<>();

        for (
                String field
                : known
        ) {

            if (
                    lower.contains(field)
            ) {

                fields.add(field);
            }
        }

        if (
                fields.isEmpty()
        ) {

            fields.add("required field");
        }

        return fields;
    }

    private void addLoginCases(
            List<RequirementTestCase> testCases,
            RequirementStory story
    ) {

        addCase(
                testCases,
                story.id(),
                "Login with valid credentials",
                "Registered username/password",
                "User lands on account overview",
                "positive",
                "login"
        );

        addCase(
                testCases,
                story.id(),
                "Login with invalid password",
                "Valid username, wrong password",
                "Login error appears",
                "negative",
                "validation",
                "login"
        );
    }

    private void addTransactionCases(
            List<RequirementTestCase> testCases,
            RequirementStory story
    ) {

        addCase(
                testCases,
                story.id(),
                "Find transaction by transaction ID",
                "Transaction ID",
                "Matching transactions displayed",
                "positive",
                "find_transactions"
        );

        addCase(
                testCases,
                story.id(),
                "Find transaction by amount",
                "Amount: 50",
                "Matching transactions displayed",
                "positive",
                "find_transactions"
        );

        addCase(
                testCases,
                story.id(),
                "Find transaction by date range",
                "Valid date range",
                "Transactions in range displayed",
                "positive",
                "find_transactions"
        );

        addCase(
                testCases,
                story.id(),
                "Find transaction with no matching results",
                "Amount: 999999",
                "No-match scenario is handled gracefully",
                "negative",
                "find_transactions"
        );
    }

    private void addGenericCases(
            List<RequirementTestCase> testCases,
            RequirementStory story
    ) {

        if (
                story.criteria()
                        .isEmpty()
        ) {

            addCase(
                    testCases,
                    story.id(),
                    "Validate " + story.title(),
                    "Requirement-derived data",
                    story.title() + " behavior is available",
                    "positive"
            );

            return;
        }

        for (
                String criterion
                : story.criteria()
        ) {

            addCase(
                    testCases,
                    story.id(),
                    "Validate " + concise(criterion),
                    "Requirement-derived data",
                    criterion,
                    "positive"
            );
        }
    }

    private void addCase(
            List<RequirementTestCase> testCases,
            String userStory,
            String scenario,
            String testData,
            String expectedResult,
            String... extraTags
    ) {

        for (
                RequirementTestCase existing
                : testCases
        ) {

            if (
                    existing.getScenario()
                            .equalsIgnoreCase(scenario)
                            &&
                            existing.getUserStory()
                                    .equalsIgnoreCase(userStory)
            ) {

                return;
            }
        }

        String tcId =
                "TC-%03d".formatted(
                        testCases.size() + 1
                );

        Set<String> tags =
                new LinkedHashSet<>();

        tags.add("generated");
        tags.add("ai_requirement");
        tags.add("requirements");
        tags.add(tag(tcId));
        tags.add(tag(userStory));

        for (
                String extraTag
                : extraTags
        ) {

            tags.add(
                    tag(extraTag)
            );
        }

        testCases.add(
                new RequirementTestCase(
                        tcId,
                        userStory,
                        scenario,
                        testData,
                        expectedResult,
                        new ArrayList<>(tags)
                )
        );
    }

    private String generateFeature(
            List<RequirementTestCase> testCases,
            String featureName,
            String url
    ) {

        StringBuilder feature =
                new StringBuilder();

        feature.append("Feature: ")
                .append(
                        toTitle(
                                featureName == null
                                        ||
                                        featureName.isBlank()
                                        ? "requirements"
                                        : featureName
                        )
                )
                .append("\n\n");

        for (
                RequirementTestCase testCase
                : testCases
        ) {

            appendTags(
                    feature,
                    testCase
            );

            feature.append("  Scenario: ")
                    .append(testCase.getTcId())
                    .append(" ")
                    .append(testCase.getScenario())
                    .append("\n");

            appendSteps(
                    feature,
                    testCase,
                    url
            );

            feature.append("\n");
        }

        return feature.toString();
    }

    private void appendTags(
            StringBuilder feature,
            RequirementTestCase testCase
    ) {

        feature.append("  ");

        for (
                String tag
                : testCase.getTags()
        ) {

            feature.append("@")
                    .append(tag)
                    .append(" ");
        }

        feature.append("\n");
    }

    private void appendSteps(
            StringBuilder feature,
            RequirementTestCase testCase,
            String url
    ) {

        String scenario =
                safe(testCase.getScenario())
                        .toLowerCase(Locale.ROOT);

        appendLaunch(
                feature,
                url
        );

        if (
                scenario.contains("successful login redirects to inventory")
        ) {

            appendSauceLoginStart(
                    feature,
                    "standard_user",
                    "secret_sauce"
            );
            feature.append("    Then user should see \"Products\"\n")
                    .append("    And user clicks \"cart\"\n")
                    .append("    Then user should see \"Your Cart\"\n");
            return;
        }

        if (
                scenario.contains("invalid login shows credential validation")
        ) {

            appendSauceLoginStart(
                    feature,
                    "invalid_user",
                    "wrong_password"
            );
            feature.append("    Then user should see \"Epic sadface\"\n")
                    .append("    Then user should see \"Username and password do not match\"\n");
            return;
        }

        if (
                scenario.contains("locked user cannot access inventory")
        ) {

            appendSauceLoginStart(
                    feature,
                    "locked_out_user",
                    "secret_sauce"
            );
            feature.append("    Then user should see \"locked out\"\n");
            return;
        }

        if (
                scenario.contains("view product inventory list")
        ) {

            appendSauceLoggedInInventory(feature);
            feature.append("    Then user should see \"Sauce Labs Backpack\"\n")
                    .append("    Then user should see \"Sauce Labs Bike Light\"\n")
                    .append("    Then user should see \"$29.99\"\n")
                    .append("    Then user should see \"Add to cart\"\n");
            return;
        }

        if (
                scenario.contains("sort products by name and price")
        ) {

            appendSauceLoggedInInventory(feature);
            feature.append("    When user enters \"az\" into \"sort\"\n")
                    .append("    Then product list should be sorted by \"name ascending\"\n")
                    .append("    When user enters \"za\" into \"sort\"\n")
                    .append("    Then product list should be sorted by \"name descending\"\n")
                    .append("    When user enters \"lohi\" into \"sort\"\n")
                    .append("    Then product list should be sorted by \"price ascending\"\n")
                    .append("    When user enters \"hilo\" into \"sort\"\n")
                    .append("    Then product list should be sorted by \"price descending\"\n");
            return;
        }

        if (
                scenario.contains("add product to cart")
        ) {

            appendSauceLoggedInInventory(feature);
            feature.append("    When user clicks \"add Sauce Labs Backpack to cart\"\n")
                    .append("    Then cart badge should show \"1\"\n")
                    .append("    Then user should see \"Remove\"\n")
                    .append("    And user clicks \"cart\"\n")
                    .append("    Then cart should contain \"Sauce Labs Backpack\"\n");
            return;
        }

        if (
                scenario.contains("remove product from cart")
        ) {

            appendSauceLoggedInInventory(feature);
            feature.append("    When user clicks \"add Sauce Labs Backpack to cart\"\n")
                    .append("    Then cart badge should show \"1\"\n")
                    .append("    When user clicks \"remove Sauce Labs Backpack\"\n")
                    .append("    Then cart badge should show \"0\"\n")
                    .append("    And user clicks \"cart\"\n")
                    .append("    Then cart should not contain \"Sauce Labs Backpack\"\n");
            return;
        }

        if (
                scenario.contains("cart persists during navigation and refresh")
        ) {

            appendSauceLoggedInInventory(feature);
            feature.append("    When user clicks \"add Sauce Labs Backpack to cart\"\n")
                    .append("    Then cart badge should show \"1\"\n")
                    .append("    And user clicks \"cart\"\n")
                    .append("    Then cart should contain \"Sauce Labs Backpack\"\n")
                    .append("    When user refreshes page\n")
                    .append("    Then cart should contain \"Sauce Labs Backpack\"\n")
                    .append("    And user clicks \"continue shopping\"\n")
                    .append("    Then user should see \"Products\"\n")
                    .append("    And user clicks \"cart\"\n")
                    .append("    Then cart should contain \"Sauce Labs Backpack\"\n");
            return;
        }

        if (
                scenario.contains("checkout information validates required fields")
        ) {

            appendSauceCheckoutStart(feature);
            feature.append("    And user clicks \"continue\"\n")
                    .append("    Then user should see \"First Name is required\"\n")
                    .append("    And user enters \"AIF\" into \"first name\"\n")
                    .append("    And user enters \"User\" into \"last name\"\n")
                    .append("    And user enters \"94105\" into \"postal code\"\n")
                    .append("    And user clicks \"continue\"\n")
                    .append("    Then user should see \"Checkout: Overview\"\n");
            return;
        }

        if (
                scenario.contains("checkout overview shows accurate totals")
        ) {

            appendSauceCheckoutOverviewStart(feature);
            feature.append("    Then user should see \"Checkout: Overview\"\n")
                    .append("    Then cart should contain \"Sauce Labs Backpack\"\n")
                    .append("    Then user should see \"Item total:\"\n")
                    .append("    Then user should see \"Tax:\"\n")
                    .append("    Then checkout total should equal item total plus tax\n");
            return;
        }

        if (
                scenario.contains("field label")
                        ||
                        scenario.contains("labels")
        ) {

            feature.append("    When user clicks \"Register\"\n")
                    .append("    Then user should see \"First Name\"\n")
                    .append("    Then user should see \"Last Name\"\n")
                    .append("    Then user should see \"Username\"\n")
                    .append("    Then user should see \"Password\"\n");
            return;
        }

        if (
                scenario.contains("register")
                        ||
                        scenario.contains("registration")
        ) {

            appendRegistrationSteps(
                    feature,
                    scenario
            );

            return;
        }

        if (
                scenario.contains("after logout")
        ) {

            appendLoggedInStart(feature);
            feature.append("    And user clicks \"Accounts Overview\"\n")
                    .append("    And user clicks \"Logout\"\n")
                    .append("    And user clicks \"browser back\"\n")
                    .append("    Then user should see \"login page\"\n");
            return;
        }

        if (
                scenario.contains("logout")
        ) {

            appendLoggedInStart(feature);
            feature.append("    And user clicks \"Logout\"\n")
                    .append("    Then user should see \"login page\"\n");
            return;
        }

        if (
                scenario.contains("login")
        ) {

            appendLoginScenarioSteps(
                    feature,
                    scenario
            );

            return;
        }

        if (
                scenario.contains("account overview")
                        &&
                        !scenario.contains("after logout")
        ) {

            appendLoggedInStart(feature);
            feature.append("    And user clicks \"Accounts Overview\"\n")
                    .append("    Then user should see \"accounts and balances\"\n");
            return;
        }

        if (
                scenario.contains("open new")
        ) {

            appendOpenAccountSteps(
                    feature,
                    scenario
            );

            return;
        }

        if (
                scenario.contains("transfer")
        ) {

            appendTransferSteps(
                    feature,
                    scenario
            );

            return;
        }

        if (
                scenario.contains("pay bill")
                        ||
                        scenario.contains("bill payment")
        ) {

            appendBillPaySteps(
                    feature,
                    scenario
            );

            return;
        }

        if (
                scenario.contains("transaction")
        ) {

            appendFindTransactionSteps(
                    feature,
                    scenario
            );

            return;
        }

        feature.append("    Then flow should complete successfully\n");
    }

    private void appendSauceLoggedInInventory(
            StringBuilder feature
    ) {

        appendSauceLoginStart(
                feature,
                "standard_user",
                "secret_sauce"
        );
        feature.append("    Then user should see \"Products\"\n");
    }

    private void appendSauceCheckoutStart(
            StringBuilder feature
    ) {

        appendSauceLoggedInInventory(feature);
        feature.append("    When user clicks \"add Sauce Labs Backpack to cart\"\n")
                .append("    And user clicks \"cart\"\n")
                .append("    Then cart should contain \"Sauce Labs Backpack\"\n")
                .append("    And user clicks \"checkout\"\n")
                .append("    Then user should see \"Checkout: Your Information\"\n");
    }

    private void appendSauceCheckoutOverviewStart(
            StringBuilder feature
    ) {

        appendSauceCheckoutStart(feature);
        feature.append("    And user enters \"AIF\" into \"first name\"\n")
                .append("    And user enters \"User\" into \"last name\"\n")
                .append("    And user enters \"94105\" into \"postal code\"\n")
                .append("    And user clicks \"continue\"\n");
    }

    private void appendSauceLoginStart(
            StringBuilder feature,
            String username,
            String password
    ) {

        feature.append("    When user enters \"")
                .append(username)
                .append("\" into \"username\"\n")
                .append("    And user enters \"")
                .append(password)
                .append("\" into \"password\"\n")
                .append("    And user clicks \"login button\"\n");
    }

    private void appendLaunch(
            StringBuilder feature,
            String url
    ) {

        feature.append("    Given user launches \"")
                .append(safe(url))
                .append("\"\n");
    }

    private void appendRegistrationSteps(
            StringBuilder feature,
            String scenario
    ) {

        feature.append("    When user clicks \"Register\"\n");

        if (
                scenario.contains("all fields blank")
                        ||
                        scenario.contains("blank form")
        ) {

            feature.append("    And user clicks \"Register button\"\n")
                    .append("    Then user should see \"required field error\"\n");
            return;
        }

        String omittedField =
                omittedRegistrationField(scenario);

        fillRegistrationFields(
                feature,
                omittedField,
                scenario.contains("mismatched passwords")
                        ? "Test123"
                        : "${password}",
                scenario.contains("mismatched passwords")
                        ? "Test456"
                        : "${confirmPassword}",
                scenario.contains("already used")
                        ? "${username}"
                        : "${username}"
        );

        feature.append("    And user clicks \"Register button\"\n");

        if (
                scenario.contains("mismatched passwords")
        ) {

            feature.append("    Then user should see \"password mismatch error\"\n");
            return;
        }

        if (
                scenario.contains("already used")
        ) {

            feature.append("    Then user should see \"duplicate username error\"\n");
            return;
        }

        if (
                omittedField != null
        ) {

            feature.append("    Then user should see \"")
                    .append(omittedField)
                    .append(" error\"\n");
            return;
        }

        feature.append("    Then user should see \"registration success message\"\n");
    }

    private void fillRegistrationFields(
            StringBuilder feature,
            String omittedField,
            String password,
            String confirmPassword,
            String username
    ) {

        appendEnterUnlessOmitted(feature, "first name", "${firstName}", omittedField);
        appendEnterUnlessOmitted(feature, "last name", "${lastName}", omittedField);
        appendEnterUnlessOmitted(feature, "address", "${address}", omittedField);
        appendEnterUnlessOmitted(feature, "city", "${city}", omittedField);
        appendEnterUnlessOmitted(feature, "state", "${state}", omittedField);
        appendEnterUnlessOmitted(feature, "zip code", "${zip}", omittedField);
        appendEnterUnlessOmitted(feature, "phone", "${phone}", omittedField);
        appendEnterUnlessOmitted(feature, "ssn", "${ssn}", omittedField);
        appendEnterUnlessOmitted(feature, "username", username, omittedField);
        appendEnterUnlessOmitted(feature, "password", password, omittedField);
        appendEnterUnlessOmitted(feature, "confirm password", confirmPassword, omittedField);
    }

    private void appendEnterUnlessOmitted(
            StringBuilder feature,
            String target,
            String value,
            String omittedField
    ) {

        if (
                omittedField != null
                        &&
                        target.equalsIgnoreCase(omittedField)
        ) {

            return;
        }

        feature.append("    And user enters \"")
                .append(value)
                .append("\" into \"")
                .append(target)
                .append("\"\n");
    }

    private String omittedRegistrationField(
            String scenario
    ) {

        List<String> fields =
                List.of(
                        "first name",
                        "last name",
                        "address",
                        "city",
                        "state",
                        "zip code",
                        "phone",
                        "ssn",
                        "username",
                        "password",
                        "confirmation password"
                );

        for (
                String field
                : fields
        ) {

            if (
                    scenario.contains("without " + field)
            ) {

                return "confirmation password".equals(field)
                        ? "confirm password"
                        : field;
            }
        }

        return null;
    }

    private void appendLoginScenarioSteps(
            StringBuilder feature,
            String scenario
    ) {

        if (
                scenario.contains("invalid")
        ) {

            feature.append("    When user enters \"${username}\" into \"username\"\n")
                    .append("    And user enters \"wrong-password\" into \"password\"\n")
                    .append("    And user clicks \"login button\"\n")
                    .append("    Then user should see \"login error\"\n");
            return;
        }

        appendLoggedInStart(feature);
        feature.append("    Then user should see \"account overview\"\n");
    }

    private void appendLoggedInStart(
            StringBuilder feature
    ) {

        feature.append("    When user enters \"${username}\" into \"username\"\n")
                .append("    And user enters \"${password}\" into \"password\"\n")
                .append("    And user clicks \"login button\"\n");
    }

    private void appendOpenAccountSteps(
            StringBuilder feature,
            String scenario
    ) {

        appendLoggedInStart(feature);
        feature.append("    And user clicks \"Open New Account\"\n")
                .append("    And user enters \"")
                .append(
                        scenario.contains("savings")
                                ? "SAVINGS"
                                : "CHECKING"
                )
                .append("\" into \"account type\"\n")
                .append("    And user enters \"${account}\" into \"source account\"\n")
                .append("    And user clicks \"open new account button\"\n")
                .append("    Then user should see \"new account number\"\n");
    }

    private void appendTransferSteps(
            StringBuilder feature,
            String scenario
    ) {

        appendLoggedInStart(feature);
        feature.append("    And user clicks \"Transfer Funds\"\n");

        if (
                !scenario.contains("blank amount")
        ) {

            feature.append("    And user enters \"50\" into \"amount\"\n");
        }

        feature.append("    And user enters \"${account}\" into \"from account\"\n")
                .append("    And user enters \"${account}\" into \"to account\"\n")
                .append("    And user clicks \"transfer button\"\n");

        if (
                scenario.contains("blank amount")
        ) {

            feature.append("    Then user should see \"amount validation error\"\n");
            return;
        }

        feature.append("    Then user should see \"transfer confirmation\"\n");
    }

    private void appendBillPaySteps(
            StringBuilder feature,
            String scenario
    ) {

        appendLoggedInStart(feature);
        feature.append("    And user clicks \"Bill Pay\"\n")
                .append("    And user enters \"${payee}\" into \"payee name\"\n")
                .append("    And user enters \"${address}\" into \"address\"\n")
                .append("    And user enters \"${city}\" into \"city\"\n")
                .append("    And user enters \"${state}\" into \"state\"\n")
                .append("    And user enters \"${zip}\" into \"zip\"\n")
                .append("    And user enters \"${phone}\" into \"phone\"\n")
                .append("    And user enters \"${account}\" into \"account\"\n");

        if (
                scenario.contains("mismatched")
        ) {

            feature.append("    And user enters \"99999\" into \"verify account\"\n");

        } else {

            feature.append("    And user enters \"${account}\" into \"verify account\"\n");
        }

        if (
                !scenario.contains("blank amount")
        ) {

            feature.append("    And user enters \"${amount}\" into \"amount\"\n");
        }

        feature.append("    And user clicks \"send payment button\"\n");

        if (
                scenario.contains("mismatched")
        ) {

            feature.append("    Then user should see \"invalid account\"\n");
            return;
        }

        if (
                scenario.contains("blank amount")
        ) {

            feature.append("    Then user should see \"required field error\"\n");
            return;
        }

        feature.append("    Then user should see \"Bill Payment Complete\"\n");
    }

    private void appendFindTransactionSteps(
            StringBuilder feature,
            String scenario
    ) {

        appendLoggedInStart(feature);
        feature.append("    And user clicks \"Find Transactions\"\n");

        if (
                scenario.contains("transaction id")
        ) {

            feature.append("    And user enters \"${transactionId}\" into \"transaction id\"\n");

        } else if (
                scenario.contains("date range")
        ) {

            feature.append("    And user enters \"2026-01-01\" into \"from date\"\n")
                    .append("    And user enters \"2026-12-31\" into \"to date\"\n");

        } else if (
                scenario.contains("no matching")
                        ||
                        scenario.contains("no-match")
        ) {

            feature.append("    And user enters \"999999\" into \"amount\"\n");

        } else {

            feature.append("    And user enters \"50\" into \"amount\"\n");
        }

        feature.append("    And user clicks \"find transactions button\"\n");

        if (
                scenario.contains("no matching")
                        ||
                        scenario.contains("no-match")
        ) {

            feature.append("    Then user should see \"no matching transactions\"\n");
            return;
        }

        feature.append("    Then user should see \"matching transactions\"\n");
    }

    private String normalizeStoryId(
            String id
    ) {

        return safe(id)
                .toUpperCase(Locale.ROOT)
                .replace("_", "-");
    }

    private String numberedStoryId(
            String number
    ) {

        String cleaned =
                safe(number)
                        .replaceAll("[^0-9]", "");

        if (
                cleaned.isBlank()
        ) {

            return "US-000";
        }

        try {
            return "US-%03d".formatted(
                    Integer.parseInt(cleaned)
            );

        } catch (
                NumberFormatException ignored
        ) {

            return "US-" + cleaned;
        }
    }

    private boolean containsAny(
            String value,
            String... needles
    ) {

        String lower =
                safe(value)
                        .toLowerCase(Locale.ROOT);

        for (
                String needle
                : needles
        ) {

            if (
                    lower.contains(
                            safe(needle)
                                    .toLowerCase(Locale.ROOT)
                    )
            ) {

                return true;
            }
        }

        return false;
    }

    private String tag(
            String value
    ) {

        String tag =
                safe(value)
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", "_")
                        .replaceAll("^_+|_+$", "");

        if (
                tag.isBlank()
        ) {

            return "requirement";
        }

        if (
                Character.isDigit(
                        tag.charAt(0)
                )
        ) {

            return "tag_" + tag;
        }

        return tag;
    }

    private String concise(
            String value
    ) {

        String cleaned =
                safe(value)
                        .replaceAll("\\s+", " ")
                        .trim();

        if (
                cleaned.length() <= 70
        ) {

            return cleaned;
        }

        return cleaned.substring(0, 70)
                .trim();
    }

    private String toTitle(
            String value
    ) {

        String[] parts =
                safe(value)
                        .replaceAll("[^A-Za-z0-9]+", " ")
                        .trim()
                        .split("\\s+");

        StringBuilder title =
                new StringBuilder();

        for (
                String part
                : parts
        ) {

            if (
                    part.isBlank()
            ) {

                continue;
            }

            if (
                    !title.isEmpty()
            ) {

                title.append(" ");
            }

            title.append(
                    part.substring(0, 1)
                            .toUpperCase(Locale.ROOT)
            );

            if (
                    part.length() > 1
            ) {

                title.append(
                        part.substring(1)
                                .toLowerCase(Locale.ROOT)
                );
            }
        }

        return title.isEmpty()
                ? "Requirements"
                : title.toString();
    }

    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }

    record Analysis(
            List<RequirementTestCase> testCases,
            String featureFile
    ) {

        static Analysis empty() {

            return new Analysis(
                    List.of(),
                    ""
            );
        }

        boolean hasTestCases() {

            return testCases != null
                    &&
                    !testCases.isEmpty()
                    &&
                    featureFile != null
                    &&
                    !featureFile.isBlank();
        }
    }

    private record RequirementStory(
            String id,
            String title,
            String content,
            List<String> criteria,
            boolean narrativeStory
    ) {

        private String searchText() {

            String joinedCriteria =
                    criteria == null
                            ? ""
                            : String.join(
                                    " ",
                                    criteria
                            );

            return (
                    id
                            + " "
                            + title
                            + " "
                            + content
                            + " "
                            + joinedCriteria
            )
                    .toLowerCase(Locale.ROOT);
        }
    }

    private record StoryMatch(
            String id,
            int contentStart,
            int start,
            boolean narrativeStory
    ) {
    }
}
