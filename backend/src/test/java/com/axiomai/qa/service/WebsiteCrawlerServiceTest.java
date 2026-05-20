package com.axiomai.qa.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebsiteCrawlerServiceTest {

    @Test
    void buildsSelectorForDataTestAttributeWithoutRewritingItToDataTestId()
            throws Exception {

        WebsiteCrawlerService service =
                new WebsiteCrawlerService();

        Method method =
                WebsiteCrawlerService.class
                        .getDeclaredMethod(
                                "buildCssSelector",
                                String.class,
                                String.class,
                                String.class,
                                String.class,
                                String.class,
                                String.class,
                                String.class,
                                String.class,
                                String.class
                        );

        method.setAccessible(true);

        String selector =
                (String) method.invoke(
                        service,
                        "INPUT",
                        "",
                        "user-name",
                        "text",
                        "",
                        "",
                        "",
                        "username",
                        ""
                );

        assertEquals(
                "[data-test='username']",
                selector
        );
    }
}
