package com.axiomai.auth.repository;

import com.axiomai.auth.entity.AifUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AifUserRepository
        extends JpaRepository<AifUserEntity, Long> {

    Optional<AifUserEntity> findByEmailIgnoreCase(
            String email
    );

    boolean existsByEmailIgnoreCase(
            String email
    );
}
