package com.axiomai.workspace;

import com.axiomai.qa.service.GeneratedProjectWriterService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkspaceArtifactControllerTest {

    private final StubGeneratedProjectWriterService generatedProjectWriterService =
            new StubGeneratedProjectWriterService();

    private final StubWorkspaceAccessService workspaceAccessService =
            new StubWorkspaceAccessService();

    private final WorkspaceArtifactController controller =
            new WorkspaceArtifactController(
                    generatedProjectWriterService,
                    workspaceAccessService
            );

    @Test
    void downloadRequiresExistingWorkspaceOwnership()
            throws Exception {

        Path workspace =
                Files.createTempDirectory("aif-artifact-test");

        Files.writeString(
                workspace.resolve("framework.zip"),
                "zip"
        );

        workspaceAccessService.normalizedSessionId =
                "chat-1";

        generatedProjectWriterService.workspace =
                workspace;

        ResponseEntity<Resource> response =
                controller.download(
                        "chat-1",
                        "framework.zip",
                        "session-token"
                );

        assertEquals(
                200,
                response.getStatusCode()
                        .value()
        );

        assertEquals(
                "session-token",
                workspaceAccessService.lastToken
        );

        assertEquals(
                "chat-1",
                workspaceAccessService.lastSessionId
        );
    }

    @Test
    void userACannotAccessUserBGeneratedArtifact() {

        workspaceAccessService.exception =
                new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "This workspace belongs to a different user."
                );

        assertThrows(
                ResponseStatusException.class,
                () -> controller.download(
                        "chat-1",
                        "framework.zip",
                        "bad-token"
                )
        );
    }

    private static class StubGeneratedProjectWriterService
            extends GeneratedProjectWriterService {

        private Path workspace;

        private StubGeneratedProjectWriterService() {

            super(
                    null,
                    null,
                    null
            );
        }

        @Override
        public Path getWorkspaceRoot(
                String sessionId
        ) {

            return workspace;
        }
    }

    private static class StubWorkspaceAccessService
            extends WorkspaceAccessService {

        private String normalizedSessionId;

        private ResponseStatusException exception;

        private String lastToken;

        private String lastSessionId;

        private StubWorkspaceAccessService() {

            super(
                    null,
                    null,
                    null
            );
        }

        @Override
        public String requireAccess(
                String token,
                String sessionId
        ) {

            lastToken =
                    token;

            lastSessionId =
                    sessionId;

            if (
                    exception != null
            ) {

                throw exception;
            }

            return normalizedSessionId;
        }
    }
}
