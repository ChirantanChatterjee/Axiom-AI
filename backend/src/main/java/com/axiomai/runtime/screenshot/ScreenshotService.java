package com.axiomai.runtime.screenshot;

import com.microsoft.playwright.Page;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ScreenshotService {

    private static final String
            SCREENSHOT_DIR =
            "screenshots/";

    public String takeScreenshot(

            Page page,
            Long executionId,
            Integer stepOrder,
            String status

    ) {

        try {

            File directory =
                    new File(SCREENSHOT_DIR);

            if (!directory.exists()) {

                directory.mkdirs();

            }

            String timestamp =
                    LocalDateTime.now()
                            .format(
                                    DateTimeFormatter.ofPattern(
                                            "yyyyMMdd_HHmmss"
                                    )
                            );

            String filename =

                    executionId
                            + "_"
                            + stepOrder
                            + "_"
                            + status
                            + "_"
                            + timestamp
                            + ".png";

            String fullPath =
                    SCREENSHOT_DIR + filename;

            page.screenshot(

                    new Page.ScreenshotOptions()
                            .setPath(
                                    Paths.get(fullPath)
                            )
                            .setFullPage(true)

            );

            System.out.println(
                    "Screenshot saved: "
                            + fullPath
            );

            return fullPath;

        } catch (Exception e) {

            System.out.println(
                    "Screenshot capture failed: "
                            + e.getMessage()
            );

            return null;

        }

    }

}