package com.axiomai.workspace;

import com.axiomai.qa.flow.DetectedFlow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AutomationWorkspaceServiceTest {

    @Test
    void chatSessionIdIsUsedAsFrameworkWorkspaceId() {

        AutomationWorkspaceService service =
                new AutomationWorkspaceService();

        AutomationSession session =
                service.getOrCreateSession(
                        "chat-abc"
                );

        assertEquals(
                "chat-abc",
                session.getSessionId()
        );
    }

    @Test
    void chatsKeepSeparateWebsiteContext() {

        AutomationWorkspaceService service =
                new AutomationWorkspaceService();

        service.setWebsite(
                "chat-one",
                "https://saucedemo.com"
        );

        service.setWebsite(
                "chat-two",
                "https://example.com"
        );

        assertEquals(
                "https://saucedemo.com",
                service.getSession("chat-one")
                        .getWebsiteUrl()
        );

        assertEquals(
                "https://example.com",
                service.getSession("chat-two")
                        .getWebsiteUrl()
        );

        assertNull(
                service.getSession("chat-three")
                        .getWebsiteUrl()
        );
    }

    @Test
    void findFlowMatchesSpaceAndUnderscoreFeatureAliases() {

        AutomationWorkspaceService service =
                new AutomationWorkspaceService();

        DetectedFlow billPay =
                new DetectedFlow();

        billPay.setFlowType("BILL_PAY");
        billPay.setPageUrl("https://parabank.parasoft.com/parabank/index.htm");

        service.storeFlows(
                "chat-one",
                List.of(billPay)
        );

        assertEquals(
                billPay,
                service.findFlow(
                        "chat-one",
                        "bill pay"
                )
        );
    }
}
