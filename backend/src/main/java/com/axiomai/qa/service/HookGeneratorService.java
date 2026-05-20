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

        browser =
                playwright.chromium()
                        .launch(
                                new BrowserType.LaunchOptions()
                                        .setHeadless(false)
                                        .setArgs(java.util.Arrays.asList(
                                                "--incognito",
                                                "--start-maximized",
                                                "--start-fullscreen",
                                                "--no-first-run",
                                                "--no-default-browser-check"
                                        ))
                        );

        context = browser.newContext(
                new Browser.NewContextOptions()
                        .setViewportSize(null)
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
