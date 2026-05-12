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
                        );

        context = browser.newContext();

        page = context.newPage();
    }

    @After
    public void tearDown() {

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