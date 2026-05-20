package com.axiomai.qa.ai;

import com.axiomai.qa.models.PageElement;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component

public class LocalSemanticResolver {

    // =====================================================
    // MAIN RESOLUTION
    // =====================================================

    public ElementSemanticMatch resolve(

            String userIntent,
            List<PageElement> elements

    ) {

        try {

            if (

                    userIntent == null
                            ||
                            elements == null
                            ||
                            elements.isEmpty()

            ) {

                return null;
            }

            System.out.println(
                    "[SEMANTIC TARGET] "
                            + userIntent
            );

            String normalizedIntent =
                    normalize(userIntent);

            List<ScoredElement> scored =
                    new ArrayList<>();

            for (PageElement element : elements) {

                // =================================================
                // STRICT FILTERING
                // =================================================

                if (

                        shouldRejectElement(
                                normalizedIntent,
                                element
                        )

                ) {

                    continue;
                }

                // =================================================
                // SCORE
                // =================================================

                double score =
                        calculateScore(
                                normalizedIntent,
                                element
                        );

                if (score > 0) {

                    scored.add(

                            new ScoredElement(
                                    element,
                                    score
                            )

                    );
                }
            }

            if (scored.isEmpty()) {

                System.out.println(
                        "[LOCAL SEMANTIC] No candidates found."
                );

                return null;
            }

            // =================================================
            // SORT BY SCORE
            // =================================================

            scored.sort(
                    Comparator.comparingDouble(
                            ScoredElement::getScore
                    ).reversed()
            );

            // =================================================
            // PICK BEST
            // =================================================

            ScoredElement best =
                    scored.get(0);

            System.out.println(
                    "[LOCAL SEMANTIC MATCH] "
                            + best.getElement()
                            .getBestSelector()
            );

            System.out.println(
                    "[LOCAL SEMANTIC SCORE] "
                            + best.getScore()
            );

            return ElementSemanticMatch.builder()

                    .selector(
                            best.getElement()
                                    .getBestSelector()
                    )

                    .confidence(
                            best.getScore()
                    )

                    .reasoning(
                            "Resolved using local semantic scoring"
                    )

                    .source(
                            "LOCAL"
                    )

                    .businessRole(
                            best.getElement()
                                    .getBusinessRole()
                    )

                    .semanticTarget(
                            userIntent
                    )

                    .build();

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }
    }

    // =====================================================
    // SCORE ENGINE
    // =====================================================

    private double calculateScore(

            String intent,
            PageElement element

    ) {

        double score = 0;

        String text =
                normalize(
                        element.getText()
                );

        String placeholder =
                normalize(
                        element.getPlaceholder()
                );

        String aria =
                normalize(
                        element.getAriaLabel()
                );

        String role =
                normalize(
                        element.getBusinessRole()
                );

        String type =
                normalize(
                        element.getType()
                );

        String selector =
                normalize(
                        element.getBestSelector()
                );

        // =================================================
        // SIGN IN BUTTON
        // =================================================

        if (

                intent.contains("sign_in_button")
                        ||
                        intent.contains("login_button")

        ) {

            if (
                    text.contains("sign in")
            ) {
                score += 0.95;
            }

            if (
                    text.contains("login")
            ) {
                score += 0.90;
            }

            if (
                    aria.contains("sign in")
            ) {
                score += 0.85;
            }

            if (
                    role.contains("sign_in_button")
            ) {
                score += 0.95;
            }

            if (

                    selector.contains("signin")
                            ||
                            selector.contains("login")
                            ||
                            selector.contains("sign-in")

            ) {

                score += 1.0;
            }
        }

        // =================================================
        // SEARCH BOX
        // =================================================

        if (

                intent.contains("search_box")

        ) {

            if (
                    placeholder.contains("search")
            ) {
                score += 0.95;
            }

            if (
                    aria.contains("search")
            ) {
                score += 0.90;
            }

            if (
                    role.contains("search_box")
            ) {
                score += 0.95;
            }

            if (
                    type.contains("search")
            ) {
                score += 0.80;
            }

            if (
                    text.contains("search")
            ) {
                score += 0.85;
            }

            if (

                    selector.contains("search")
                            ||
                            selector.contains("query")

            ) {

                score += 1.0;
            }
        }

        // =================================================
        // USERNAME FIELD
        // =================================================

        if (

                isUsernameIntent(intent)

        ) {

            if (
                    placeholder.contains("username")
            ) {
                score += 1.0;
            }

            if (
                    placeholder.contains("user name")
            ) {
                score += 1.0;
            }

            if (
                    placeholder.contains("user-name")
            ) {
                score += 1.0;
            }

            if (
                    placeholder.contains("email")
            ) {
                score += 0.90;
            }

            if (
                    placeholder.contains("phone")
            ) {
                score += 0.85;
            }

            if (
                    aria.contains("email")
            ) {
                score += 0.85;
            }

            if (
                    aria.contains("username")
            ) {
                score += 0.85;
            }

            if (
                    role.contains("username_field")
                            ||
                            role.contains("auth_field")
            ) {
                score += 0.95;
            }

            if (
                    selector.contains("email")
            ) {
                score += 1.0;
            }

            if (
                    selector.contains("username")
            ) {
                score += 1.0;
            }

            if (
                    selector.contains("user-name")
            ) {
                score += 1.0;
            }

            if (
                    selector.contains("user")
            ) {
                score += 0.75;
            }

            if (
                    selector.contains("identifier")
            ) {
                score += 1.0;
            }

            if (
                    type.contains("email")
            ) {
                score += 1.0;
            }

            if (
                    type.contains("text")
            ) {
                score += 0.50;
            }

            if (
                    element.isVisible()
            ) {
                score += 0.75;
            }
        }

        // =================================================
        // PASSWORD FIELD
        // =================================================

        if (

                intent.contains("password_field")

        ) {

            if (
                    type.contains("password")
            ) {
                score += 2.0;
            }

            if (
                    placeholder.contains("password")
            ) {
                score += 1.0;
            }

            if (
                    aria.contains("password")
            ) {
                score += 1.0;
            }

            if (
                    role.contains("password_field")
            ) {
                score += 1.5;
            }

            if (
                    text.contains("password")
            ) {
                score += 0.80;
            }

            if (
                    selector.contains("password")
            ) {
                score += 1.5;
            }

            // =============================================
            // GOOGLE PASSWORD FIELD BOOST
            // =============================================

            if (
                    selector.contains("passwd")
            ) {
                score += 2.5;
            }

            // =============================================
            // VISIBLE BONUS
            // =============================================

            if (
                    element.isVisible()
            ) {
                score += 3.0;
            }
        }

        // =================================================
        // SEARCH BUTTON
        // =================================================

        if (

                intent.contains("search_button")

        ) {

            if (
                    text.contains("search")
            ) {
                score += 0.95;
            }

            if (
                    aria.contains("search")
            ) {
                score += 0.90;
            }

            if (
                    role.contains("search_button")
            ) {
                score += 0.95;
            }

            if (

                    selector.contains("search")
                            ||
                            selector.contains("icon-search")

            ) {

                score += 1.0;
            }
        }

        // =================================================
        // VIDEO CARD
        // =================================================

        if (

                intent.contains("first_video")

        ) {

            if (
                    role.contains("video_card")
            ) {
                score += 0.95;
            }

            if (

                    selector.contains("video")
                            ||
                            selector.contains("thumbnail")
                            ||
                            selector.contains("ytd")

            ) {

                score += 1.0;
            }
        }

        // =================================================
        // GENERAL VISIBILITY BONUS
        // =================================================

        if (
                element.isVisible()
        ) {

            score += 0.25;
        }

        // =================================================
        // SELECTOR BONUS
        // =================================================

        if (

                element.getBestSelector() != null
                        &&
                        !element.getBestSelector().isBlank()

        ) {

            score += 0.10;
        }

        return score;
    }

    // =====================================================
    // STRICT FILTERING
    // =====================================================

    private boolean shouldRejectElement(

            String intent,
            PageElement element

    ) {

        if (element == null) {
            return true;
        }

        String type =
                normalize(
                        element.getType()
                );

        String selector =
                normalize(
                        element.getBestSelector()
                );

        String role =
                normalize(
                        element.getBusinessRole()
                );

        String text =
                normalize(
                        element.getText()
                );

        String placeholder =
                normalize(
                        element.getPlaceholder()
                );

        String aria =
                normalize(
                        element.getAriaLabel()
                );

        // =================================================
        // GLOBAL HIDDEN ELEMENT REJECTION
        // =================================================

        if (

                selector.contains("hidden")
                        ||
                        selector.contains("aria-hidden")
                        ||
                        selector.contains("display:none")
                        ||
                        selector.contains("visibility:hidden")

        ) {

            return true;
        }

        // =================================================
        // REJECT NON-VISIBLE PASSWORD FIELDS
        // =================================================

        if (

                intent.contains("password_field")
                        &&
                        !element.isVisible()

        ) {

            return true;
        }

        // ============================================
        // PASSWORD FIELD
        // ============================================

        if (
                isUsernameIntent(intent)
        ) {

            return !(

                    type.contains("email")
                            ||

                            type.contains("text")
                            ||

                            type.contains("tel")
                            ||

                            selector.contains("email")
                            ||

                            selector.contains("username")
                            ||

                            selector.contains("user-name")
                            ||

                            selector.contains("identifier")
                            ||

                            selector.contains("phone")
                            ||

                            placeholder.contains("username")
                            ||

                            placeholder.contains("user name")
                            ||

                            placeholder.contains("email")
                            ||

                            placeholder.contains("phone")
                            ||

                            role.contains("username")
                            ||

                            role.contains("auth")
                            ||

                            aria.contains("email")
                            ||

                            aria.contains("username")
                            ||

                            aria.contains("phone")

            );
        }

        if (
                intent.contains("password_field")
        ) {

            return !(

                    type.contains("password")
                            ||

                            selector.contains("password")
                            ||

                            selector.contains("passwd")
                            ||

                            role.contains("password")
                            ||

                            text.contains("password")
                            ||

                            aria.contains("password")

            );
        }

        // ============================================
        // SEARCH BOX
        // ============================================

        if (
                intent.contains("search_box")
        ) {

            return !(

                    type.contains("search")
                            ||

                            selector.contains("search")
                            ||

                            selector.contains("query")
                            ||

                            role.contains("search")
                            ||

                            text.contains("search")
                            ||

                            aria.contains("search")

            );
        }

        // ============================================
        // LOGIN BUTTON
        // ============================================

        if (

                intent.contains("login_button")
                        ||
                        intent.contains("sign_in_button")

        ) {

            return !(

                    selector.contains("login")
                            ||

                            selector.contains("signin")
                            ||

                            selector.contains("sign-in")
                            ||

                            role.contains("login")
                            ||

                            text.contains("sign in")
                            ||

                            text.contains("login")
                            ||

                            aria.contains("sign in")
                            ||

                            aria.contains("login")

            );
        }

        // ============================================
        // SEARCH BUTTON
        // ============================================

        if (
                intent.contains("search_button")
        ) {

            return !(

                    selector.contains("search")
                            ||

                            role.contains("search")
                            ||

                            text.contains("search")
                            ||

                            aria.contains("search")

            );
        }

        return false;
    }

    // =====================================================
    // NORMALIZE
    // =====================================================

    private String normalize(
            String value
    ) {

        return value == null
                ? ""
                : value
                .trim()
                .toLowerCase();
    }

    private boolean isUsernameIntent(
            String intent
    ) {

        if (
                intent == null
        ) {

            return false;
        }

        return intent.contains("username")
                ||
                intent.contains("auth_field")
                ||
                intent.equals("auth")
                ||
                intent.equals("email")
                ||
                intent.contains("email_field");
    }

    // =====================================================
    // SCORED ELEMENT
    // =====================================================

    private static class ScoredElement {

        private final PageElement element;

        private final double score;

        public ScoredElement(

                PageElement element,
                double score

        ) {

            this.element = element;
            this.score = score;
        }

        public PageElement getElement() {
            return element;
        }

        public double getScore() {
            return score;
        }
    }
}
