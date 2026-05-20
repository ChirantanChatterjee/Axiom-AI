package com.axiomai.qa.service;

import org.springframework.stereotype.Service;

@Service
public class RunnerGeneratorService {

    public String generateRunner() {

        return """
package com.axiomai.generated.runner;

import org.junit.platform.suite.api.*;

import static io.cucumber.junit.platform.engine.Constants.*;

@Suite
@IncludeEngines("cucumber")

@SelectClasspathResource("features")

@ConfigurationParameter(
        key = GLUE_PROPERTY_NAME,
        value = "com.axiomai.generated"
)

@ConfigurationParameter(
        key = PLUGIN_PROPERTY_NAME,
        value =
                "pretty," +
                "html:target/cucumber-report.html," +
                "json:target/cucumber-report.json," +
                "junit:target/cucumber-report.xml"
)

public class TestRunner {
}
""";
    }
}
