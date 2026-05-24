package com.axiomai.qa.util;

import com.axiomai.qa.models.PageElement;

public class ElementClassifier {

    // =====================================================
    // MAIN CLASSIFIER
    // =====================================================

    public static void classify(
            PageElement element
    ) {

        String tag =
                safe(element.getTag())
                        .toLowerCase();

        String text =
                safe(element.getText());

        String type =
                safe(element.getType())
                        .toLowerCase();

        String id =
                safe(element.getId());

        String name =
                safe(element.getName());

        String placeholder =
                safe(element.getPlaceholder());

        String ariaLabel =
                safe(element.getAriaLabel());

        String dataTestId =
                safe(element.getDataTestId());

        String combined = (

                tag + " "
                        + text + " "
                        + type + " "
                        + id + " "
                        + name + " "
                        + placeholder + " "
                        + ariaLabel + " "
                        + dataTestId

        ).toLowerCase();

        String actionDescriptor = (

                text + " "
                        + id + " "
                        + name + " "
                        + placeholder + " "
                        + ariaLabel + " "
                        + dataTestId

        ).toLowerCase();

        boolean inputLike =
                tag.equals("input")
                        ||
                        tag.equals("textarea")
                        ||
                        tag.equals("select");

        boolean buttonControl =
                tag.equals("button")
                        ||
                        (
                                tag.equals("input")
                                        &&
                                        (
                                                type.equals("submit")
                                                        ||
                                                        type.equals("button")
                                        )
                        );

        boolean actionLike =
                buttonControl
                        ||
                        tag.equals("a");

        // =================================================
        // PASSWORD FIELD
        // =================================================

        if (

                inputLike
                        &&
                        (
                                combined.contains("password")
                                        ||
                                        type.equals("password")
                        )

        ) {

            element.setBusinessRole(
                    "PASSWORD_FIELD"
            );

            element.setImportanceScore(95);

            return;
        }

        // =================================================
        // AUTH FIELD
        // =================================================

        if (

                inputLike
                        &&
                        (
                                combined.contains("username")
                                        ||
                                        combined.contains("email")
                                        ||
                                        combined.contains("user")
                        )

        ) {

            element.setBusinessRole(
                    "AUTH_FIELD"
            );

            element.setImportanceScore(95);

            return;
        }

        // =================================================
        // SEARCH BUTTON
        // =================================================

        if (

                actionLike
                        &&
                        actionDescriptor.contains("search")

        ) {

            element.setBusinessRole(
                    "SEARCH_BUTTON"
            );

            element.setImportanceScore(85);

            return;
        }

        // =================================================
        // NEXT / CONTINUE BUTTON
        // =================================================

        if (

                actionLike
                        &&
                        (
                                actionDescriptor.contains("next")
                                        ||
                                        actionDescriptor.contains("continue")
                        )

        ) {

            element.setBusinessRole(
                    "NEXT_BUTTON"
            );

            element.setImportanceScore(90);

            return;
        }

        // =================================================
        // LOGIN BUTTON
        // =================================================

        if (

                actionLike
                        &&
                        (
                                actionDescriptor.contains("login")
                                        ||
                                        actionDescriptor.contains("log in")
                                        ||
                                        actionDescriptor.contains("sign in")
                                        ||
                                        actionDescriptor.contains("signin")
                        )

        ) {

            element.setBusinessRole(
                    "LOGIN_BUTTON"
            );

            element.setImportanceScore(100);

            return;
        }

        // =================================================
        // SEARCH FIELD
        // =================================================

        if (

                inputLike
                        &&
                        (
                                combined.contains("search")
                                        ||
                                        combined.contains("find")
                        )

        ) {

            element.setBusinessRole(
                    "SEARCH_FIELD"
            );

            element.setImportanceScore(80);

            return;
        }

        // =================================================
        // TEXT INPUT
        // =================================================

        if (

                inputLike
                        &&
                        !type.equals("hidden")

        ) {

            element.setBusinessRole(
                    "TEXT_INPUT"
            );

            element.setImportanceScore(60);

            return;
        }

        // =================================================
        // PRIMARY BUTTON
        // =================================================

        if (

                buttonControl

        ) {

            element.setBusinessRole(
                    "PRIMARY_ACTION_BUTTON"
            );

            element.setImportanceScore(70);

            return;
        }

        // =================================================
        // LINK
        // =================================================

        if (

                tag.equals("a")

        ) {

            element.setBusinessRole(
                    "NAVIGATION_LINK"
            );

            element.setImportanceScore(40);

            return;
        }

        // =================================================
        // UNKNOWN
        // =================================================

        element.setBusinessRole(
                "UNKNOWN"
        );

        element.setImportanceScore(10);
    }

    // =====================================================
    // SAFE
    // =====================================================

    private static String safe(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }
}
