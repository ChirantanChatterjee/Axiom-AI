package com.axiomai.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "aif_users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AifUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false, length = 1024)
    private String passwordHash;

    @Column(nullable = false, length = 64)
    private String provider;

    private String providerUserId;

    private String avatarUrl;

    private String role;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant lastLoginAt;
}
