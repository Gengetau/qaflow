package com.gengetau.qaflow.modules.defects;

import com.gengetau.qaflow.modules.activity.ActivityLog;
import com.gengetau.qaflow.modules.activity.ActivityLogRepository;
import com.gengetau.qaflow.modules.defects.dto.DefectCommentRequest;
import com.gengetau.qaflow.modules.defects.dto.DefectCommentResponse;
import com.gengetau.qaflow.modules.defects.dto.DefectRequest;
import com.gengetau.qaflow.modules.defects.dto.DefectResponse;
import com.gengetau.qaflow.modules.defects.dto.DefectTransitionRequest;
import com.gengetau.qaflow.modules.projects.Project;
import com.gengetau.qaflow.modules.projects.ProjectService;
import com.gengetau.qaflow.modules.test_runs.TestRunItem;
import com.gengetau.qaflow.modules.test_runs.TestRunItemRepository;
import com.gengetau.qaflow.modules.test_runs.TestRunItemResult;
import com.gengetau.qaflow.modules.users.User;
import com.gengetau.qaflow.modules.users.UserRepository;
import com.gengetau.qaflow.security.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DefectService {

  private final DefectRepository defectRepository;
  private final TestRunItemRepository testRunItemRepository;
  private final ProjectService projectService;
  private final UserRepository userRepository;
  private final ActivityLogRepository activityLogRepository;

  public DefectService(
      DefectRepository defectRepository,
      TestRunItemRepository testRunItemRepository,
      ProjectService projectService,
      UserRepository userRepository,
      ActivityLogRepository activityLogRepository) {
    this.defectRepository = defectRepository;
    this.testRunItemRepository = testRunItemRepository;
    this.projectService = projectService;
    this.userRepository = userRepository;
    this.activityLogRepository = activityLogRepository;
  }

  @Transactional(readOnly = true)
  public List<DefectResponse> list(CurrentUser currentUser, UUID projectId) {
    projectService.requireProjectForRead(projectId, currentUser);
    return defectRepository.findDetailedByProjectIdOrderByCreatedAtDesc(projectId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public DefectResponse create(CurrentUser currentUser, UUID projectId, DefectRequest request) {
    Project project = projectService.requireProjectForTestArtifactWrite(projectId, currentUser);
    User actor = requireUser(currentUser.id());
    Defect defect =
        defectRepository.save(
            new Defect(
                project,
                null,
                request.title().trim(),
                trimToNull(request.description()),
                request.severity(),
                request.priority(),
                requireOptionalUser(request.assigneeId()),
                actor));
    log(project, actor, defect.getId(), "DEFECT_CREATED");
    return toResponse(defect);
  }

  @Transactional
  public DefectResponse createFromRunItem(CurrentUser currentUser, UUID itemId, DefectRequest request) {
    TestRunItem item = requireRunItem(itemId);
    Project project = item.getTestRun().getProject();
    projectService.requireProjectForTestArtifactWrite(project.getId(), currentUser);
    User actor = requireUser(currentUser.id());
    if (item.getResult() != TestRunItemResult.FAILED) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only failed run items can create defects");
    }

    Defect defect =
        defectRepository.save(
            new Defect(
                project,
                item,
                request.title().trim(),
                trimToNull(request.description()),
                request.severity(),
                request.priority(),
                requireOptionalUser(request.assigneeId()),
                actor));
    log(project, actor, defect.getId(), "DEFECT_CREATED");
    return toResponse(defect);
  }

  @Transactional(readOnly = true)
  public DefectResponse get(CurrentUser currentUser, UUID defectId) {
    Defect defect = requireDefect(defectId);
    projectService.requireProjectForRead(defect.getProject().getId(), currentUser);
    return toResponse(defect);
  }

  @Transactional
  public DefectResponse update(CurrentUser currentUser, UUID defectId, DefectRequest request) {
    Defect defect = requireDefect(defectId);
    Project project = projectService.requireProjectForTestArtifactWrite(defect.getProject().getId(), currentUser);
    User actor = requireUser(currentUser.id());
    defect.update(
        request.title().trim(),
        trimToNull(request.description()),
        request.severity(),
        request.priority(),
        requireOptionalUser(request.assigneeId()));
    log(project, actor, defect.getId(), "DEFECT_UPDATED");
    return toResponse(defect);
  }

  @Transactional
  public DefectResponse transition(CurrentUser currentUser, UUID defectId, DefectTransitionRequest request) {
    Defect defect = requireDefect(defectId);
    Project project = projectService.requireProjectForTestArtifactWrite(defect.getProject().getId(), currentUser);
    User actor = requireUser(currentUser.id());
    if (!canTransition(defect.getStatus(), request.status())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Invalid defect transition from " + defect.getStatus() + " to " + request.status());
    }

    defect.transitionTo(request.status());
    log(project, actor, defect.getId(), "DEFECT_TRANSITIONED");
    return toResponse(defect);
  }

  @Transactional
  public DefectResponse addComment(CurrentUser currentUser, UUID defectId, DefectCommentRequest request) {
    Defect defect = requireDefect(defectId);
    Project project = projectService.requireProjectForTestArtifactWrite(defect.getProject().getId(), currentUser);
    User actor = requireUser(currentUser.id());
    defect.addComment(new DefectComment(actor, request.body().trim()));
    Defect saved = defectRepository.saveAndFlush(defect);
    log(project, actor, defect.getId(), "DEFECT_COMMENTED");
    return toResponse(saved);
  }

  private Defect requireDefect(UUID defectId) {
    return defectRepository
        .findDetailedById(defectId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Defect was not found"));
  }

  private TestRunItem requireRunItem(UUID itemId) {
    return testRunItemRepository
        .findDetailedById(itemId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test run item was not found"));
  }

  private User requireUser(UUID userId) {
    return userRepository
        .findById(userId)
        .filter(User::isActive)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not active"));
  }

  private User requireOptionalUser(UUID userId) {
    if (userId == null) {
      return null;
    }
    return requireUser(userId);
  }

  private boolean canTransition(DefectStatus current, DefectStatus next) {
    return switch (current) {
      case OPEN -> next == DefectStatus.IN_PROGRESS;
      case IN_PROGRESS -> next == DefectStatus.RESOLVED;
      case RESOLVED -> next == DefectStatus.CLOSED || next == DefectStatus.REOPENED;
      case CLOSED -> next == DefectStatus.REOPENED;
      case REOPENED -> next == DefectStatus.IN_PROGRESS;
    };
  }

  private DefectResponse toResponse(Defect defect) {
    TestRunItem item = defect.getTestRunItem();
    return new DefectResponse(
        defect.getId(),
        defect.getProject().getId(),
        item == null ? null : item.getId(),
        item == null ? null : item.getTestCase().getId(),
        item == null ? null : item.getTestCase().getCaseKey(),
        defect.getTitle(),
        defect.getDescription(),
        defect.getSeverity(),
        defect.getPriority(),
        defect.getStatus(),
        defect.getAssignee() == null ? null : defect.getAssignee().getId(),
        defect.getReportedBy().getId(),
        defect.getCreatedAt(),
        defect.getUpdatedAt(),
        defect.getComments().stream().map(this::toCommentResponse).toList());
  }

  private DefectCommentResponse toCommentResponse(DefectComment comment) {
    return new DefectCommentResponse(
        comment.getId(),
        comment.getDefect().getId(),
        comment.getAuthor().getId(),
        comment.getBody(),
        comment.getCreatedAt(),
        comment.getUpdatedAt());
  }

  private void log(Project project, User actor, UUID entityId, String action) {
    activityLogRepository.save(
        new ActivityLog(project.getWorkspace(), project, actor, "DEFECT", entityId, action));
  }

  private String trimToNull(String value) {
    if (value == null || value.trim().isBlank()) {
      return null;
    }
    return value.trim();
  }
}
