package com.axiomai.workspace;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceChatSessionDto {

    private String id;

    private String title;

    private String websiteUrl;

    private String domainName;

    private boolean frameworkLocked;

    private Instant createdAt;

    private Instant updatedAt;

    private List<Map<String, Object>> messages;
}
