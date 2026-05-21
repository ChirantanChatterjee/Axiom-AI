package com.axiomai.workspace;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SupabaseStorageCleanupService {

    private static final int BATCH_SIZE =
            100;

    private final ObjectMapper
            objectMapper;

    private final HttpClient
            httpClient;

    private final String
            supabaseUrl;

    private final String
            serviceRoleKey;

    private final String
            bucket;

    private final String
            storagePrefix;

    public SupabaseStorageCleanupService(

            ObjectMapper objectMapper,

            @Value("${aif.supabase.url:}") String supabaseUrl,

            @Value("${aif.supabase.service-role-key:}") String serviceRoleKey,

            @Value("${aif.supabase.storage.bucket:}") String bucket,

            @Value("${aif.supabase.storage.prefix:generated-frameworks/}") String storagePrefix

    ) {

        this.objectMapper =
                objectMapper;

        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(
                                Duration.ofSeconds(10)
                        )
                        .build();

        this.supabaseUrl =
                trimTrailingSlash(supabaseUrl);

        this.serviceRoleKey =
                serviceRoleKey == null
                        ? ""
                        : serviceRoleKey.trim();

        this.bucket =
                bucket == null
                        ? ""
                        : bucket.trim();

        this.storagePrefix =
                normalizePrefix(storagePrefix);
    }

    public SupabaseStorageCleanupResult cleanupSession(
            String sessionId
    ) {

        if (
                supabaseUrl.isBlank()
                        ||
                        serviceRoleKey.isBlank()
                        ||
                        bucket.isBlank()
        ) {

            return SupabaseStorageCleanupResult.skipped(
                    "Supabase storage cleanup is not configured."
            );
        }

        if (
                sessionId == null
                        ||
                        sessionId.isBlank()
        ) {

            return SupabaseStorageCleanupResult.failed(
                    "Supabase storage cleanup skipped because the session id is blank."
            );
        }

        try {

            String sessionPrefix =
                    storagePrefix
                            + sanitizePathPart(sessionId);

            List<String> objectPaths =
                    new ArrayList<>();

            collectObjectPaths(
                    sessionPrefix,
                    objectPaths
            );

            int deletedObjects =
                    deleteObjectPaths(objectPaths);

            return SupabaseStorageCleanupResult.completed(
                    deletedObjects
            );

        } catch (Exception e) {

            return SupabaseStorageCleanupResult.failed(
                    "Supabase storage cleanup failed: "
                            + e.getMessage()
            );
        }
    }

    public boolean isConfigured() {

        return !supabaseUrl.isBlank()
                &&
                !serviceRoleKey.isBlank()
                &&
                !bucket.isBlank();
    }

    public String sessionObjectPath(

            String sessionId,

            String fileName

    ) {

        return storagePrefix
                + sanitizePathPart(sessionId)
                + "/"
                + sanitizePathPart(fileName);
    }

    public boolean uploadFile(

            String objectPath,

            Path file

    ) throws Exception {

        if (
                !isConfigured()
        ) {

            return false;
        }

        if (
                objectPath == null
                        ||
                        objectPath.isBlank()
                        ||
                        file == null
                        ||
                        !Files.exists(file)
        ) {

            return false;
        }

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        supabaseUrl
                                                + "/storage/v1/object/"
                                                + encodePathSegment(bucket)
                                                + "/"
                                                + encodeObjectPath(objectPath)
                                )
                        )
                        .timeout(
                                Duration.ofSeconds(60)
                        )
                        .header(
                                "apikey",
                                serviceRoleKey
                        )
                        .header(
                                "Authorization",
                                "Bearer " + serviceRoleKey
                        )
                        .header(
                                "Content-Type",
                                "application/zip"
                        )
                        .header(
                                "x-upsert",
                                "true"
                        )
                        .POST(
                                HttpRequest.BodyPublishers.ofFile(file)
                        )
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (
                response.statusCode() < 200
                        ||
                        response.statusCode() >= 300
        ) {

            throw new IllegalStateException(
                    "Supabase upload request failed with status "
                            + response.statusCode()
            );
        }

        return true;
    }

    public boolean downloadFile(

            String objectPath,

            Path target

    ) throws Exception {

        if (
                !isConfigured()
        ) {

            return false;
        }

        if (
                objectPath == null
                        ||
                        objectPath.isBlank()
                        ||
                        target == null
        ) {

            return false;
        }

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        supabaseUrl
                                                + "/storage/v1/object/"
                                                + encodePathSegment(bucket)
                                                + "/"
                                                + encodeObjectPath(objectPath)
                                )
                        )
                        .timeout(
                                Duration.ofSeconds(60)
                        )
                        .header(
                                "apikey",
                                serviceRoleKey
                        )
                        .header(
                                "Authorization",
                                "Bearer " + serviceRoleKey
                        )
                        .GET()
                        .build();

        HttpResponse<byte[]> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofByteArray()
                );

        if (
                response.statusCode() == 404
        ) {

            return false;
        }

        if (
                response.statusCode() < 200
                        ||
                        response.statusCode() >= 300
        ) {

            throw new IllegalStateException(
                    "Supabase download request failed with status "
                            + response.statusCode()
            );
        }

        Files.createDirectories(
                target.getParent()
        );

        Files.write(
                target,
                response.body()
        );

        return true;
    }

    private void collectObjectPaths(

            String prefix,

            List<String> objectPaths

    ) throws Exception {

        List<JsonNode> entries =
                listObjects(prefix);

        for (
                JsonNode entry
                : entries
        ) {

            String name =
                    entry.path("name")
                            .asText("");

            if (
                    name.isBlank()
            ) {

                continue;
            }

            String childPath =
                    prefix.isBlank()
                            ? name
                            : prefix + "/" + name;

            if (
                    isFolder(entry)
            ) {

                collectObjectPaths(
                        childPath,
                        objectPaths
                );

            } else {

                objectPaths.add(childPath);
            }
        }
    }

    private List<JsonNode> listObjects(
            String prefix
    ) throws Exception {

        List<JsonNode> allEntries =
                new ArrayList<>();

        int offset =
                0;

        while (true) {

            List<JsonNode> page =
                    listObjectPage(
                            prefix,
                            offset
                    );

            allEntries.addAll(page);

            if (
                    page.size() < 1000
            ) {

                break;
            }

            offset += page.size();
        }

        return allEntries;
    }

    private List<JsonNode> listObjectPage(

            String prefix,

            int offset

    ) throws Exception {

        Map<String, Object> body =
                new LinkedHashMap<>();

        body.put(
                "prefix",
                prefix
        );

        body.put(
                "limit",
                1000
        );

        body.put(
                "offset",
                offset
        );

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        supabaseUrl
                                                + "/storage/v1/object/list/"
                                                + encodePathSegment(bucket)
                                )
                        )
                        .timeout(
                                Duration.ofSeconds(30)
                        )
                        .header(
                                "apikey",
                                serviceRoleKey
                        )
                        .header(
                                "Authorization",
                                "Bearer " + serviceRoleKey
                        )
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        objectMapper.writeValueAsString(body)
                                )
                        )
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (
                response.statusCode() < 200
                        ||
                        response.statusCode() >= 300
        ) {

            throw new IllegalStateException(
                    "Supabase list request failed with status "
                            + response.statusCode()
            );
        }

        return objectMapper.readValue(
                response.body(),
                new TypeReference<List<JsonNode>>() {
                }
        );
    }

    private int deleteObjectPaths(
            List<String> objectPaths
    ) throws Exception {

        int deleted =
                0;

        for (
                int start = 0;
                start < objectPaths.size();
                start += BATCH_SIZE
        ) {

            int end =
                    Math.min(
                            start + BATCH_SIZE,
                            objectPaths.size()
                    );

            List<String> batch =
                    objectPaths.subList(
                            start,
                            end
                    );

            Map<String, Object> body =
                    Map.of(
                            "prefixes",
                            batch
                    );

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(
                                            supabaseUrl
                                                    + "/storage/v1/object/"
                                                    + encodePathSegment(bucket)
                                    )
                            )
                            .timeout(
                                    Duration.ofSeconds(30)
                            )
                            .header(
                                    "apikey",
                                    serviceRoleKey
                            )
                            .header(
                                    "Authorization",
                                    "Bearer " + serviceRoleKey
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .method(
                                    "DELETE",
                                    HttpRequest.BodyPublishers.ofString(
                                            objectMapper.writeValueAsString(body)
                                    )
                            )
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (
                    response.statusCode() < 200
                            ||
                            response.statusCode() >= 300
            ) {

                throw new IllegalStateException(
                        "Supabase delete request failed with status "
                                + response.statusCode()
                );
            }

            deleted += batch.size();
        }

        return deleted;
    }

    private boolean isFolder(
            JsonNode entry
    ) {

        return entry.path("id")
                .isMissingNode()
                ||
                entry.path("id")
                        .isNull();
    }

    private String sanitizePathPart(
            String value
    ) {

        return value.trim()
                .replaceAll(
                        "[^A-Za-z0-9._-]",
                        "-"
                );
    }

    private String normalizePrefix(
            String prefix
    ) {

        if (
                prefix == null
                        ||
                        prefix.isBlank()
        ) {

            return "";
        }

        String normalized =
                prefix.trim()
                        .replaceAll(
                                "^/+",
                                ""
                        )
                        .replaceAll(
                                "/+$",
                                ""
                        );

        if (
                normalized.isBlank()
        ) {

            return "";
        }

        return normalized + "/";
    }

    private String trimTrailingSlash(
            String value
    ) {

        if (value == null) {

            return "";
        }

        return value.trim()
                .replaceAll(
                        "/+$",
                        ""
                );
    }

    private String encodePathSegment(
            String value
    ) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }

    private String encodeObjectPath(
            String objectPath
    ) {

        return Arrays.stream(
                        objectPath.split("/")
                )
                .filter(segment -> !segment.isBlank())
                .map(this::encodePathSegment)
                .reduce(
                        (left, right) -> left + "/" + right
                )
                .orElse("");
    }
}
