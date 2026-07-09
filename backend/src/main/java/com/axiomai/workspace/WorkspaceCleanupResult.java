package com.axiomai.workspace;

public record WorkspaceCleanupResult(
        String sessionId,
        boolean workspaceSessionDeleted,
        boolean executionSessionDeleted,
        int localFilesDeleted,
        int reportFilesDeleted,
        SupabaseStorageCleanupResult supabaseStorage,
        boolean chatSessionDeleted,
        int chatMessagesAffected,
        String deletionMode
) {

    public WorkspaceCleanupResult(
            String sessionId,
            boolean workspaceSessionDeleted,
            boolean executionSessionDeleted,
            int localFilesDeleted,
            int reportFilesDeleted,
            SupabaseStorageCleanupResult supabaseStorage
    ) {

        this(
                sessionId,
                workspaceSessionDeleted,
                executionSessionDeleted,
                localFilesDeleted,
                reportFilesDeleted,
                supabaseStorage,
                false,
                0,
                "soft"
        );
    }

    public WorkspaceCleanupResult withChatDeletion(
            WorkspaceChatSessionDeletionResult chatDeletion
    ) {

        if (
                chatDeletion == null
        ) {

            return this;
        }

        return new WorkspaceCleanupResult(
                sessionId,
                workspaceSessionDeleted,
                executionSessionDeleted,
                localFilesDeleted,
                reportFilesDeleted,
                supabaseStorage,
                chatDeletion.deleted(),
                chatDeletion.messageCount(),
                chatDeletion.deletionMode()
        );
    }
}
