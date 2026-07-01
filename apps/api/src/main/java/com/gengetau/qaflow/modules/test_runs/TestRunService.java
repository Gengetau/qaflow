package com.gengetau.qaflow.modules.test_runs;

import com.gengetau.qaflow.modules.activity.ActivityLog;
import com.gengetau.qaflow.modules.activity.ActivityLogRepository;
import com.gengetau.qaflow.modules.projects.Project;
import com.gengetau.qaflow.modules.projects.ProjectService;
import com.gengetau.qaflow.modules.test_cases.TestCase;
import com.gengetau.qaflow.modules.test_cases.TestCaseRepository;
import com.gengetau.qaflow.modules.test_runs.dto.TestRunCreateRequest;
import com.gengetau.qaflow.modules.test_runs.dto.TestRunItemResponse;
import com.gengetau.qaflow.modules.test_runs.dto.TestRunItemResultRequest;
import com.gengetau.qaflow.modules.test_runs.dto.TestRunResponse;
import com.gengetau.qaflow.modules.test_runs.dto.TestRunUpdateRequest;
import com.gengetau.qaflow.modules.users.User;
import com.gengetau.qaflow.modules.users.UserRepository;
import com.gengetau.qaflow.security.CurrentUser;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TestRunService {

  private final TestRunRepository testRunRepository;
  private final TestRunItemRepository testRunItemRepository;
  private final TestCaseRepository testCaseRepository;
  private final ProjectService projectService;
  private final UserRepository userRepository;
  private final ActivityLogRepository activityLogRepository;

  public TestRunService(
      TestRunRepository testRunRepository,
      TestRunItemRepository testRunItemRepository,
      TestCaseRepository testCaseRepository,
      ProjectService projectService,
      UserRepository userRepository,
      ActivityLogRepository activityLogRepository) {
    this.testRunRepository = testRunRepository;
    this.testRunItemRepository = testRunItemRepository;
    this.testCaseRepository = testCaseRepository;
    this.projectService = projectService;
    this.userRepository = userRepository;
    this.activityLogRepository = activityLogRepository;
  }

  @Transactional(readOnly = true)
  public List<TestRunResponse> list(CurrentUser currentUser, UUID projectId) {
    projectService.requireProjectForRead(projectId, currentUser);
    return testRunRepository.findByProject_IdOrderByCreatedAtDesc(projectId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public TestRunResponse create(CurrentUser currentUser, UUID projectId, TestRunCreateRequest request) {
    Project project = projectService.requireProjectForTestArtifactWrite(projectId, currentUser);
    User actor = requireUser(currentUser.id());
    List<UUID> testCaseIds = new LinkedHashSet<>(request.testCaseIds()).stream().toList();
    List<TestCase> testCases = testCaseRepository.findAllById(testCaseIds);

    if (testCases.size() != testCaseIds.size()
        || testCases.stream().anyMatch(testCase -> !testCase.getProject().getId().equals(projectId))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All selected test cases must belong to project");
    }

    TestRun testRun = new TestRun(project, request.name().trim(), trimToNull(request.description()), actor);
    testCases.forEach(testCase -> testRun.addItem(new TestRunItem(testCase)));
    TestRun saved = testRunRepository.save(testRun);
    log(project, actor, saved.getId(), "TEST_RUN_CREATED");
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public TestRunResponse get(CurrentUser currentUser, UUID testRunId) {
    TestRun testRun = requireRun(testRunId);
    projectService.requireProjectForRead(testRun.getProject().getId(), currentUser);
    return toResponse(testRun);
  }

  @Transactional
  public TestRunResponse update(CurrentUser currentUser, UUID testRunId, TestRunUpdateRequest request) {
    TestRun testRun = requireRun(testRunId);
    Project project = projectService.requireProjectForTestArtifactWrite(testRun.getProject().getId(), currentUser);
    User actor = requireUser(currentUser.id());
    if (testRun.getStatus() != TestRunStatus.PLANNED) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test run must be PLANNED to update");
    }

    testRun.updateDetails(request.name().trim(), trimToNull(request.description()));
    log(project, actor, testRun.getId(), "TEST_RUN_UPDATED");
    return toResponse(testRun);
  }

  @Transactional
  public TestRunResponse start(CurrentUser currentUser, UUID testRunId) {
    TestRun testRun = requireRun(testRunId);
    Project project = projectService.requireProjectForTestArtifactWrite(testRun.getProject().getId(), currentUser);
    User actor = requireUser(currentUser.id());
    if (testRun.getStatus() != TestRunStatus.PLANNED) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test run must be PLANNED to start");
    }

    testRun.start();
    log(project, actor, testRun.getId(), "TEST_RUN_STARTED");
    return toResponse(testRun);
  }

  @Transactional
  public TestRunResponse complete(CurrentUser currentUser, UUID testRunId) {
    TestRun testRun = requireRun(testRunId);
    Project project = projectService.requireProjectForTestArtifactWrite(testRun.getProject().getId(), currentUser);
    User actor = requireUser(currentUser.id());
    if (testRun.getStatus() != TestRunStatus.IN_PROGRESS) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test run must be IN_PROGRESS to complete");
    }

    testRun.complete();
    log(project, actor, testRun.getId(), "TEST_RUN_COMPLETED");
    return toResponse(testRun);
  }

  @Transactional(readOnly = true)
  public List<TestRunItemResponse> listItems(CurrentUser currentUser, UUID testRunId) {
    TestRun testRun = requireRun(testRunId);
    projectService.requireProjectForRead(testRun.getProject().getId(), currentUser);
    return testRun.getItems().stream().map(this::toItemResponse).toList();
  }

  @Transactional
  public TestRunItemResponse updateResult(
      CurrentUser currentUser, UUID itemId, TestRunItemResultRequest request) {
    TestRunItem item = requireItem(itemId);
    Project project =
        projectService.requireProjectForTestArtifactWrite(
            item.getTestRun().getProject().getId(), currentUser);
    User actor = requireUser(currentUser.id());
    if (item.getTestRun().getStatus() != TestRunStatus.IN_PROGRESS) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test run must be in progress to execute results");
    }

    item.execute(request.result(), trimToNull(request.actualResult()), actor);
    log(project, actor, item.getTestRun().getId(), "TEST_RUN_ITEM_EXECUTED");
    return toItemResponse(item);
  }

  private TestRun requireRun(UUID testRunId) {
    return testRunRepository
        .findDetailedById(testRunId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test run was not found"));
  }

  private TestRunItem requireItem(UUID itemId) {
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

  private TestRunResponse toResponse(TestRun testRun) {
    return new TestRunResponse(
        testRun.getId(),
        testRun.getProject().getId(),
        testRun.getName(),
        testRun.getDescription(),
        testRun.getStatus(),
        testRun.getStartedAt(),
        testRun.getCompletedAt(),
        testRun.getCreatedBy().getId(),
        testRun.getCreatedAt(),
        testRun.getUpdatedAt(),
        testRun.getItems().stream().map(this::toItemResponse).toList());
  }

  private TestRunItemResponse toItemResponse(TestRunItem item) {
    return new TestRunItemResponse(
        item.getId(),
        item.getTestRun().getId(),
        item.getTestCase().getId(),
        item.getTestCase().getCaseKey(),
        item.getTestCase().getTitle(),
        item.getAssignee() == null ? null : item.getAssignee().getId(),
        item.getResult(),
        item.getActualResult(),
        item.getExecutedAt(),
        item.getExecutedBy() == null ? null : item.getExecutedBy().getId(),
        item.getCreatedAt(),
        item.getUpdatedAt());
  }

  private void log(Project project, User actor, UUID entityId, String action) {
    activityLogRepository.save(
        new ActivityLog(project.getWorkspace(), project, actor, "TEST_RUN", entityId, action));
  }

  private String trimToNull(String value) {
    if (value == null || value.trim().isBlank()) {
      return null;
    }
    return value.trim();
  }
}
