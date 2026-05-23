package com.axiomai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Locale;

@SpringBootApplication
@EnableScheduling
public class BackendApplication {

    public static void main(String[] args) {

        SpringApplication application =
                new SpringApplication(BackendApplication.class);

        String serviceRole =
                serviceRole();

        if (
                "worker".equals(serviceRole)
        ) {

            setSystemPropertyIfEnvMissing(
                    "aif.worker.enabled",
                    "true",
                    "AIF_WORKER_ENABLED"
            );

            setSystemPropertyIfEnvMissing(
                    "spring.main.web-application-type",
                    "none",
                    "SPRING_MAIN_WEB_APPLICATION_TYPE"
            );

            if (
                    isBlank(
                            System.getenv(
                                    "SPRING_MAIN_WEB_APPLICATION_TYPE"
                            )
                    )
            ) {

                application.setWebApplicationType(
                        WebApplicationType.NONE
                );
            }

        } else {

            setSystemPropertyIfEnvMissing(
                    "aif.worker.enabled",
                    "false",
                    "AIF_WORKER_ENABLED"
            );
        }

        application.run(args);

    }

    private static String serviceRole() {

        String configured =
                System.getenv("AIF_SERVICE_ROLE");

        if (
                configured == null
                        ||
                        configured.isBlank()
        ) {

            return "api";
        }

        return configured.trim()
                .toLowerCase(Locale.ROOT);
    }

    private static void setSystemPropertyIfEnvMissing(

            String property,
            String value,
            String environmentVariable

    ) {

        if (
                isBlank(
                        System.getenv(environmentVariable)
                )
                        &&
                        isBlank(
                                System.getProperty(property)
                        )
        ) {

            System.setProperty(
                    property,
                    value
            );
        }
    }

    private static boolean isBlank(
            String value
    ) {

        return value == null
                ||
                value.isBlank();
    }

}
