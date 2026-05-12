package com.axiomai.qa.util;

import com.axiomai.qa.models.PageElement;

public class ElementClassifier {

    // =====================================================
    // MAIN CLASSIFIER
    // =====================================================

    public static void classify(PageElement element) {

        if (element == null) {
            return;
        }

        String tag =
                safe(element.getTag()).toLowerCase();

        String text =
                safe(element.getText()).toLowerCase();

        String id =
                safe(element.getId()).toLowerCase();

        String name =
                safe(element.getName()).toLowerCase();

        String type =
                safe(element.getType()).toLowerCase();

        String placeholder =
                safe(element.getPlaceholder()).toLowerCase();

        String css =
                safe(element.getCssSelector()).toLowerCase();

        String combined =
                (
                        text + " " +
                                id + " " +
                                name + " " +
                                placeholder + " " +
                                css
                ).toLowerCase();

        // =====================================================
        // DEFAULT VALUES
        // =====================================================

        element.setBusinessRole("UNKNOWN");

        element.setRecommendedAction("NONE");

        element.setImportanceScore(20);

        element.setTestCandidate(false);

        // =====================================================
        // HIDDEN INPUTS
        // =====================================================

        if (
                type.equals("hidden")
                        ||
                        name.contains("token")
                        ||
                        name.contains("_csrf")
                        ||
                        id.contains("_csrf")
        ) {

            element.setBusinessRole("HIDDEN_FIELD");

            element.setRecommendedAction("IGNORE");

            element.setImportanceScore(0);

            element.setTestCandidate(false);

            return;
        }

        // =====================================================
        // PASSWORD FIELD
        // =====================================================

        if (
                type.equals("password")
                        ||
                        combined.contains("password")
                        ||
                        combined.contains("passwd")
                        ||
                        combined.contains("pwd")
        ) {

            element.setBusinessRole("PASSWORD_FIELD");

            element.setRecommendedAction("TYPE");

            element.setImportanceScore(100);

            element.setTestCandidate(true);

            return;
        }

        // =====================================================
        // AUTH FIELD
        // =====================================================

        if (
                combined.contains("username")
                        ||
                        combined.contains("email")
                        ||
                        combined.contains("login")
                        ||
                        combined.contains("signin")
                        ||
                        combined.contains("user")
        ) {

            if (
                    tag.equals("input")
                            ||
                            tag.equals("textarea")
            ) {

                element.setBusinessRole("AUTH_FIELD");

                element.setRecommendedAction("TYPE");

                element.setImportanceScore(95);

                element.setTestCandidate(true);

                return;
            }
        }

        // =====================================================
        // SEARCH FIELD
        // =====================================================

        if (
                combined.contains("search")
                        ||
                        combined.contains("query")
                        ||
                        name.equals("q")
                        ||
                        placeholder.contains("search")
        ) {

            if (
                    tag.equals("input")
                            ||
                            tag.equals("textarea")
            ) {

                element.setBusinessRole("SEARCH_FIELD");

                element.setRecommendedAction("TYPE");

                element.setImportanceScore(90);

                element.setTestCandidate(true);

                return;
            }
        }

        // =====================================================
        // SEARCH BUTTON
        // =====================================================

        if (
                combined.contains("search")
                        ||
                        text.contains("search")
                        ||
                        name.equals("btnk")
                        ||
                        combined.contains("google search")
        ) {

            if (
                    type.equals("submit")
                            ||
                            tag.equals("button")
                            ||
                            tag.equals("input")
            ) {

                element.setBusinessRole("SEARCH_BUTTON");

                element.setRecommendedAction("CLICK");

                element.setImportanceScore(90);

                element.setTestCandidate(true);

                return;
            }
        }

        // =====================================================
        // LOGIN BUTTON
        // =====================================================

        if (
                combined.contains("login")
                        ||
                        combined.contains("sign in")
                        ||
                        combined.contains("signin")
                        ||
                        combined.contains("log in")
        ) {

            element.setBusinessRole("LOGIN_BUTTON");

            element.setRecommendedAction("CLICK");

            element.setImportanceScore(100);

            element.setTestCandidate(true);

            return;
        }

        // =====================================================
        // LOGOUT BUTTON
        // =====================================================

        if (
                combined.contains("logout")
                        ||
                        combined.contains("sign out")
                        ||
                        combined.contains("log out")
        ) {

            element.setBusinessRole("LOGOUT_BUTTON");

            element.setRecommendedAction("CLICK");

            element.setImportanceScore(95);

            element.setTestCandidate(true);

            return;
        }

        // =====================================================
        // COOKIE BUTTON
        // =====================================================

        if (
                combined.contains("accept all")
                        ||
                        combined.contains("reject all")
                        ||
                        combined.contains("cookies")
                        ||
                        combined.contains("cookie")
        ) {

            element.setBusinessRole("COOKIE_BUTTON");

            element.setRecommendedAction("CLICK");

            element.setImportanceScore(80);

            element.setTestCandidate(true);

            return;
        }

        // =====================================================
        // PRIMARY ACTION BUTTON
        // =====================================================

        if (
                combined.contains("submit")
                        ||
                        combined.contains("continue")
                        ||
                        combined.contains("next")
                        ||
                        combined.contains("save")
                        ||
                        combined.contains("confirm")
                        ||
                        combined.contains("checkout")
        ) {

            if (
                    tag.equals("button")
                            ||
                            type.equals("submit")
            ) {

                element.setBusinessRole("PRIMARY_ACTION_BUTTON");

                element.setRecommendedAction("CLICK");

                element.setImportanceScore(90);

                element.setTestCandidate(true);

                return;
            }
        }

        // =====================================================
        // DANGER BUTTON
        // =====================================================

        if (
                combined.contains("delete")
                        ||
                        combined.contains("remove")
                        ||
                        combined.contains("cancel")
        ) {

            element.setBusinessRole("DANGER_BUTTON");

            element.setRecommendedAction("CLICK");

            element.setImportanceScore(85);

            element.setTestCandidate(true);

            return;
        }

        // =====================================================
        // DROPDOWN
        // =====================================================

        if (tag.equals("select")) {

            element.setBusinessRole("DROPDOWN");

            element.setRecommendedAction("SELECT");

            element.setImportanceScore(80);

            element.setTestCandidate(true);

            return;
        }

        // =====================================================
        // CHECKBOX
        // =====================================================

        if (type.equals("checkbox")) {

            element.setBusinessRole("CHECKBOX");

            element.setRecommendedAction("CLICK");

            element.setImportanceScore(70);

            element.setTestCandidate(true);

            return;
        }

        // =====================================================
        // RADIO BUTTON
        // =====================================================

        if (type.equals("radio")) {

            element.setBusinessRole("RADIO_BUTTON");

            element.setRecommendedAction("CLICK");

            element.setImportanceScore(70);

            element.setTestCandidate(true);

            return;
        }

        // =====================================================
        // TEXT INPUT
        // =====================================================

        if (
                tag.equals("input")
                        ||
                        tag.equals("textarea")
        ) {

            if (
                    type.equals("text")
                            ||
                            type.isBlank()
                            ||
                            type.equals("email")
                            ||
                            type.equals("number")
            ) {

                element.setBusinessRole("TEXT_INPUT");

                element.setRecommendedAction("TYPE");

                element.setImportanceScore(75);

                element.setTestCandidate(true);

                return;
            }
        }

        // =====================================================
        // BUTTON
        // =====================================================

        if (
                tag.equals("button")
                        ||
                        type.equals("button")
                        ||
                        type.equals("submit")
        ) {

            element.setBusinessRole("BUTTON");

            element.setRecommendedAction("CLICK");

            element.setImportanceScore(70);

            element.setTestCandidate(true);

            return;
        }

        // =====================================================
        // NAVIGATION LINK
        // =====================================================

        if (tag.equals("a")) {

            element.setBusinessRole("NAVIGATION_LINK");

            element.setRecommendedAction("CLICK");

            element.setImportanceScore(40);

            element.setTestCandidate(false);

            return;
        }
    }

    // =====================================================
    // SAFE STRING
    // =====================================================

    private static String safe(String value) {

        return value == null ? "" : value;
    }
}