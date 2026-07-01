package com.gengetau.qaflow.security;

import com.gengetau.qaflow.modules.workspaces.WorkspaceMember;
import com.gengetau.qaflow.modules.workspaces.WorkspaceMemberRepository;
import com.gengetau.qaflow.modules.workspaces.WorkspaceRole;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PermissionService {

  private final WorkspaceMemberRepository workspaceMemberRepository;

  public PermissionService(WorkspaceMemberRepository workspaceMemberRepository) {
    this.workspaceMemberRepository = workspaceMemberRepository;
  }

  @Transactional(readOnly = true)
  public WorkspaceMember requireWorkspaceMember(UUID workspaceId, CurrentUser currentUser) {
    return workspaceMemberRepository
        .findByWorkspace_IdAndUser_Id(workspaceId, currentUser.id())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Workspace access denied"));
  }

  @Transactional(readOnly = true)
  public WorkspaceMember requireWorkspaceRole(
      UUID workspaceId, CurrentUser currentUser, WorkspaceRole... allowedRoles) {
    WorkspaceMember member = requireWorkspaceMember(workspaceId, currentUser);
    boolean allowed = Arrays.asList(allowedRoles).contains(member.getRole());
    if (!allowed) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient workspace role");
    }
    return member;
  }

  @Transactional(readOnly = true)
  public WorkspaceMember requireWorkspaceManagement(UUID workspaceId, CurrentUser currentUser) {
    return requireWorkspaceRole(workspaceId, currentUser, WorkspaceRole.OWNER);
  }

  @Transactional(readOnly = true)
  public WorkspaceMember requireTestArtifactWrite(UUID workspaceId, CurrentUser currentUser) {
    return requireWorkspaceRole(workspaceId, currentUser, WorkspaceRole.OWNER, WorkspaceRole.TESTER);
  }
}
