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
                safe(element.getTag());

        String text =
                safe(element.getText());

        String type =
                safe(element.getType());

        String name =
                safe(element.getName());

        String placeholder =
                safe(element.getPlaceholder());

        String combined = (

                tag + " "
                        + text + " "
                        + type + " "
                        + name + " "
                        + placeholder

        ).toLowerCase();

        // =================================================
        // AUTH FIELD
        // =================================================

        if (

                combined.contains("username")
                        ||
                        combined.contains("email")
                        ||
                        combined.contains("user")
                        ||

                        combined.contains("用户名")
                        ||
                        combined.contains("邮箱")
                        ||
                        combined.contains("账号")

        ) {

            element.setBusinessRole(
                    "AUTH_FIELD"
            );

            element.setImportanceScore(95);

            return;
        }

        // =================================================
        // PASSWORD FIELD
        // =================================================

        if (

                combined.contains("password")
                        ||
                        type.equalsIgnoreCase("password")
                        ||

                        combined.contains("密码")

        ) {

            element.setBusinessRole(
                    "PASSWORD_FIELD"
            );

            element.setImportanceScore(95);

            return;
        }

        // =================================================
        // LOGIN BUTTON
        // =================================================

        if (

                combined.contains("login")
                        ||
                        combined.contains("sign in")
                        ||
                        combined.contains("signin")

                        ||

                        combined.contains("登录")

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

                combined.contains("search")
                        ||
                        combined.contains("find")

        ) {

            element.setBusinessRole(
                    "SEARCH_FIELD"
            );

            element.setImportanceScore(80);

            return;
        }

        // =================================================
        // SEARCH BUTTON
        // =================================================

        if (

                tag.equals("button")
                        &&
                        combined.contains("search")

        ) {

            element.setBusinessRole(
                    "SEARCH_BUTTON"
            );

            element.setImportanceScore(85);

            return;
        }

        // =================================================
        // TEXT INPUT
        // =================================================

        if (

                tag.equals("input")
                        ||
                        tag.equals("textarea")

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

                tag.equals("button")

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