package com.gengetau.qaflow.modules.workspaces;

import com.gengetau.qaflow.modules.workspaces.dto.WorkspaceCreateRequest;
import com.gengetau.qaflow.modules.workspaces.dto.WorkspaceDetailResponse;
import com.gengetau.qaflow.modules.workspaces.dto.WorkspaceMemberCreateRequest;
import com.gengetau.qaflow.modules.workspaces.dto.WorkspaceMemberResponse;
import com.gengetau.qaflow.modules.workspaces.dto.WorkspaceMemberUpdateRequest;
import com.gengetau.qaflow.modules.workspaces.dto.WorkspaceSummaryResponse;
import com.gengetau.qaflow.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

  private final WorkspaceService workspaceService;

  public WorkspaceController(WorkspaceService workspaceService) {
    this.workspaceService = workspaceService;
  }

  @GetMapping
  List<WorkspaceSummaryResponse> list(@AuthenticationPrincipal CurrentUser currentUser) {
    return workspaceService.list(currentUser);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  WorkspaceSummaryResponse create(
      @AuthenticationPrincipal CurrentUser currentUser, @Valid @RequestBody WorkspaceCreateRequest request) {
    return workspaceService.create(currentUser, request);
  }

  @GetMapping("/{workspaceId}")
  WorkspaceDetailResponse get(
      @AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID workspaceId) {
    return workspaceService.get(currentUser, workspaceId);
  }

  @PostMapping("/{workspaceId}/members")
  @ResponseStatus(HttpStatus.CREATED)
  WorkspaceMemberResponse addMember(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID workspaceId,
      @Valid @RequestBody WorkspaceMemberCreateRequest request) {
    return workspaceService.addMember(currentUser, workspaceId, request);
  }

  @PatchMapping("/{workspaceId}/members/{memberId}")
  WorkspaceMemberResponse updateMember(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID workspaceId,
      @PathVariable UUID memberId,
      @Valid @RequestBody WorkspaceMemberUpdateRequest request) {
    return workspaceService.updateMember(currentUser, workspaceId, memberId, request);
  }

  @DeleteMapping("/{workspaceId}/members/{memberId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteMember(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID workspaceId,
      @PathVariable UUID memberId) {
    workspaceService.deleteMember(currentUser, workspaceId, memberId);
  }
}
