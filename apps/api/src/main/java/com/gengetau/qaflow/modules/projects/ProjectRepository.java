package com.gengetau.qaflow.modules.projects;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
  Page<Project> findByWorkspace_Id(UUID workspaceId, Pageable pageable);

  boolean existsByWorkspace_IdAndKey(UUID workspaceId, String key);
}
