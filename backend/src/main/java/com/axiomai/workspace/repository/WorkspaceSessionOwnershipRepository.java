package com.axiomai.workspace.repository;

import com.axiomai.workspace.entity.WorkspaceSessionOwnershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceSessionOwnershipRepository
        extends JpaRepository<WorkspaceSessionOwnershipEntity, String> {
}
