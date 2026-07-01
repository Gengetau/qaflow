package com.gengetau.qaflow.modules.workspaces;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {
  List<WorkspaceMember> findByUser_Id(UUID userId);

  Optional<WorkspaceMember> findByWorkspace_IdAndUser_Id(UUID workspaceId, UUID userId);

  boolean existsByWorkspace_IdAndUser_Id(UUID workspaceId, UUID userId);

  Optional<WorkspaceMember> findByIdAndWorkspace_Id(UUID memberId, UUID workspaceId);

  List<WorkspaceMember> findByWorkspace_IdOrderByJoinedAtAsc(UUID workspaceId);

  long countByWorkspace_IdAndRole(UUID workspaceId, WorkspaceRole role);
}