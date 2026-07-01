package com.gengetau.qaflow.modules.defects;

import com.gengetau.qaflow.modules.defects.dto.DefectCommentRequest;
import com.gengetau.qaflow.modules.defects.dto.DefectRequest;
import com.gengetau.qaflow.modules.defects.dto.DefectResponse;
import com.gengetau.qaflow.modules.defects.dto.DefectTransitionRequest;
import com.gengetau.qaflow.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DefectController {

  private final DefectService defectService;

  public DefectController(DefectService defectService) {
    this.defectService = defectService;
  }

  @GetMapping("/projects/{projectId}/defects")
  List<DefectResponse> list(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID projectId) {
    return defectService.list(currentUser, projectId);
  }

  @PostMapping("/projects/{projectId}/defects")
  @ResponseStatus(HttpStatus.CREATED)
  DefectResponse create(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID projectId,
      @Valid @RequestBody DefectRequest request) {
    return defectService.create(currentUser, projectId, request);
  }

  @PostMapping("/test-run-items/{itemId}/defects")
  @ResponseStatus(HttpStatus.CREATED)
  DefectResponse createFromRunItem(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID itemId,
      @Valid @RequestBody DefectRequest request) {
    return defectService.createFromRunItem(currentUser, itemId, request);
  }

  @GetMapping("/defects/{defectId}")
  DefectResponse get(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID defectId) {
    return defectService.get(currentUser, defectId);
  }

  @PatchMapping("/defects/{defectId}")
  DefectResponse update(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID defectId,
      @Valid @RequestBody DefectRequest request) {
    return defectService.update(currentUser, defectId, request);
  }

  @PostMapping("/defects/{defectId}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  DefectResponse addComment(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID defectId,
      @Valid @RequestBody DefectCommentRequest request) {
    return defectService.addComment(currentUser, defectId, request);
  }

  @PostMapping("/defects/{defectId}/transition")
  DefectResponse transition(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID defectId,
      @Valid @RequestBody DefectTransitionRequest request) {
    return defectService.transition(currentUser, defectId, request);
  }
}
