package com.axiomai.ai.service;

import com.axiomai.ai.dto.AIResponse;
import com.axiomai.ai.intent.IntentParser;
import com.axiomai.ai.orchestrator.AICommandOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class AIOrchestratorService {

    private final IntentParser
            intentParser;

    private final AICommandOrchestrator
            orchestrator;

    public AIResponse processMessage(
            String message
    ) {

        var command =
                intentParser.parse(message);

        return orchestrator.execute(command);

    }

}