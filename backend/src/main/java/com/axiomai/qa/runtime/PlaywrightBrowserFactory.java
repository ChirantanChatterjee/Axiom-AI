package com.axiomai.qa.runtime;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;

import java.util.List;

public final class PlaywrightBrowserFactory {

    private static final List<String> VISIBLE_BROWSER_ARGS =
            List.of(
                    "--incognito",
                    "--start-maximized",
                    "--start-fullscreen",
                    "--no-first-run",
                    "--no-default-browser-check",
                    "--no-sandbox",
                    "--disable-dev-shm-usage"
            );

    private PlaywrightBrowserFactory() {
    }

    public static Browser launchVisibleChromium(
            Playwright playwright
    ) {

        boolean headless =
                resolveHeadless();

        String browserChannel =
                normalize(
                        System.getenv("AIF_BROWSER_CHANNEL")
                );

        BrowserType.LaunchOptions launchOptions =
                launchOptions(
                        headless,
                        browserChannel
                );

        try {

            return playwright.chromium()
                    .launch(launchOptions);

        } catch (PlaywrightException exception) {

            if (
                    browserChannel == null
                            ||
                            !isMissingChannelFailure(exception)
            ) {

                throw exception;
            }

            return playwright.chromium()
                    .launch(
                            launchOptions(
                                    headless,
                                    null
                            )
                    );
        }
    }

    static BrowserType.LaunchOptions launchOptions(
            boolean headless,
            String browserChannel
    ) {

        BrowserType.LaunchOptions launchOptions =
                new BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setTimeout(resolveLaunchTimeout())
                        .setArgs(VISIBLE_BROWSER_ARGS);

        if (
                browserChannel != null
        ) {

            launchOptions.setChannel(browserChannel);
        }

        return launchOptions;
    }

    static boolean resolveHeadless() {

        String configured =
                normalize(
                        System.getenv("AIF_HEADLESS")
                );

        if (
                configured != null
        ) {

            return Boolean.parseBoolean(configured);
        }

        return isHostedLinuxWithoutDisplay();
    }

    private static double resolveLaunchTimeout() {

        String configured =
                normalize(
                        System.getenv("AIF_BROWSER_LAUNCH_TIMEOUT_MS")
                );

        if (
                configured == null
        ) {

            return 60000;
        }

        try {

            return Double.parseDouble(configured);

        } catch (NumberFormatException ignored) {

            return 60000;
        }
    }

    private static boolean isHostedLinuxWithoutDisplay() {

        String osName =
                System.getProperty("os.name", "")
                        .toLowerCase();

        return osName.contains("linux")
                &&
                (
                        normalize(System.getenv("DISPLAY")) == null
                                ||
                                isTruthy(System.getenv("RENDER"))
                                ||
                                isTruthy(System.getenv("CI"))
                );
    }

    private static boolean isTruthy(
            String value
    ) {

        String normalized =
                normalize(value);

        return normalized != null
                &&
                !"false".equalsIgnoreCase(normalized)
                &&
                !"0".equals(normalized);
    }

    private static boolean isMissingChannelFailure(
            PlaywrightException exception
    ) {

        String message =
                exception.getMessage() == null
                        ? ""
                        : exception.getMessage()
                        .toLowerCase();

        return message.contains("distribution")
                &&
                message.contains("is not found");
    }

    private static String normalize(
            String value
    ) {

        if (
                value == null
                        ||
                        value.isBlank()
        ) {

            return null;
        }

        return value.trim();
    }
}
