package com.gengetau.qaflow.modules.projects;

import com.gengetau.qaflow.common.pagination.PageResponse;
import com.gengetau.qaflow.modules.activity.ActivityLog;
import com.gengetau.qaflow.modules.activity.ActivityLogRepository;
import com.gengetau.qaflow.modules.projects.dto.ProjectCreateRequest;
import com.gengetau.qaflow.modules.projects.dto.ProjectResponse;
import com.gengetau.qaflow.modules.projects.dto.ProjectUpdateRequest;
import com.gengetau.qaflow.modules.users.User;
import com.gengetau.qaflow.modules.users.UserRepository;
import com.gengetau.qaflow.modules.workspaces.Workspace;
import com.gengetau.qaflow.modules.workspaces.WorkspaceMember;
import com.gengetau.qaflow.security.CurrentUser;
import com.gengetau.qaflow.security.PermissionService;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final PermissionService permissionService;
  private final ActivityLogRepository activityLogRepository;

  public ProjectService(
      ProjectRepository projectRepository,
      UserRepository userRepository,
      PermissionService permissionService,
      ActivityLogRepository activityLogRepository) {
    this.projectRepository = projectRepository;
    this.userRepository = userRepository;
    this.permissionService = permissionService;
    this.activityLogRepository = activityLogRepository;
  }

  @Transactional(readOnly = true)
  public PageResponse<ProjectResponse> list(CurrentUser currentUser, UUID workspaceId, int page, int size) {
    permissionService.requireWorkspaceMember(workspaceId, currentUser);
    PageRequest pageRequest =
        PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), 100),
            Sort.by(Sort.Direction.DESC, "createdAt"));
    return PageResponse.from(projectRepository.findByWorkspace_Id(workspaceId, pageRequest).map(this::toResponse));
  }

  @Transactional
  public ProjectResponse create(CurrentUser currentUser, ProjectCreateRequest request) {
    WorkspaceMember member = permissionService.requireWorkspaceManagement(request.workspaceId(), currentUser);
    User actor = requireUser(currentUser.id());
    String key = normalizeKey(request.key());
    if (projectRepository.existsByWorkspace_IdAndKey(request.workspaceId(), key)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Project key already exists in this workspace");
    }

    Project project =
        projectRepository.save(
            new Project(member.getWorkspace(), request.name().trim(), key, trimToNull(request.description()), actor));
    log(member.getWorkspace(), project, actor, "PROJECT", project.getId(), "PROJECT_CREATED");
    return toResponse(project);
  }

  @Transactional(readOnly = true)
  public ProjectResponse get(CurrentUser currentUser, UUID projectId) {
    return toResponse(requireProjectForRead(projectId, currentUser));
  }

  @Transactional
  public ProjectResponse update(CurrentUser currentUser, UUID projectId, ProjectUpdateRequest request) {
    Project project = requireProjectForWorkspaceManagement(projectId, currentUser);
    project.update(request.name().trim(), trimToNull(request.description()), request.status());
    log(project.getWorkspace(), project, requireUser(currentUser.id()), "PROJECT", project.getId(), "PROJECT_UPDATED");
    return toResponse(project);
  }

  @Transactional
  public void delete(CurrentUser currentUser, UUID projectId) {
    Project project = requireProjectForWorkspaceManagement(projectId, currentUser);
    Workspace workspace = project.getWorkspace();
    User actor = requireUser(currentUser.id());
    projectRepository.delete(project);
    log(workspace, null, actor, "PROJECT", projectId, "PROJECT_DELETED");
  }

  @Transactional(readOnly = true)
  public Project requireProjectForRead(UUID projectId, CurrentUser currentUser) {
    Project project = requireProject(projectId);
    permissionService.requireWorkspaceMember(project.getWorkspace().getId(), currentUser);
    return project;
  }

  @Transactional(readOnly = true)
  public Project requireProjectForTestArtifactWrite(UUID projectId, CurrentUser currentUser) {
    Project project = requireProject(projectId);
    permissionService.requireTestArtifactWrite(project.getWorkspace().getId(), currentUser);
    return project;
  }

  private Project requireProjectForWorkspaceManagement(UUID projectId, CurrentUser currentUser) {
    Project project = requireProject(projectId);
    permissionService.requireWorkspaceManagement(project.getWorkspace().getId(), currentUser);
    return project;
  }

  private Project requireProject(UUID projectId) {
    return projectRepository
        .findById(projectId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project was not found"));
  }

  private User requireUser(UUID userId) {
    return userRepository
        .findById(userId)
        .filter(User::isActive)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not active"));
  }

  public ProjectResponse toResponse(Project project) {
    return new ProjectResponse(
        project.getId(),
        project.getWorkspace().getId(),
        project.getName(),
        project.getKey(),
        project.getDescription(),
        project.getStatus(),
        project.getCreatedAt(),
        project.getUpdatedAt());
  }

  private void log(Workspace workspace, Project project, User actor, String entityType, UUID entityId, String action) {
    activityLogRepository.save(new ActivityLog(workspace, project, actor, entityType, entityId, action));
  }

  private String normalizeKey(String key) {
    return key.trim().toUpperCase(Locale.ROOT);
  }

  private String trimToNull(String value) {
    if (value == null || value.trim().isBlank()) {
      return null;
    }
    return value.trim();
  }
}
