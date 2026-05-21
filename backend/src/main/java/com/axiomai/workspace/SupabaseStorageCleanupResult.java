package com.axiomai.workspace;

public record SupabaseStorageCleanupResult(
        boolean configured,
        int deletedObjects,
        String message
) {

    public static SupabaseStorageCleanupResult skipped(
            String message
    ) {

        return new SupabaseStorageCleanupResult(
                false,
                0,
                message
        );
    }

    public static SupabaseStorageCleanupResult completed(
            int deletedObjects
    ) {

        return new SupabaseStorageCleanupResult(
                true,
                deletedObjects,
                "Supabase storage cleanup completed."
        );
    }

    public static SupabaseStorageCleanupResult failed(
            String message
    ) {

        return new SupabaseStorageCleanupResult(
                true,
                0,
                message
        );
    }
}
