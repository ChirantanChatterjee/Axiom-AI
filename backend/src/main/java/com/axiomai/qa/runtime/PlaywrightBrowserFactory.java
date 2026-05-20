package com.axiomai.qa.runtime;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

import java.util.List;

public final class PlaywrightBrowserFactory {

    private static final List<String> VISIBLE_BROWSER_ARGS =
            List.of(
                    "--incognito",
                    "--start-maximized",
                    "--start-fullscreen",
                    "--no-first-run",
                    "--no-default-browser-check"
            );

    private PlaywrightBrowserFactory() {
    }

    public static Browser launchVisibleChromium(
            Playwright playwright
    ) {

        return playwright.chromium()
                .launch(
                        new BrowserType.LaunchOptions()
                                .setHeadless(false)
                                .setArgs(VISIBLE_BROWSER_ARGS)
                );
    }
}
