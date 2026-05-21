package com.axiomai.ai.service;

import com.axiomai.ai.model.IntentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class AIIntentService {

    public IntentResponse understand(String message) {

        String lower =
                message.toLowerCase();

        // ==========================================
        // EXECUTE FLOW
        // ==========================================

        if (

                lower.contains("execute")
                        &&
                        lower.contains("flow")

        ) {

            return IntentResponse
                    .builder()
                    .intent("EXECUTE_FLOW")
                    .flowId(1L)
                    .rawResponse(message)
                    .build();
        }

        // ==========================================
        // SHOW REPORT
        // ==========================================

        if (

                lower.contains("report")

        ) {

            return IntentResponse
                    .builder()
                    .intent("SHOW_REPORT")
                    .flowId(1L)
                    .rawResponse(message)
                    .build();
        }

        // ==========================================
        // DB ENTRIES
        // ==========================================

        if (

                lower.contains("database")
                        ||
                        lower.contains("db")

        ) {

            return IntentResponse
                    .builder()
                    .intent("SHOW_DB")
                    .flowId(1L)
                    .rawResponse(message)
                    .build();
        }

        // ==========================================
        // GENERATE FRAMEWORK
        // ==========================================

        if (

                lower.contains("generate")
                        &&
                        lower.contains("framework")

        ) {

            return IntentResponse
                    .builder()
                    .intent("GENERATE_FRAMEWORK")
                    .url(
                            "https://opensource-demo.orangehrmlive.com"
                    )
                    .rawResponse(message)
                    .build();
        }

        // ==========================================
        // UNKNOWN
        // ==========================================

        return IntentResponse
                .builder()
                .intent("UNKNOWN")
                .rawResponse(message)
                .build();
    }

}