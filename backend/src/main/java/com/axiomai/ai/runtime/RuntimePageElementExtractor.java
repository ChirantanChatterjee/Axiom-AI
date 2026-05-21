package com.axiomai.ai.runtime;

import com.axiomai.qa.models.PageElement;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component

public class RuntimePageElementExtractor {

    // =====================================================
    // EXTRACT ELEMENTS
    // =====================================================

    public List<PageElement> extract(
            Page page
    ) {

        List<PageElement> elements =
                new ArrayList<>();

        try {

            // =============================================
            // DOM STABILIZATION
            // =============================================

            page.waitForLoadState();

            page.waitForTimeout(1000);

            extractInputs(
                    page,
                    elements
            );

            extractButtons(
                    page,
                    elements
            );

            extractLinks(
                    page,
                    elements
            );

            extractVideoCards(
                    page,
                    elements
            );

        } catch (Exception e) {

            e.printStackTrace();
        }

        return elements;
    }

    // =====================================================
    // INPUTS
    // =====================================================

    private void extractInputs(

            Page page,
            List<PageElement> elements

    ) {

        try {

            Locator inputs =
                    page.locator(
                            "input"
                    );

            int count =
                    Math.min(
                            inputs.count(),
                            100
                    );

            for (int i = 0; i < count; i++) {

                try {

                    Locator locator =
                            inputs.nth(i);

                    PageElement element =
                            buildElement(
                                    locator,
                                    "INPUT"
                            );

                    // =========================================
                    // SKIP EMPTY
                    // =========================================

                    if (element == null) {
                        continue;
                    }

                    // =========================================
                    // PASSWORD FIELD SPECIAL DETECTION
                    // =========================================

                    String type =
                            safe(
                                    element.getType()
                            ).toLowerCase();

                    String selector =
                            safe(
                                    element.getBestSelector()
                            ).toLowerCase();

                    if (

                            type.contains("password")
                                    ||

                                    selector.contains("passwd")
                                    ||

                                    selector.contains("password")

                    ) {

                        element.setBusinessRole(
                                "PASSWORD_FIELD"
                        );
                    }

                    elements.add(element);

                } catch (Exception ignored) {
                }
            }

        } catch (Exception ignored) {
        }
    }

    // =====================================================
    // BUTTONS
    // =====================================================

    private void extractButtons(

            Page page,
            List<PageElement> elements

    ) {

        try {

            Locator buttons =
                    page.locator(
                            "button"
                    );

            int count =
                    Math.min(
                            buttons.count(),
                            50
                    );

            for (int i = 0; i < count; i++) {

                try {

                    Locator locator =
                            buttons.nth(i);

                    PageElement element =
                            buildElement(
                                    locator,
                                    "BUTTON"
                            );

                    if (element != null) {

                        elements.add(element);
                    }

                } catch (Exception ignored) {
                }
            }

        } catch (Exception ignored) {
        }
    }

    // =====================================================
    // LINKS
    // =====================================================

    private void extractLinks(

            Page page,
            List<PageElement> elements

    ) {

        try {

            Locator links =
                    page.locator("a");

            int count =
                    Math.min(
                            links.count(),
                            50
                    );

            for (int i = 0; i < count; i++) {

                try {

                    Locator locator =
                            links.nth(i);

                    PageElement element =
                            buildElement(
                                    locator,
                                    "LINK"
                            );

                    if (element != null) {

                        elements.add(element);
                    }

                } catch (Exception ignored) {
                }
            }

        } catch (Exception ignored) {
        }
    }

    // =====================================================
    // VIDEO CARDS
    // =====================================================

    private void extractVideoCards(

            Page page,
            List<PageElement> elements

    ) {

        try {

            Locator videos =
                    page.locator(
                            "ytd-video-renderer"
                    );

            int count =
                    Math.min(
                            videos.count(),
                            20
                    );

            for (int i = 0; i < count; i++) {

                try {

                    Locator locator =
                            videos.nth(i);

                    PageElement element =
                            buildElement(
                                    locator,
                                    "VIDEO_CARD"
                            );

                    if (element == null) {
                        continue;
                    }

                    element.setBusinessRole(
                            "VIDEO_CARD"
                    );

                    elements.add(element);

                } catch (Exception ignored) {
                }
            }

        } catch (Exception ignored) {
        }
    }

    // =====================================================
    // BUILD ELEMENT
    // =====================================================

    private PageElement buildElement(

            Locator locator,
            String type

    ) {

        try {

            PageElement element =
                    new PageElement();

            String text =
                    safeText(locator);

            String placeholder =
                    safeAttribute(
                            locator,
                            "placeholder"
                    );

            String aria =
                    safeAttribute(
                            locator,
                            "aria-label"
                    );

            String typeAttr =
                    safeAttribute(
                            locator,
                            "type"
                    );

            String selector =
                    buildBestSelector(
                            locator
                    );

            // =============================================
            // VISIBILITY
            // =============================================

            boolean visible = false;

            try {

                visible =
                        locator.isVisible();

            } catch (Exception ignored) {
            }

            element.setTag(type);

            element.setText(text);

            element.setPlaceholder(
                    placeholder
            );

            element.setAriaLabel(
                    aria
            );

            element.setType(
                    typeAttr
            );

            element.setBestSelector(
                    selector
            );

            element.setVisible(
                    visible
            );

            determineBusinessRole(
                    element
            );

            return element;

        } catch (Exception ignored) {

            return null;
        }
    }

    // =====================================================
    // BUSINESS ROLE
    // =====================================================

    private void determineBusinessRole(
            PageElement element
    ) {

        String combined = (

                safe(element.getText())
                        + " "

                        + safe(element.getPlaceholder())
                        + " "

                        + safe(element.getAriaLabel())
                        + " "

                        + safe(element.getType())
                        + " "

                        + safe(element.getBestSelector())

        ).toLowerCase();

        // =================================================
        // PASSWORD
        // =================================================

        if (

                combined.contains("password")
                        ||

                        combined.contains("passwd")

        ) {

            element.setBusinessRole(
                    "PASSWORD_FIELD"
            );

            return;
        }

        // =================================================
        // SEARCH
        // =================================================

        if (

                combined.contains("search")

        ) {

            element.setBusinessRole(
                    "SEARCH_BOX"
            );

            return;
        }

        // =================================================
        // LOGIN
        // =================================================

        if (

                combined.contains("sign in")
                        ||
                        combined.contains("login")

        ) {

            element.setBusinessRole(
                    "SIGN_IN_BUTTON"
            );

            return;
        }

        // =================================================
        // EMAIL
        // =================================================

        if (

                combined.contains("email")
                        ||
                        combined.contains("username")
                        ||
                        combined.contains("user-name")
                        ||
                        combined.contains("user name")
                        ||
                        combined.contains("phone")
                        ||
                        combined.contains("identifier")

        ) {

            element.setBusinessRole(
                    "USERNAME_FIELD"
            );

            return;
        }
    }

    // =====================================================
    // BEST SELECTOR
    // =====================================================

    private String buildBestSelector(
            Locator locator
    ) {

        try {

            String id =
                    safeAttribute(
                            locator,
                            "id"
                    );

            if (

                    id != null
                            &&
                            !id.isBlank()

            ) {

                return "#" + id;
            }

            String dataTestId =
                    safeAttribute(
                            locator,
                            "data-testid"
                    );

            if (

                    dataTestId != null
                            &&
                            !dataTestId.isBlank()

            ) {

                return "[data-testid='"
                        + cssAttr(dataTestId)
                        + "']";
            }

            String dataTest =
                    safeAttribute(
                            locator,
                            "data-test"
                    );

            if (

                    dataTest != null
                            &&
                            !dataTest.isBlank()

            ) {

                return "[data-test='"
                        + cssAttr(dataTest)
                        + "']";
            }

            String dataCy =
                    safeAttribute(
                            locator,
                            "data-cy"
                    );

            if (

                    dataCy != null
                            &&
                            !dataCy.isBlank()

            ) {

                return "[data-cy='"
                        + cssAttr(dataCy)
                        + "']";
            }

            String name =
                    safeAttribute(
                            locator,
                            "name"
                    );

            if (

                    name != null
                            &&
                            !name.isBlank()

            ) {

                return "[name='"
                        + name
                        + "']";
            }

            String aria =
                    safeAttribute(
                            locator,
                            "aria-label"
                    );

            if (

                    aria != null
                            &&
                            !aria.isBlank()

            ) {

                return "[aria-label='"
                        + aria
                        + "']";
            }

        } catch (Exception ignored) {
        }

        return null;
    }

    // =====================================================
    // SAFE ATTRIBUTE
    // =====================================================

    private String safeAttribute(

            Locator locator,
            String attribute

    ) {

        try {

            return locator.getAttribute(
                    attribute
            );

        } catch (Exception e) {

            return null;
        }
    }

    // =====================================================
    // SAFE TEXT
    // =====================================================

    private String safeText(
            Locator locator
    ) {

        try {

            return locator.innerText();

        } catch (Exception e) {

            return "";
        }
    }

    // =====================================================
    // SAFE
    // =====================================================

    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }

    private String cssAttr(
            String value
    ) {

        return safe(value)
                .replace("\\", "\\\\")
                .replace("'", "\\'");
    }
}
