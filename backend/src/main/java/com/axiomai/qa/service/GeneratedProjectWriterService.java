package com.axiomai.qa.service;

import com.axiomai.qa.models.GeneratedFramework;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class GeneratedProjectWriterService {

    @Autowired
    private HookGeneratorService hookGeneratorService;

    @Autowired
    private RunnerGeneratorService runnerGeneratorService;

    @Autowired
    private PomGeneratorService pomGeneratorService;

    // =====================================================
    // MAIN WRITE METHOD
    // =====================================================

    public String writeFramework(
            GeneratedFramework framework
    ) {

        try {

            // =================================================
            // ROOT
            // =================================================

            String root =
                    "generated-framework";

            // =================================================
            // FEATURE FOLDER
            // =================================================

            Path featureFolder =
                    Paths.get(
                            root,
                            "src/test/resources/features"
                    );

            // =================================================
            // STEP DEFINITIONS
            // =================================================

            Path stepFolder =
                    Paths.get(
                            root,
                            "src/test/java/com/axiomai/generated/steps"
                    );

            // =================================================
            // PAGE OBJECTS
            // =================================================

            Path pageFolder =
                    Paths.get(
                            root,
                            "src/test/java/com/axiomai/generated/pages"
                    );

            // =================================================
            // HOOKS
            // =================================================

            Path hooksFolder =
                    Paths.get(
                            root,
                            "src/test/java/com/axiomai/generated/hooks"
                    );

            // =================================================
            // RUNNER
            // =================================================

            Path runnerFolder =
                    Paths.get(
                            root,
                            "src/test/java/com/axiomai/generated/runner"
                    );

            // =================================================
            // CREATE DIRECTORIES
            // =================================================

            Files.createDirectories(featureFolder);

            Files.createDirectories(stepFolder);

            Files.createDirectories(pageFolder);

            Files.createDirectories(hooksFolder);

            Files.createDirectories(runnerFolder);

            // =================================================
            // WRITE FEATURE FILE
            // =================================================

            Files.writeString(

                    featureFolder.resolve(
                            "search.feature"
                    ),

                    framework.getFeatureFile()
            );

            // =================================================
            // WRITE PAGE OBJECT
            // =================================================

            Files.writeString(

                    pageFolder.resolve(
                            "GooglePage.java"
                    ),

                    framework.getPageObject()
            );

            // =================================================
            // WRITE STEP DEFINITIONS
            // =================================================

            Files.writeString(

                    stepFolder.resolve(
                            "GoogleSteps.java"
                    ),

                    framework.getStepDefinition()
            );

            // =================================================
            // WRITE HOOKS
            // =================================================

            Files.writeString(

                    hooksFolder.resolve(
                            "Hooks.java"
                    ),

                    hookGeneratorService.generateHooks()
            );

            // =================================================
            // WRITE TEST RUNNER
            // =================================================

            Files.writeString(

                    runnerFolder.resolve(
                            "TestRunner.java"
                    ),

                    runnerGeneratorService.generateRunner()
            );

            // =================================================
            // WRITE POM.XML
            // =================================================

            Files.writeString(

                    Paths.get(
                            root,
                            "pom.xml"
                    ),

                    pomGeneratorService.generatePom()
            );

            return "Framework generated successfully.";

        } catch (IOException e) {

            e.printStackTrace();

            return "Framework generation failed.";
        }
    }
}