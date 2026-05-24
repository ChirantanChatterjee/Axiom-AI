package com.axiomai.workspace;

import com.axiomai.qa.execution.repository.GeneratedTestExecutionJobRepository;
import com.axiomai.qa.service.GeneratedProjectWriterService;
import com.axiomai.workspace.repository.GeneratedFrameworkArchiveRepository;
import com.axiomai.workspace.repository.WorkspaceSessionVariableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;

@Service
@RequiredArgsConstructor
public class WorkspaceSessionPresenceService {

    private final AutomationWorkspaceService
            automationWorkspaceService;

    private final GeneratedProjectWriterService
            generatedProjectWriterService;

    private final GeneratedFrameworkArchiveRepository
            generatedFrameworkArchiveRepository;

    private final WorkspaceSessionVariableRepository
            variableRepository;

    private final GeneratedTestExecutionJobRepository
            generatedTestExecutionJobRepository;

    @Transactional(readOnly = true)
    public boolean hasExistingSessionState(
            String sessionId
    ) {

        if (
                sessionId == null
                        ||
                        sessionId.isBlank()
        ) {

            return false;
        }

        return automationWorkspaceService.hasSession(sessionId)
                ||
                Files.exists(
                        generatedProjectWriterService
                                .getWorkspaceRoot(sessionId)
                )
                ||
                generatedFrameworkArchiveRepository.existsById(sessionId)
                ||
                variableRepository.existsBySessionId(sessionId)
                ||
                generatedTestExecutionJobRepository.existsBySessionId(sessionId);
    }
}
