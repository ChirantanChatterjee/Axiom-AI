package com.axiomai.workspace;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class GeneratedArtifact {

    // =====================================================
    // ARTIFACT NAME
    // =====================================================

    private String name;

    // =====================================================
    // ARTIFACT TYPE
    // =====================================================

    private String type;

    // =====================================================
    // ABSOLUTE PATH
    // =====================================================

    private String path;

    // =====================================================
    // DOWNLOAD URL
    // =====================================================

    private String downloadUrl;
}