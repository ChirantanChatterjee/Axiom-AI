package com.axiomai.qa.service;

import org.springframework.stereotype.Service;

@Service
public class PomGeneratorService {

    public String generatePom() {

        return """
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"

         xsi:schemaLocation="
         http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>com.axiomai.generated</groupId>

    <artifactId>generated-framework</artifactId>

    <version>1.0</version>

    <properties>

        <maven.compiler.source>17</maven.compiler.source>

        <maven.compiler.target>17</maven.compiler.target>

    </properties>

    <dependencies>

        <!-- PLAYWRIGHT -->

        <dependency>

            <groupId>com.microsoft.playwright</groupId>

            <artifactId>playwright</artifactId>

            <version>1.52.0</version>

        </dependency>

        <!-- CUCUMBER -->

        <dependency>

            <groupId>io.cucumber</groupId>

            <artifactId>cucumber-java</artifactId>

            <version>7.20.1</version>

        </dependency>

        <dependency>

            <groupId>io.cucumber</groupId>

            <artifactId>cucumber-junit-platform-engine</artifactId>

            <version>7.20.1</version>

            <scope>test</scope>

        </dependency>

        <!-- JUNIT -->

        <dependency>

            <groupId>org.junit.platform</groupId>

            <artifactId>junit-platform-suite</artifactId>

            <version>1.10.2</version>

            <scope>test</scope>

        </dependency>

    </dependencies>

    <build>

        <plugins>

            <plugin>

                <groupId>org.apache.maven.plugins</groupId>

                <artifactId>maven-surefire-plugin</artifactId>

                <version>3.2.5</version>

            </plugin>

        </plugins>

    </build>

</project>
""";
    }
}
