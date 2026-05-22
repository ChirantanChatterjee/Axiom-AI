package com.axiomai.qa.service;

import org.springframework.stereotype.Service;

@Service
public class HookGeneratorService {

    public String generateHooks() {

        return """
package com.axiomai.generated.hooks;

import com.microsoft.playwright.*;

import io.cucumber.java.*;


public class Hooks {

    public static Playwright playwright;

    public static Browser browser;

    public static BrowserContext context;

    public static Page page;

    @Before
    public void setup() {

        System.out.println("AIF HOOK setup start");

        playwright = Playwright.create();

        boolean headless =
                resolveHeadless();

        String browserChannel =
                normalize(
                        System.getenv("AIF_BROWSER_CHANNEL")
                );

        BrowserType.LaunchOptions launchOptions =
                new BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setTimeout(15000)
                        .setArgs(java.util.Arrays.asList(
                                "--incognito",
                                "--no-first-run",
                                "--no-default-browser-check",
                                "--no-sandbox",
                                "--disable-setuid-sandbox",
                                "--disable-gpu",
                                "--disable-dev-shm-usage"
                        ));

        if (browserChannel != null && !browserChannel.isBlank()) {
            launchOptions.setChannel(browserChannel);
        }

        browser =
                launchChromium(
                        launchOptions,
                        browserChannel
                );

        context = browser.newContext(
                new Browser.NewContextOptions()
                        .setViewportSize(1440, 1000)
        );

        context.setDefaultTimeout(runtimeTimeoutMs());
        context.setDefaultNavigationTimeout(runtimeNavigationTimeoutMs());

        page = context.newPage();
        page.setDefaultTimeout(runtimeTimeoutMs());
        page.setDefaultNavigationTimeout(runtimeNavigationTimeoutMs());

        System.out.println("AIF HOOK setup complete");
    }

    private Browser launchChromium(
            BrowserType.LaunchOptions launchOptions,
            String browserChannel
    ) {

        try {
            return playwright.chromium()
                    .launch(launchOptions);
        } catch (PlaywrightException exception) {

            if (browserChannel == null || !isMissingChannelFailure(exception)) {
                throw exception;
            }

            return playwright.chromium()
                    .launch(
                            new BrowserType.LaunchOptions()
                                    .setHeadless(resolveHeadless())
                                    .setTimeout(15000)
                                    .setArgs(java.util.Arrays.asList(
                                            "--incognito",
                                            "--no-first-run",
                                            "--no-default-browser-check",
                                            "--no-sandbox",
                                            "--disable-setuid-sandbox",
                                            "--disable-gpu",
                                            "--disable-dev-shm-usage"
                                    ))
                    );
        }
    }

    private boolean isMissingChannelFailure(PlaywrightException exception) {

        String message =
                exception.getMessage() == null
                        ? ""
                        : exception.getMessage()
                        .toLowerCase();

        return message.contains("distribution")
                &&
                message.contains("is not found");
    }

    private boolean resolveHeadless() {

        String configured =
                normalize(
                        System.getenv("AIF_HEADLESS")
                );

        if (configured != null) {
            return Boolean.parseBoolean(configured);
        }

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

    private boolean isTruthy(String value) {

        String normalized =
                normalize(value);

        return normalized != null
                &&
                !"false".equalsIgnoreCase(normalized)
                &&
                !"0".equals(normalized);
    }

    private double runtimeTimeoutMs() {

        return parseTimeout(
                System.getenv("AIF_STEP_TIMEOUT_MS"),
                8000
        );
    }

    private double runtimeNavigationTimeoutMs() {

        return parseTimeout(
                System.getenv("AIF_NAVIGATION_TIMEOUT_MS"),
                15000
        );
    }

    private double parseTimeout(String value, double fallback) {

        String normalized =
                normalize(value);

        if (normalized == null) {
            return fallback;
        }

        try {
            double parsed = Double.parseDouble(normalized);
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String normalize(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    @AfterStep
    public void captureStepScreenshot(Scenario scenario) {

        if (page == null) {
            return;
        }

        try {

            byte[] screenshot =
                    page.screenshot(
                            new Page.ScreenshotOptions()
                                    .setFullPage(false)
                                    .setTimeout(3000)
                    );

            scenario.attach(
                    screenshot,
                    "image/png",
                    "step-screenshot"
            );

        } catch (RuntimeException exception) {

            System.out.println(
                    "AIF screenshot skipped: "
                            + exception.getMessage()
            );
        }
    }

    @After
    public void tearDown() {

        if (context != null) {
            context.close();
        }

        if (browser != null) {
            browser.close();
        }

        if (playwright != null) {
            playwright.close();
        }
    }
}
""";
    }
}
