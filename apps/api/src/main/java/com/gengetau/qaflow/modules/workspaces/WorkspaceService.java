package com.gengetau.qaflow.modules.workspaces;

import com.gengetau.qaflow.modules.users.User;
import com.gengetau.qaflow.modules.users.UserRepository;
import com.gengetau.qaflow.modules.workspaces.dto.WorkspaceCreateRequest;
import com.gengetau.qaflow.modules.workspaces.dto.WorkspaceDetailResponse;
import com.gengetau.qaflow.modules.workspaces.dto.WorkspaceMemberCreateRequest;
import com.gengetau.qaflow.modules.workspaces.dto.WorkspaceMemberResponse;
import com.gengetau.qaflow.modules.workspaces.dto.WorkspaceMemberUpdateRequest;
import com.gengetau.qaflow.modules.workspaces.dto.WorkspaceSummaryResponse;
import com.gengetau.qaflow.modules.workspaces.dto.WorkspaceUserResponse;
import com.gengetau.qaflow.security.CurrentUser;
import com.gengetau.qaflow.security.PermissionService;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkspaceService {

  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final UserRepository userRepository;
  private final PermissionService permissionService;

  public WorkspaceService(
      WorkspaceRepository workspaceRepository,
      WorkspaceMemberRepository workspaceMemberRepository,
      UserRepository userRepository,
      PermissionService permissionService) {
    this.workspaceRepository = workspaceRepository;
    this.workspaceMemberRepository = workspaceMemberRepository;
    this.userRepository = userRepository;
    this.permissionService = permissionService;
  }

  @Transactional(readOnly = true)
  public List<WorkspaceSummaryResponse> list(CurrentUser currentUser) {
    return workspaceMemberRepository.findByUser_Id(currentUser.id()).stream()
        .map(this::toSummary)
        .toList();
  }

  @Transactional
  public WorkspaceSummaryResponse create(CurrentUser currentUser, WorkspaceCreateRequest request) {
    User user = requireUser(currentUser.id());
    Workspace workspace =
        workspaceRepository.save(new Workspace(request.name().trim(), uniqueSlug(request.name()), user));
    WorkspaceMember member = workspaceMemberRepository.save(new WorkspaceMember(workspace, user, WorkspaceRole.OWNER));
    return toSummary(member);
  }

  @Transactional(readOnly = true)
  public WorkspaceDetailResponse get(CurrentUser currentUser, UUID workspaceId) {
    WorkspaceMember currentMember = permissionService.requireWorkspaceMember(workspaceId, currentUser);
    return toDetail(currentMember);
  }

  @Transactional
  public WorkspaceMemberResponse addMember(
      CurrentUser currentUser, UUID workspaceId, WorkspaceMemberCreateRequest request) {
    Workspace workspace = permissionService.requireWorkspaceManagement(workspaceId, currentUser).getWorkspace();
    User user =
        userRepository
            .findByEmail(normalizeEmail(request.email()))
            .filter(User::isActive)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User was not found"));

    if (workspaceMemberRepository.existsByWorkspace_IdAndUser_Id(workspaceId, user.getId())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a workspace member");
    }

    WorkspaceMember member = workspaceMemberRepository.save(new WorkspaceMember(workspace, user, request.role()));
    return toMember(member);
  }

  @Transactional
  public WorkspaceMemberResponse updateMember(
      CurrentUser currentUser, UUID workspaceId, UUID memberId, WorkspaceMemberUpdateRequest request) {
    permissionService.requireWorkspaceManagement(workspaceId, currentUser);
    WorkspaceMember member = requireWorkspaceMemberRecord(workspaceId, memberId);

    if (member.getRole() == WorkspaceRole.OWNER
        && request.role() != WorkspaceRole.OWNER
        && workspaceMemberRepository.countByWorkspace_IdAndRole(workspaceId, WorkspaceRole.OWNER) <= 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace must keep at least one owner");
    }

    member.setRole(request.role());
    return toMember(member);
  }

  @Transactional
  public void deleteMember(CurrentUser currentUser, UUID workspaceId, UUID memberId) {
    permissionService.requireWorkspaceManagement(workspaceId, currentUser);
    WorkspaceMember member = requireWorkspaceMemberRecord(workspaceId, memberId);

    if (member.getRole() == WorkspaceRole.OWNER
        && workspaceMemberRepository.countByWorkspace_IdAndRole(workspaceId, WorkspaceRole.OWNER) <= 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace must keep at least one owner");
    }

    workspaceMemberRepository.delete(member);
  }

  private WorkspaceDetailResponse toDetail(WorkspaceMember currentMember) {
    Workspace workspace = currentMember.getWorkspace();
    List<WorkspaceMemberResponse> members =
        workspaceMemberRepository.findByWorkspace_IdOrderByJoinedAtAsc(workspace.getId()).stream()
            .map(this::toMember)
            .toList();
    return new WorkspaceDetailResponse(
        workspace.getId(), workspace.getName(), workspace.getSlug(), currentMember.getRole().name(), members);
  }

  private WorkspaceSummaryResponse toSummary(WorkspaceMember member) {
    Workspace workspace = member.getWorkspace();
    return new WorkspaceSummaryResponse(
        workspace.getId(), workspace.getName(), workspace.getSlug(), member.getRole().name());
  }

  private WorkspaceMemberResponse toMember(WorkspaceMember member) {
    User user = member.getUser();
    return new WorkspaceMemberResponse(
        member.getId(),
        new WorkspaceUserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getAvatarUrl()),
        member.getRole(),
        member.getJoinedAt());
  }

  private WorkspaceMember requireWorkspaceMemberRecord(UUID workspaceId, UUID memberId) {
    return workspaceMemberRepository
        .findByIdAndWorkspace_Id(memberId, workspaceId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace member was not found"));
  }

  private User requireUser(UUID userId) {
    return userRepository
        .findById(userId)
        .filter(User::isActive)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not active"));
  }

  private String uniqueSlug(String workspaceName) {
    String base = slugify(workspaceName);
    String candidate = base;
    int suffix = 2;
    while (workspaceRepository.existsBySlug(candidate)) {
      candidate = base + "-" + suffix;
      suffix++;
    }
    return candidate;
  }

  private String slugify(String input) {
    String slug = input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    return slug.isBlank() ? "workspace" : slug;
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
