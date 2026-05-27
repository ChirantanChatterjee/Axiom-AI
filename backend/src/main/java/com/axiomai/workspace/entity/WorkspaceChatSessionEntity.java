package com.axiomai.workspace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "workspace_chat_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceChatSessionEntity {

    @Id
    @Column(nullable = false, length = 128)
    private String sessionId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 320)
    private String userEmail;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 2048)
    private String websiteUrl;

    @Column(length = 255)
    private String domainName;

    @Column(nullable = false)
    private boolean frameworkLocked;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Lob
    @Column(nullable = false, columnDefinition = "text")
    private String messagesJson;
}
