package com.axiomai.ai.service;

import com.axiomai.ai.dto.AICommand;
import com.axiomai.ai.dto.AIResponse;
import com.axiomai.ai.intent.IntentParser;
import com.axiomai.ai.orchestrator.AICommandOrchestrator;
import com.axiomai.core.memory.ExecutionMemoryService;
import com.axiomai.core.session.ExecutionSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class AIOrchestratorService {

    private final IntentParser
            intentParser;

    private final AICommandOrchestrator
            orchestrator;

    private final ExecutionMemoryService
            executionMemoryService;

    // =====================================================
    // PROCESS MESSAGE
    // =====================================================

    public AIResponse processMessage(
            String message
    ) {

        String userId =
                "default-user";

        // =================================================
        // SESSION
        // =================================================

        ExecutionSession session =

                executionMemoryService
                        .getOrCreateSession(
                                userId
                        );

        // =================================================
        // PARSE
        // =================================================

        AICommand command =
                intentParser.parse(message);

        // =================================================
        // NORMALIZATION
        // =================================================

        normalizeCommand(
                command,
                message
        );

        // =================================================
        // CONVERSATIONAL RECOVERY
        // =================================================

        recoverConversationalIntent(
                command,
                message,
                session
        );

        // =================================================
        // SESSION ENRICHMENT
        // =================================================

        enrichCommandFromSession(
                command,
                session
        );

        // =================================================
        // UPDATE MEMORY
        // =================================================

        executionMemoryService
                .updateIntent(

                        userId,

                        command.getIntent()
                );

        // =================================================
        // EXECUTE
        // =================================================

        return orchestrator.execute(
                command
        );
    }

    // =====================================================
    // NORMALIZATION
    // =====================================================

    private void normalizeCommand(

            AICommand command,

            String message

    ) {

        // =================================================
        // URL FIX
        // =================================================

        if (

                "GENERATE_FRAMEWORK"
                        .equalsIgnoreCase(
                                command.getIntent()
                        )

                        &&

                        (
                                command.getUrl() == null
                                        ||
                                        command.getUrl().isBlank()
                        )

                        &&

                        command.getFlowName() != null
                        &&
                        command.getFlowName()
                                .startsWith("http")

        ) {

            command.setUrl(
                    command.getFlowName()
            );
        }

        // =================================================
        // TARGET FIX
        // =================================================

        if (

                (
                        command.getTarget() == null
                                ||
                                command.getTarget().isBlank()
                )

                        &&

                        command.getFlowName() != null
                        &&
                        !command.getFlowName().isBlank()

        ) {

            command.setTarget(
                    command.getFlowName()
            );
        }

        command.setMessage(message);
    }

    // =====================================================
    // CONVERSATIONAL RECOVERY
    // =====================================================

    private void recoverConversationalIntent(

            AICommand command,

            String message,

            ExecutionSession session

    ) {

        String lower =
                message.toLowerCase();

        // =================================================
        // EXECUTE IT
        // =================================================

        if (

                "UNKNOWN".equalsIgnoreCase(
                        command.getIntent()
                )

                        &&

                        (
                                lower.contains("execute")
                                        ||
                                        lower.contains("run")
                                        ||
                                        lower.contains("start")
                        )

        ) {

            command.setIntent(
                    "EXECUTE_FLOW"
            );

            command.setTarget(
                    session.getActiveFlowName()
            );

            return;
        }

        // =================================================
        // SHOW REPORT
        // =================================================

        if (

                "UNKNOWN".equalsIgnoreCase(
                        command.getIntent()
                )

                        &&

                        lower.contains("report")

        ) {

            command.setIntent(
                    "SHOW_REPORT"
            );

            return;
        }

        // =================================================
        // GENERATE FRAMEWORK
        // =================================================

        if (

                "UNKNOWN".equalsIgnoreCase(
                        command.getIntent()
                )

                        &&

                        lower.contains("generate")
                        &&
                        lower.contains("framework")

        ) {

            command.setIntent(
                    "GENERATE_FRAMEWORK"
            );
        }
    }

    // =====================================================
    // SESSION ENRICHMENT
    // =====================================================

    private void enrichCommandFromSession(

            AICommand command,

            ExecutionSession session

    ) {

        // =================================================
        // TARGET
        // =================================================

        if (

                (
                        command.getTarget() == null
                                ||
                                command.getTarget().isBlank()
                )

                        &&

                        session.getActiveFlowName()
                                != null

        ) {

            command.setTarget(
                    session.getActiveFlowName()
            );
        }

        // =================================================
        // URL
        // =================================================

        if (

                (
                        command.getUrl() == null
                                ||
                                command.getUrl().isBlank()
                )

                        &&

                        session.getActiveUrl()
                                != null

        ) {

            command.setUrl(
                    session.getActiveUrl()
            );
        }
    }
}