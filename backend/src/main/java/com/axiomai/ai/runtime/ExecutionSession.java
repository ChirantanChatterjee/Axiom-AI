package com.axiomai.ai.runtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ExecutionSession {

    // =====================================================
    // SESSION ID
    // =====================================================

    @Builder.Default
    private String sessionId =
            UUID.randomUUID().toString();

    // =====================================================
    // START TIME
    // =====================================================

    @Builder.Default
    private LocalDateTime startedAt =
            LocalDateTime.now();

    // =====================================================
    // STATUS
    // =====================================================

    private String status;

    // =====================================================
    // TARGET URL
    // =====================================================

    private String targetUrl;

}