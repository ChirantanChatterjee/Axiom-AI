package com.axiomai.auth.repository;

import com.axiomai.auth.entity.AifAuthSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AifAuthSessionRepository
        extends JpaRepository<AifAuthSessionEntity, Long> {

    Optional<AifAuthSessionEntity> findByToken(
            String token
    );
}
