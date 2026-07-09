package com.axiomai.workspace;

public record WorkspaceChatSessionDeletionResult(
        String sessionId,
        boolean deleted,
        boolean alreadyDeleted,
        int messageCount,
        String deletionMode
) {

    public static WorkspaceChatSessionDeletionResult softDeleted(
            String sessionId,
            boolean alreadyDeleted,
            int messageCount
    ) {

        return new WorkspaceChatSessionDeletionResult(
                sessionId,
                true,
                alreadyDeleted,
                messageCount,
                "soft"
        );
    }

    public static WorkspaceChatSessionDeletionResult notFound(
            String sessionId
    ) {

        return new WorkspaceChatSessionDeletionResult(
                sessionId,
                false,
                false,
                0,
                "soft"
        );
    }
}
