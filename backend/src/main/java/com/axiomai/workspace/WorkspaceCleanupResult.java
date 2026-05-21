package com.axiomai.workspace;

public record WorkspaceCleanupResult(
        String sessionId,
        boolean workspaceSessionDeleted,
        boolean executionSessionDeleted,
        int localFilesDeleted,
        int reportFilesDeleted,
        SupabaseStorageCleanupResult supabaseStorage
) {
}
