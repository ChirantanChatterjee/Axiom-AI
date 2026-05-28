package com.axiomai.workspace;

import com.axiomai.qa.flow.DetectedFlow;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    void runtimeVariableAliasesStayWithinChatSession() {

        AutomationWorkspaceService service =
                new AutomationWorkspaceService();

        service.putVariable(
                "chat-one",
                "authfield",
                "user@example.com",
                false
        );

        service.putVariable(
                "chat-two",
                "username",
                "other@example.com",
                false
        );

        Map<String, String> chatOneValues =
                service.getVariableValues("chat-one");

        Map<String, String> chatTwoValues =
                service.getVariableValues("chat-two");

        assertEquals(
                "user@example.com",
                chatOneValues.get("username")
        );

        assertEquals(
                "user@example.com",
                chatOneValues.get("authfield")
        );

        assertEquals(
                "other@example.com",
                chatTwoValues.get("username")
        );

        assertEquals(
                "other@example.com",
                chatTwoValues.get("authfield")
        );
    }

    @Test
    void generatedTestTagMemoryStaysWithinChatSession() {

        AutomationWorkspaceService service =
                new AutomationWorkspaceService();

        service.setLastGeneratedTestExecution(
                "chat-one",
                "@login"
        );

        service.setLastGeneratedTestExecution(
                "chat-two",
                "@checkout"
        );

        assertEquals(
                "@login",
                service.getLastGeneratedTestExecution("chat-one")
        );

        assertEquals(
                "@checkout",
                service.getLastGeneratedTestExecution("chat-two")
        );

        assertNull(
                service.getLastGeneratedTestExecution("chat-three")
        );
    }

    @Test
    void removeSessionClearsChatScopedWorkspaceMemory() {

        AutomationWorkspaceService service =
                new AutomationWorkspaceService();

        service.setWebsite(
                "chat-one",
                "https://saucedemo.com"
        );

        assertNotNull(
                service.removeSession("chat-one")
        );

        assertNull(
                service.getSession("chat-one")
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
