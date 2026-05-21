package com.axiomai.workspace;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class WorkspaceVariable {

    // =====================================================
    // VARIABLE KEY
    // =====================================================

    private String key;

    // =====================================================
    // VARIABLE VALUE
    // =====================================================

    private String value;

    // =====================================================
    // SENSITIVE
    // =====================================================

    private boolean sensitive;

    // =====================================================
    // SOURCE
    // =====================================================

    private String source;
}