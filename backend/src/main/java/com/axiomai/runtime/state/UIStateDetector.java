package com.axiomai.runtime.state;

import com.microsoft.playwright.Page;

public class UIStateDetector {

    public UIState detect(Page page) {

        String content =
                page.content().toLowerCase();

        if(content.contains("enter your password")) {

            return UIState.PASSWORD_SCREEN;
        }

        if(content.contains("sign in")) {

            return UIState.LOGIN_SCREEN;
        }

        if(content.contains("dashboard")) {

            return UIState.DASHBOARD;
        }

        return UIState.UNKNOWN;
    }
}