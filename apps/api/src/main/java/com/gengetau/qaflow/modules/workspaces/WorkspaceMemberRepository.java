package com.gengetau.qaflow.modules.workspaces;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {
  List<WorkspaceMember> findByUser_Id(UUID userId);
}