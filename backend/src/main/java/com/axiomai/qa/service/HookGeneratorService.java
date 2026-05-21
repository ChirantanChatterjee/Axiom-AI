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
                "true".equalsIgnoreCase(
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
                        .setArgs(java.util.Arrays.asList(
                                "--incognito",
                                "--no-first-run",
                                "--no-default-browser-check"
                        ));

        if (browserChannel != null && !browserChannel.isBlank()) {
            launchOptions.setChannel(browserChannel);
        }

        browser =
                playwright.chromium()
                        .launch(launchOptions);

        context = browser.newContext(
                new Browser.NewContextOptions()
                        .setViewportSize(1440, 1000)
        );

        page = context.newPage();
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
