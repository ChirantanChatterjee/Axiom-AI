package com.axiomai.qa.service;

import com.axiomai.qa.models.*;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FlowDetectionService {

    public List<DetectedFlow> detectFlows(
            SiteMapResult siteMap
    ) {

        List<DetectedFlow> flows =
                new ArrayList<>();

        for (PageNode page : siteMap.getPages()) {

            DetectedFlow loginFlow =
                    detectLoginFlow(page);

            if (loginFlow != null) {

                flows.add(loginFlow);
            }

            DetectedFlow searchFlow =
                    detectSearchFlow(page);

            if (searchFlow != null) {

                flows.add(searchFlow);
            }
        }

        return flows;
    }

    // =====================================================
    // LOGIN FLOW
    // =====================================================

    private DetectedFlow detectLoginFlow(
            PageNode page
    ) {

        PageElement usernameField = null;

        PageElement passwordField = null;

        PageElement loginButton = null;

        for (PageElement element : page.getElements()) {

            String role =
                    element.getBusinessRole();

            if (
                    role == null
            ) {
                continue;
            }

            if (
                    role.equals("AUTH_FIELD")
            ) {

                usernameField = element;
            }

            if (
                    role.equals("PASSWORD_FIELD")
            ) {

                passwordField = element;
            }

            if (
                    role.equals("LOGIN_BUTTON")
            ) {

                loginButton = element;
            }
        }

        if (
                usernameField != null
                        &&
                        passwordField != null
                        &&
                        loginButton != null
        ) {

            List<FlowStep> steps =
                    new ArrayList<>();

            steps.add(
                    new FlowStep(
                            "TYPE",
                            "USERNAME",
                            usernameField.getCssSelector()
                    )
            );

            steps.add(
                    new FlowStep(
                            "TYPE",
                            "PASSWORD",
                            passwordField.getCssSelector()
                    )
            );

            steps.add(
                    new FlowStep(
                            "CLICK",
                            "LOGIN_BUTTON",
                            loginButton.getCssSelector()
                    )
            );

            return new DetectedFlow(
                    FlowType.LOGIN,
                    page.getUrl(),
                    steps
            );
        }

        return null;
    }

    // =====================================================
    // SEARCH FLOW
    // =====================================================

    private DetectedFlow detectSearchFlow(
            PageNode page
    ) {

        PageElement searchField = null;

        PageElement searchButton = null;

        for (PageElement element : page.getElements()) {

            String role =
                    element.getBusinessRole();

            if (
                    role == null
            ) {
                continue;
            }

            if (
                    role.equals("SEARCH_FIELD")
            ) {

                searchField = element;
            }

            if (
                    role.equals("SEARCH_BUTTON")
            ) {

                searchButton = element;
            }
        }

        if (
                searchField != null
        ) {

            List<FlowStep> steps =
                    new ArrayList<>();

            steps.add(
                    new FlowStep(
                            "TYPE",
                            "SEARCH_TEXT",
                            searchField.getCssSelector()
                    )
            );

            if (searchButton != null) {

                steps.add(
                        new FlowStep(
                                "CLICK",
                                "SEARCH_BUTTON",
                                searchButton.getCssSelector()
                        )
                );
            }

            return new DetectedFlow(
                    FlowType.SEARCH,
                    page.getUrl(),
                    steps
            );
        }

        return null;
    }
}