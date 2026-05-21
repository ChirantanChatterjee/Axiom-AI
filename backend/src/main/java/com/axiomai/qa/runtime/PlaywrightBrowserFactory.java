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

        boolean headless =
                Boolean.parseBoolean(
                        System.getenv()
                                .getOrDefault("AIF_HEADLESS", "false")
                );

        String browserChannel =
                System.getenv()
                        .getOrDefault("AIF_BROWSER_CHANNEL", "chrome");

        BrowserType.LaunchOptions launchOptions =
                new BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setTimeout(15000)
                        .setArgs(VISIBLE_BROWSER_ARGS);

        if (
                browserChannel != null
                        &&
                        !browserChannel.isBlank()
        ) {

            launchOptions.setChannel(browserChannel);
        }

        return playwright.chromium()
                .launch(launchOptions);
    }
}
