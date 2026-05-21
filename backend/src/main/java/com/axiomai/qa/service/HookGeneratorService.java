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

        page = context.newPage();
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

        byte[] screenshot =
                page.screenshot(
                        new Page.ScreenshotOptions()
                                .setFullPage(true)
                );

        scenario.attach(
                screenshot,
                "image/png",
                "step-screenshot"
        );
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
