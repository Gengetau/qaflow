package com.gengetau.qaflow.modules.projects;

import com.gengetau.qaflow.common.pagination.PageResponse;
import com.gengetau.qaflow.modules.projects.dto.ProjectCreateRequest;
import com.gengetau.qaflow.modules.projects.dto.ProjectResponse;
import com.gengetau.qaflow.modules.projects.dto.ProjectUpdateRequest;
import com.gengetau.qaflow.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

  private final ProjectService projectService;

  public ProjectController(ProjectService projectService) {
    this.projectService = projectService;
  }

  @GetMapping
  PageResponse<ProjectResponse> list(
      @AuthenticationPrincipal CurrentUser currentUser,
      @RequestParam UUID workspaceId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return projectService.list(currentUser, workspaceId, page, size);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  ProjectResponse create(
      @AuthenticationPrincipal CurrentUser currentUser, @Valid @RequestBody ProjectCreateRequest request) {
    return projectService.create(currentUser, request);
  }

  @GetMapping("/{projectId}")
  ProjectResponse get(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID projectId) {
    return projectService.get(currentUser, projectId);
  }

  @PatchMapping("/{projectId}")
  ProjectResponse update(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID projectId,
      @Valid @RequestBody ProjectUpdateRequest request) {
    return projectService.update(currentUser, projectId, request);
  }

  @DeleteMapping("/{projectId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID projectId) {
    projectService.delete(currentUser, projectId);
  }
}
