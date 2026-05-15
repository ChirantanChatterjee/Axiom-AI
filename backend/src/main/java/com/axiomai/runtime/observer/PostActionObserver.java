package com.axiomai.runtime.observer;

import com.microsoft.playwright.Page;

public class PostActionObserver {

    public void observe(Page page) {

        try {

            String content =
                    page.content().toLowerCase();

            detectValidationErrors(content);

            detectAuthenticationFailure(content);

            detectServerErrors(content);

            detectUnexpectedModal(content);

        } catch (Exception e) {

            System.out.println(
                    "[POST ACTION OBSERVER] FAILED");
        }
    }

    private void detectValidationErrors(
            String content
    ) {

        if(content.contains("required")) {

            System.out.println(
                    "[POST OBSERVER] VALIDATION ERROR DETECTED");
        }

        if(content.contains("invalid")) {

            System.out.println(
                    "[POST OBSERVER] INVALID INPUT DETECTED");
        }
    }

    private void detectAuthenticationFailure(
            String content
    ) {

        if(content.contains("wrong password")
                || content.contains("incorrect password")
                || content.contains("try again")
        ) {

            throw new RuntimeException(
                    "Authentication failed");
        }
    }

    private void detectServerErrors(
            String content
    ) {

        if(content.contains("500")
                || content.contains("internal server error")
        ) {

            throw new RuntimeException(
                    "Server error detected");
        }
    }

    private void detectUnexpectedModal(
            String content
    ) {

        if(content.contains("subscribe")
                || content.contains("newsletter")
        ) {

            System.out.println(
                    "[POST OBSERVER] MODAL DETECTED");
        }
    }
}