package com.axiomai.qa.ai;

import com.axiomai.qa.models.PageElement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalSemanticResolverTest {

    @Test
    void resolvesPlainUsernameTargetToVisibleUsernameInput() {

        PageElement username =
                new PageElement();

        username.setType("text");
        username.setPlaceholder("Username");
        username.setCssSelector("#user-name");
        username.setVisible(true);
        username.setBusinessRole("USERNAME_FIELD");

        ElementSemanticMatch match =
                new LocalSemanticResolver()
                        .resolve(
                                "USERNAME",
                                List.of(username)
                        );

        assertNotNull(match);

        assertEquals(
                "#user-name",
                match.getSelector()
        );

        assertTrue(
                match.getConfidence() >= 0.75
        );
    }
}
